package it.dissanahmed.rubrica.persistenza;

import it.dissanahmed.login.persistenza.LoginDaoDBMS;
import it.dissanahmed.rubrica.Persona;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.*;

/**
 * Implementazione DBMS di ContattiDao che dialoga con MySQL/MariaDB
 * tramite stored procedure protette da token (sessione).
 *
 * SP usate:
 *  - sp_get_rubrica(IN p_token)
 *  - sp_inserisci_persona(IN p_token, IN nome, cognome, indirizzo, telefono, eta)
 *  - sp_modifica_persona(IN p_token, IN telefono, IN nome, cognome, indirizzo, eta)
 *  - sp_elimina_persona(IN p_token, IN telefono)
 */
public class ContattiDbms implements ContattiDao {

        private final String url;
        private final String user;         // utente DB: LOGGED
        private final String password;     // password DB
        private final String usernameApp;  // username applicativo (per cercare il token)

        public ContattiDbms(@NotNull Properties props, String usernameApp) {
                this.url = req(props, "db.url");
                this.user = req(props, "db.user.logged", props.getProperty("db.user"));
                this.password = req(props, "db.pass.logged", props.getProperty("db.password"));
                this.usernameApp = Objects.requireNonNull(usernameApp, "username applicativo nullo");
        }


        private static String req(Properties p, String key) {
                String v = p.getProperty(key);
                if (v == null || v.isBlank()) throw new IllegalArgumentException("Manca property: " + key);
                return v;
        }
        private static String req(Properties p, String key, String fallback) {
                String v = p.getProperty(key, fallback);
                if (v == null || v.isBlank()) throw new IllegalArgumentException("Manca property: " + key);
                return v;
        }

        private String token() {
                String t = LoginDaoDBMS.TOKEN_REGISTRY.get(usernameApp);
                if (t == null || t.isBlank()) {
                        throw new IllegalStateException("Sessione non valida o assente per utente '" + usernameApp + "'. Esegui il login.");
                }
                return t;
        }

        private Connection conn() throws SQLException {
                return DriverManager.getConnection(url, user, password);
        }

        @Override
        public List<Persona> getContatti() {
                String sql = "{ call sp_get_rubrica(?) }";
                List<Persona> out = new ArrayList<>();
                try (Connection c = conn();
                     CallableStatement cs = c.prepareCall(sql)) {
                        cs.setString(1, token());
                        try (ResultSet rs = cs.executeQuery()) {
                                while (rs.next()) {
                                        String nome = rs.getString("nome");
                                        String cognome = rs.getString("cognome");
                                        String indirizzo = rs.getString("indirizzo");
                                        String telefono = rs.getString("telefono");
                                        int eta = rs.getInt("eta");
                                        out.add(new Persona(nome, cognome, indirizzo, telefono, eta));
                                }
                        }
                } catch (SQLException ex) {
                        handleSqlException(ex, "Errore getContatti");
                }
                return out;
        }

        @Override
        public void salvaContatti(List<Persona> personaList) {

                List<Persona> nuovi = personaList == null ? Collections.emptyList() : new ArrayList<>(personaList);
                List<Persona> attuali = getContatti();

                Map<String, Persona> byTelNuovi = indexByTel(nuovi);
                Map<String, Persona> byTelAttuali = indexByTel(attuali);

                for (Persona p : nuovi) {
                        salvaPersona(p);
                }

                // elimina quelli non presenti più nella lista
                for (String tel : byTelAttuali.keySet()) {
                        if (!byTelNuovi.containsKey(tel)) {
                                rimuoviPersona(byTelAttuali.get(tel));
                        }
                }
        }

        @Override
        public void salvaPersona(Persona persona) {
                if (persona == null || persona.getTelefono() == null)
                        throw new IllegalArgumentException("Persona o telefono null");

                String sql = "{ call sp_inserisci_persona(?, ?, ?, ?, ?, ?) }";
                try (Connection c = conn();
                     CallableStatement cs = c.prepareCall(sql)) {
                        cs.setString(1, token());
                        cs.setString(2, persona.getNome());
                        cs.setString(3, persona.getCognome());
                        cs.setString(4, persona.getIndirizzo());
                        cs.setString(5, persona.getTelefono());
                        cs.setInt(6, persona.getEta());
                        cs.execute();
                } catch (SQLException ex) {
                        handleSqlException(ex, "Errore salvaPersona");
                }
        }

        @Override
        public void modificaPersona(Persona daModificare, String oldTel) {
                if (daModificare == null || daModificare.getTelefono() == null)
                        throw new IllegalArgumentException("Persona o telefono null");

                String sql = "{ call sp_modifica_persona(?, ?, ?, ?, ?, ?, ?) }";
                try (Connection c = conn();
                     CallableStatement cs = c.prepareCall(sql)) {
                        cs.setString(1, token());
                        cs.setString(2, oldTel);
                        cs.setString(3, daModificare.getTelefono());
                        cs.setString(4, daModificare.getNome());
                        cs.setString(5, daModificare.getCognome());
                        cs.setString(6, daModificare.getIndirizzo());
                        cs.setInt(7, daModificare.getEta());
                        cs.execute();
                } catch (SQLException ex) {
                        if ("45011".equals(ex.getSQLState())) {
                                throw new RuntimeException("Voce non trovata nella tua rubrica (tel=" + daModificare.getTelefono() + ")", ex);
                        }
                        handleSqlException(ex, "Errore modificaPersona");
                }
        }

        @Override
        public void rimuoviPersona(Persona daEliminare) {
                if (daEliminare == null || daEliminare.getTelefono() == null)
                        throw new IllegalArgumentException("Persona o telefono null");

                String sql = "{ call sp_elimina_persona(?, ?) }";
                try (Connection c = conn();
                     CallableStatement cs = c.prepareCall(sql)) {
                        cs.setString(1, token());
                        cs.setString(2, daEliminare.getTelefono());
                        cs.execute();
                } catch (SQLException ex) {
                        handleSqlException(ex, "Errore rimuoviPersona");
                }
        }

        /* ===================== helpers ===================== */

        private static Map<String, Persona> indexByTel(List<Persona> list) {
                Map<String, Persona> map = new LinkedHashMap<>();
                for (Persona p : list) {
                        if (p.getTelefono() != null) map.put(p.getTelefono(), p);
                }
                return map;
        }

        private void handleSqlException(SQLException ex, String prefix) {
                // 45010 = sessione non valida/scaduta
                if ("45010".equals(ex.getSQLState())) {
                        throw new IllegalStateException("Sessione non valida o scaduta. Esegui nuovamente il login.", ex);
                }
                // altre SQLSTATE personalizzate possono essere gestite qui
                throw new RuntimeException(prefix + ": " + ex.getMessage(), ex);
        }
}
