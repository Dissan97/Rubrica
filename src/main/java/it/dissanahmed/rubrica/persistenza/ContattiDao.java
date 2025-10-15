package it.dissanahmed.rubrica.persistenza;

import it.dissanahmed.rubrica.Persona;
import it.dissanahmed.util.PathUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static it.dissanahmed.login.persistenza.LoginDao.openConfStream;

/**
 * Interfaccia che definisce le operazioni di persistenza per la gestione
 * dei contatti in una rubrica.
 * <p>
 * Fornisce metodi per ottenere, salvare, modificare e rimuovere oggetti
 * {@link Persona}, e un meccanismo statico per ottenere un'implementazione
 * concreta del DAO basata sulla configurazione di sistema.
 * </p>
 */
public interface ContattiDao {

        /**
         * Restituisce la lista completa dei contatti presenti nella rubrica.
         *
         * @return una lista di oggetti {@link Persona} rappresentanti i contatti memorizzati.
         */
        List<Persona> getContatti();

        /**
         * Salva una lista di contatti sovrascrivendo i dati esistenti.
         *
         * @param personaList la lista di persone da salvare.
         */
        void salvaContatti(List<Persona> personaList);

        /**
         * Salva una singola persona nella rubrica.
         *
         * @param persona la persona da salvare.
         */
        void salvaPersona(Persona persona);

        /**
         * Rimuove una persona dalla rubrica.
         *
         * @param daEliminare la persona da eliminare.
         */
        void rimuoviPersona(Persona daEliminare);

        /**
         * Modifica le informazioni di una persona già presente nella rubrica.
         *
         * @param daModificare la persona con i nuovi dati aggiornati.
         * @param oldTel       il numero di telefono precedente utilizzato per individuare la voce da aggiornare.
         */
        void modificaPersona(Persona daModificare, String oldTel);

        /**
         * Restituisce un'istanza predefinita di {@link ContattiDao} in base
         * alla configurazione specificata nel file <code>conf.properties</code>.
         * <p>
         * Se non viene fornito alcuno username, il comportamento sarà equivalente
         * a {@link #getInstance(String)} con parametro <code>null</code>.
         * </p>
         *
         * @return un'istanza di {@link ContattiDao} secondo la configurazione.
         */
        static ContattiDao getInstance() {
                return getInstance(null);
        }

        /**
         * Restituisce un'istanza concreta di {@link ContattiDao} in base
         * alle impostazioni definite nel file <code>conf.properties</code>.
         * <p>
         * Il metodo legge la proprietà <code>database.instance</code> per determinare
         * il tipo di implementazione:
         * <ul>
         *   <li><b>dbms</b>: utilizza {@link ContattiDbms}, con connessione a un database relazionale.</li>
         *   <li><b>fs</b> (default): utilizza {@link ContattiFs}, basato su file system locale.</li>
         * </ul>
         * </p>
         * <p>
         * Inoltre, se la modalità <b>fs</b> è selezionata, viene determinata la
         * directory base tramite {@link PathUtils#getBaseDirectory(Class)} e
         * la proprietà <code>fs.baseDir</code>.
         * </p>
         *
         * @param username l'username dell'utente corrente, utilizzato per gestire
         *                 directory o schemi dedicati; può essere <code>null</code>.
         * @return un'istanza di {@link ContattiDao} conforme alla configurazione.
         * @throws IllegalArgumentException se si verifica un errore nella lettura
         *                                  del file di configurazione.
         */
        static ContattiDao getInstance(String username) {
                Properties properties = new Properties();
                try (InputStream inputStream = openConfStream()) {
                        properties.load(inputStream);
                } catch (IOException e) {
                        throw new IllegalArgumentException("Errore nella lettura di conf.properties: " + e.getMessage(), e);
                }

                String instanceType = properties.getProperty("database.instance", "fs").trim().toLowerCase();

                switch (instanceType) {
                        case "dbms": {
                                return new ContattiDbms(properties, normalize(username));
                        }
                        case "fs":
                        default: {
                                String baseDirName = properties.getProperty("fs.baseDir",
                                        PathUtils.INFORMAZIONI).trim();
                                File confDir = PathUtils.getBaseDirectory(ContattiDao.class);
                                Path baseDir = confDir.toPath().resolve(baseDirName);
                                return new ContattiFs(baseDir, normalize(username));
                        }
                }
        }

        /**
         * Normalizza una stringa eliminando spazi vuoti e restituendo {@code null}
         * se la stringa risulta vuota dopo il trim.
         *
         * @param s la stringa da normalizzare.
         * @return la stringa normalizzata o {@code null} se vuota o nulla.
         */
        private static String normalize(String s) {
                if (s == null) return null;
                String x = s.trim();
                return x.isEmpty() ? null : x;
        }
}
