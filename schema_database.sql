DROP DATABASE IF EXISTS rubrica;
CREATE DATABASE rubrica CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE rubrica;

-- Utenti DB applicativi
DROP USER IF EXISTS 'LOGIN'@'%';
DROP USER IF EXISTS 'LOGGED'@'%';
CREATE USER 'LOGIN'@'%'  IDENTIFIED BY 'login_pwd';
CREATE USER 'LOGGED'@'%' IDENTIFIED BY 'logged_pwd';

-- Tabelle
CREATE TABLE LoginUser (
    username       VARCHAR(64) PRIMARY KEY,
    password_hash  CHAR(64)     NOT NULL,      -- SHA2-256 hex
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Persona per-utente: stessa persona (telefono) può esistere per utenti diversi
CREATE TABLE Persona (
    username  VARCHAR(64)  NOT NULL,
    telefono  VARCHAR(20)  NOT NULL,
    nome      VARCHAR(64)  NOT NULL,
    cognome   VARCHAR(64)  NOT NULL,
    indirizzo VARCHAR(128) NOT NULL,
    eta       INT          NOT NULL CHECK (eta >= 0),
    PRIMARY KEY (username, telefono),
    CONSTRAINT fk_persona_user
        FOREIGN KEY (username) REFERENCES LoginUser(username)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE INDEX ix_persona_cognome_nome ON Persona(username, cognome, nome);
CREATE INDEX ix_persona_telefono ON Persona(telefono);

-- Sessioni (token)
CREATE TABLE LoginSession (
    token       CHAR(64)     PRIMARY KEY,                 -- HEX(RANDOM_BYTES(32))
    username    VARCHAR(64)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at  TIMESTAMP    NOT NULL,
    CONSTRAINT fk_session_user
        FOREIGN KEY (username) REFERENCES LoginUser(username)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE INDEX ix_session_user ON LoginSession(username);
CREATE INDEX ix_session_exp  ON LoginSession(expires_at);

-- ==========================================================
-- ROUTINES (Function + Stored Procedures)
-- Tutte SQL SECURITY DEFINER
-- ==========================================================
DELIMITER $$

-- username dal token (accetta >= NOW())
DROP FUNCTION IF EXISTS fn_username_from_token $$
CREATE FUNCTION fn_username_from_token(p_token CHAR(64))
RETURNS VARCHAR(64)
SQL SECURITY DEFINER
READS SQL DATA
BEGIN
    DECLARE v_user VARCHAR(64);
    SELECT username INTO v_user
    FROM LoginSession
    WHERE token = p_token
      AND expires_at >= NOW();
    RETURN v_user;
END $$

-- LOGIN: TTL fisso 12 ore
DROP PROCEDURE IF EXISTS sp_login $$
CREATE PROCEDURE sp_login(IN p_username VARCHAR(64), IN p_plainpassword VARCHAR(255), OUT p_token CHAR(64))
SQL SECURITY DEFINER
BEGIN
    DECLARE v_hash CHAR(64);
    DECLARE v_now  TIMESTAMP;
    DECLARE v_exp  TIMESTAMP;

    SELECT password_hash INTO v_hash
    FROM LoginUser
    WHERE username = p_username;

    IF v_hash IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Utente non trovato';
    END IF;

    IF v_hash <> SHA2(p_plainpassword, 256) THEN
        SIGNAL SQLSTATE '45001' SET MESSAGE_TEXT = 'Password errata';
    END IF;

    SET v_now = NOW();
    SET v_exp = v_now + INTERVAL 12 HOUR;

    DELETE FROM LoginSession WHERE username = p_username;

    SET p_token = UPPER(HEX(RANDOM_BYTES(32)));

    INSERT INTO LoginSession(token, username, expires_at)
    VALUES (p_token, p_username, v_exp);
END $$

-- REFRESH SESSION: TTL fisso 12 ore
DROP PROCEDURE IF EXISTS sp_refresh_session $$
CREATE PROCEDURE sp_refresh_session(IN p_token CHAR(64))
SQL SECURITY DEFINER
BEGIN
    UPDATE LoginSession
    SET expires_at = NOW() + INTERVAL 12 HOUR
    WHERE token = p_token
      AND expires_at >= NOW();

    IF ROW_COUNT() = 0 THEN
        SIGNAL SQLSTATE '45010' SET MESSAGE_TEXT = 'Sessione non valida o scaduta';
    END IF;
END $$

-- REGISTER
DROP PROCEDURE IF EXISTS sp_register $$
CREATE PROCEDURE sp_register(IN p_username VARCHAR(64), IN p_plainpassword VARCHAR(255))
SQL SECURITY DEFINER
BEGIN
    DECLARE v_count INT;

    SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
    START TRANSACTION;

    SELECT COUNT(*) INTO v_count
    FROM LoginUser
    WHERE username = p_username
    FOR UPDATE;

    IF v_count > 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45002' SET MESSAGE_TEXT = 'Utente già registrato';
    END IF;

    INSERT INTO LoginUser(username, password_hash)
    VALUES (p_username, SHA2(p_plainpassword, 256));

    COMMIT;
END $$

-- LOGOUT
DROP PROCEDURE IF EXISTS sp_logout $$
CREATE PROCEDURE sp_logout(IN p_token CHAR(64))
SQL SECURITY DEFINER
BEGIN
    DELETE FROM LoginSession WHERE token = p_token;
END $$

-- INSERISCI PERSONA
DROP PROCEDURE IF EXISTS sp_inserisci_persona $$
CREATE PROCEDURE sp_inserisci_persona(
    IN p_token CHAR(64),
    IN p_nome VARCHAR(64),
    IN p_cognome VARCHAR(64),
    IN p_indirizzo VARCHAR(128),
    IN p_telefono VARCHAR(20),
    IN p_eta INT
)
SQL SECURITY DEFINER
BEGIN
    DECLARE v_user VARCHAR(64);

    SET v_user = fn_username_from_token(p_token);
    IF v_user IS NULL THEN
        SIGNAL SQLSTATE '45010' SET MESSAGE_TEXT = 'Sessione non valida o scaduta';
    END IF;

    SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
    START TRANSACTION;

    INSERT INTO Persona(username, telefono, nome, cognome, indirizzo, eta)
    VALUES(v_user, p_telefono, p_nome, p_cognome, p_indirizzo, p_eta)
    ON DUPLICATE KEY UPDATE
        nome = VALUES(nome),
        cognome = VALUES(cognome),
        indirizzo = VALUES(indirizzo),
        eta = VALUES(eta);

    COMMIT;
END $$

DROP PROCEDURE IF EXISTS sp_modifica_persona $$
CREATE PROCEDURE sp_modifica_persona(
    IN p_token        CHAR(64),
    IN p_old_telefono VARCHAR(20),
    IN p_new_telefono VARCHAR(20),
    IN p_nome         VARCHAR(64),
    IN p_cognome      VARCHAR(64),
    IN p_indirizzo    VARCHAR(128),
    IN p_eta          INT
)
SQL SECURITY DEFINER
proc_end: BEGIN
    DECLARE v_user VARCHAR(64);
    DECLARE v_exists INT;

    SET v_user = fn_username_from_token(p_token);
    IF v_user IS NULL THEN
        SIGNAL SQLSTATE '45010' SET MESSAGE_TEXT = 'Sessione non valida o scaduta';
    END IF;

    IF p_old_telefono = p_new_telefono THEN
        SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
        START TRANSACTION;

        UPDATE Persona
        SET nome = p_nome,
            cognome = p_cognome,
            indirizzo = p_indirizzo,
            eta = p_eta
        WHERE username = v_user
          AND telefono = p_old_telefono;

        IF ROW_COUNT() = 0 THEN
            ROLLBACK;
            SIGNAL SQLSTATE '45011' SET MESSAGE_TEXT = 'Voce non trovata nella tua rubrica';
        END IF;

        COMMIT;
        LEAVE proc_end;
    END IF;

    SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;
    START TRANSACTION;

    -- Blocca la riga vecchia
    SELECT COUNT(*) INTO v_exists
    FROM Persona
    WHERE username = v_user AND telefono = p_old_telefono
    FOR UPDATE;

    IF v_exists = 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45011' SET MESSAGE_TEXT = 'Voce non trovata nella tua rubrica';
    END IF;

    -- Verifica unicità nuovo numero per lo stesso utente
    SELECT COUNT(*) INTO v_exists
    FROM Persona
    WHERE username = v_user AND telefono = p_new_telefono
    FOR UPDATE;

    IF v_exists > 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45012' SET MESSAGE_TEXT = 'Telefono già presente nella tua rubrica';
    END IF;

    -- Aggiornamento PK + dati
    UPDATE Persona
    SET telefono  = p_new_telefono,
        nome      = p_nome,
        cognome   = p_cognome,
        indirizzo = p_indirizzo,
        eta       = p_eta
    WHERE username = v_user
      AND telefono = p_old_telefono;

    COMMIT;
END proc_end $$

-- ELIMINA PERSONA (firma invariata): elimina solo la voce dell'utente del token
DROP PROCEDURE IF EXISTS sp_elimina_persona $$
CREATE PROCEDURE sp_elimina_persona(IN p_token CHAR(64), IN p_telefono VARCHAR(20))
SQL SECURITY DEFINER
BEGIN
    DECLARE v_user VARCHAR(64);

    SET v_user = fn_username_from_token(p_token);
    IF v_user IS NULL THEN
        SIGNAL SQLSTATE '45010' SET MESSAGE_TEXT = 'Sessione non valida o scaduta';
    END IF;

    DELETE FROM Persona
    WHERE username = v_user
      AND telefono = p_telefono;
END $$

-- SCARICA RUBRICA (firma invariata): tutte le persone dell'utente del token
DROP PROCEDURE IF EXISTS sp_get_rubrica $$
CREATE PROCEDURE sp_get_rubrica(IN p_token CHAR(64))
SQL SECURITY DEFINER
BEGIN
    DECLARE v_user VARCHAR(64);

    SET v_user = fn_username_from_token(p_token);
    IF v_user IS NULL THEN
        SIGNAL SQLSTATE '45010' SET MESSAGE_TEXT = 'Sessione non valida o scaduta';
    END IF;

    SET TRANSACTION ISOLATION LEVEL READ COMMITTED;

    SELECT p.nome, p.cognome, p.indirizzo, p.telefono, p.eta
    FROM Persona p
    WHERE p.username = v_user
    ORDER BY p.cognome, p.nome;
END $$

DELIMITER ;

-- ==========================================================
-- PERMESSI: solo EXECUTE sulle routine
-- ==========================================================
REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'LOGIN'@'%';
REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'LOGGED'@'%';

GRANT EXECUTE ON PROCEDURE rubrica.sp_register         TO 'LOGIN'@'%';
GRANT EXECUTE ON PROCEDURE rubrica.sp_login            TO 'LOGIN'@'%';
GRANT EXECUTE ON PROCEDURE rubrica.sp_logout           TO 'LOGIN'@'%';
GRANT EXECUTE ON PROCEDURE rubrica.sp_refresh_session  TO 'LOGIN'@'%';
GRANT EXECUTE ON FUNCTION  rubrica.fn_username_from_token TO 'LOGIN'@'%';

GRANT EXECUTE ON PROCEDURE rubrica.sp_inserisci_persona TO 'LOGGED'@'%';
GRANT EXECUTE ON PROCEDURE rubrica.sp_modifica_persona  TO 'LOGGED'@'%';
GRANT EXECUTE ON PROCEDURE rubrica.sp_elimina_persona   TO 'LOGGED'@'%';
GRANT EXECUTE ON PROCEDURE rubrica.sp_get_rubrica       TO 'LOGGED'@'%';
GRANT EXECUTE ON PROCEDURE rubrica.sp_refresh_session   TO 'LOGGED'@'%';
GRANT EXECUTE ON PROCEDURE rubrica.sp_logout            TO 'LOGGED'@'%';
GRANT EXECUTE ON FUNCTION  rubrica.fn_username_from_token TO 'LOGGED'@'%';

FLUSH PRIVILEGES;

-- ==========================================================
-- EVENT SCHEDULER: pulizia token scaduti
-- ==========================================================
SET GLOBAL event_scheduler = ON;

DROP EVENT IF EXISTS ev_purge_expired_tokens;
CREATE EVENT ev_purge_expired_tokens
    ON SCHEDULE EVERY 15 MINUTE
    DO
      DELETE FROM LoginSession WHERE expires_at <= NOW();

-- ==========================================================
-- Esempi d'uso
-- ----------------------------------------------------------
-- CALL sp_register('alice','Password123!');
-- SET @tok := NULL; CALL sp_login('alice','Password123!', @tok); SELECT @tok;
-- CALL sp_inserisci_persona(@tok, 'Mario','Rossi','Via Roma 1','3201234567',30);
-- CALL sp_get_rubrica(@tok);
-- CALL sp_modifica_persona(@tok, '3201234567','Mario','Rossi','Via Milano 2',31);
-- CALL sp_elimina_persona(@tok, '3201234567');
-- CALL sp_refresh_session(@tok);
-- CALL sp_logout(@tok);
