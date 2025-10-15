package it.dissanahmed.login.persistenza;

import it.dissanahmed.login.UtenteLogin;
import it.dissanahmed.login.ex.ProblemaAutenticazione;
import it.dissanahmed.login.ex.UtenteGiaEsiste;
import it.dissanahmed.login.ex.UtenteNonTrovato;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class LoginDaoDBMS implements LoginDao {

        private static final Logger LOG = Logger.getLogger(LoginDaoDBMS.class.getName());

        private final String url;
        private final String user;
        private final String password;

        public static final Map<String, String> TOKEN_REGISTRY = new HashMap<>();

        public LoginDaoDBMS(String url, String user, String password) {
                this.url = url;
                this.user = user;         // Deve essere l'utente DB LOGIN
                this.password = password;
        }

        @Override
        public void signUp(UtenteLogin u) throws UtenteGiaEsiste {
                final String call = "{ call rubrica.sp_register(?, ?) }";
                try (Connection c = DriverManager.getConnection(url, user, password);
                     CallableStatement cs = c.prepareCall(call)) {
                        cs.setString(1, u.getUsername());
                        cs.setString(2, u.getPassword());
                        cs.execute();
                } catch (SQLException ex) {
                        logSql("signUp", ex);
                        if ("45002".equals(ex.getSQLState())) {
                                throw new UtenteGiaEsiste(u.getUsername());
                        }
                        throw new RuntimeException("Errore signUp DBMS: " + ex.getMessage(), ex);
                }
        }

        @Override
        public void signIn(UtenteLogin u) throws UtenteNonTrovato, ProblemaAutenticazione {
                final String call = "{ call rubrica.sp_login(?, ?, ?) }";
                try (Connection c = DriverManager.getConnection(url, user, password);
                     CallableStatement cs = c.prepareCall(call)) {
                        cs.setString(1, u.getUsername());
                        cs.setString(2, u.getPassword());      // plain; verifica in SP
                        cs.registerOutParameter(3, Types.VARCHAR); // OUT CHAR(64) -> ok anche VARCHAR

                        // Esegui
                        cs.execute();

                        // (alcuni driver lasciano OUT già disponibile; se vuoi essere super safe,
                        // puoi consumare eventuali resultset: while (cs.getMoreResults() || cs.getUpdateCount() != -1) {})

                        String token = cs.getString(3);
                        if (token == null || token.isBlank()) {
                                throw new ProblemaAutenticazione("Token non ricevuto dal server");
                        }
                        TOKEN_REGISTRY.put(u.getUsername(), token);
                } catch (SQLException ex) {
                        logSql("signIn", ex);
                        if ("45000".equals(ex.getSQLState())) {
                                throw new UtenteNonTrovato(u.getUsername());
                        }
                        if ("45001".equals(ex.getSQLState())) {
                                throw new ProblemaAutenticazione("Password errata");
                        }
                        throw new RuntimeException("Errore signIn DBMS: " + ex.getMessage(), ex);
                }
        }

        private void logSql(String phase, SQLException ex) {
                LOG.severe(() -> String.format(
                        "[%s] SQLState=%s, ErrorCode=%d, Msg=%s",
                        phase, ex.getSQLState(), ex.getErrorCode(), ex.getMessage()
                ));
        }
}
