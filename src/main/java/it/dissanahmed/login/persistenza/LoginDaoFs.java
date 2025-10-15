package it.dissanahmed.login.persistenza;

import it.dissanahmed.login.*;
import it.dissanahmed.login.ex.ProblemaAutenticazione;
import it.dissanahmed.login.ex.UtenteGiaEsiste;
import it.dissanahmed.login.ex.UtenteNonTrovato;
import it.dissanahmed.login.util.PasswordHasher;
import it.dissanahmed.util.PathUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/*
 * Implementazione FS:
 * - Struttura: <baseDir>/<username>/info.txt
 * - signUp: si aspetta password già hashata -> salva "username,hashedPassword"
 * - signIn: riceve password in chiaro -> legge info.txt e verifica con PasswordHasher.verify
 */
public class LoginDaoFs implements LoginDao {

        private final Path baseDir;

        public LoginDaoFs(Path baseDir) {
                this.baseDir = baseDir;
        }

        @Override
        public void signUp(UtenteLogin user) throws UtenteGiaEsiste {
                Path userDir = baseDir.resolve(PathUtils.INFORMAZIONI + File.separator + user.getUsername());
                Path info = userDir.resolve("info.txt");

                try {

                        if (!Files.exists(baseDir)) {
                                Files.createDirectories(baseDir);
                        }

                        if (Files.exists(userDir)) {
                                throw new UtenteGiaEsiste("Utente già esistente: " + user.getUsername());
                        }
                        Files.createDirectories(userDir);

                        String line = user.getUsername() + "," + PasswordHasher.hash(user.getPassword());
                        Files.writeString(info, line + System.lineSeparator(), StandardCharsets.UTF_8,
                                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                } catch (UtenteGiaEsiste e) {
                        throw e;
                } catch (IOException e) {
                        throw new RuntimeException("Errore FS in signUp: " + e.getMessage(), e);
                }
        }

        @Override
        public void signIn(UtenteLogin user) throws UtenteNonTrovato, ProblemaAutenticazione {
                Path userDir = baseDir.resolve(PathUtils.INFORMAZIONI + File.separator + user.getUsername());
                Path info = userDir.resolve("info.txt");

                try {

                        if (!Files.exists(userDir) || !Files.isDirectory(userDir) || !Files.exists(info)) {
                                throw new UtenteNonTrovato("Utente inesistente: " + user.getUsername());
                        }

                        String content = Files.readString(info, StandardCharsets.UTF_8).trim();

                        String[] parts = content.split(",", 2);
                        if (parts.length != 2) {
                                throw new IllegalStateException("Formato info.txt non valido per utente: " + user.getUsername());
                        }
                        String storedUsername = parts[0].trim();
                        String storedHash = parts[1].trim();

                        if (!storedUsername.equals(user.getUsername())) {
                                throw new IllegalStateException("Username nel file non coincide: " + storedUsername);
                        }
                        boolean ok = PasswordHasher.verify(user.getPassword(), storedHash);
                        if (!ok) {
                                throw new ProblemaAutenticazione("Password non corretta");
                        }
                } catch (UtenteNonTrovato | ProblemaAutenticazione e) {
                        throw e;
                } catch (IOException e) {
                        throw new RuntimeException("Errore FS in signIn: " + e.getMessage(), e);
                }
        }
}
