package it.dissanahmed.util;

import java.io.File;
import java.net.URISyntaxException;

/**
 * Classe di utilità che fornisce metodi di supporto per la gestione e la risoluzione
 * dei percorsi del file system relativi a una determinata classe di riferimento.
 * <p>
 * Questa classe non può essere istanziata ed è destinata a offrire
 * esclusivamente metodi statici.
 * </p>
 */
public final class PathUtils {

        /**
         * Costante che rappresenta il nome della directory o della risorsa
         * utilizzata per contenere file informativi o metadati.
         */
        public static final String INFORMAZIONI = "informazioni";

        /**
         * Costruttore privato per impedire l'istanza di questa classe di utilità.
         */
        private PathUtils() {}

        /**
         * Determina la directory base della sorgente di codice dalla quale
         * è stata caricata la classe specificata.
         * <p>
         * Il metodo tenta di risolvere la directory base come segue:
         * <ul>
         *   <li>Se la classe è stata caricata da un file JAR, restituisce la directory padre di quel JAR.</li>
         *   <li>Se la classe è stata caricata da una directory, restituisce la directory padre di quella directory.</li>
         *   <li>Se nessuno dei casi precedenti si applica o la risoluzione fallisce, restituisce
         *       la directory di lavoro corrente (ovvero {@code System.getProperty("user.dir")}).</li>
         * </ul>
         * </p>
         *
         * @param ref la classe di riferimento ({@link Class}) la cui posizione
         *            di origine del codice sarà utilizzata per determinare la directory base.
         * @return un oggetto {@link File} che rappresenta la directory base risolta.
         */
        public static File getBaseDirectory(Class<?> ref) {
                try {
                        File location = new File(ref.getProtectionDomain()
                                .getCodeSource().getLocation().toURI());

                        if (location.isFile()) {
                                return location.getParentFile();
                        }

                        if (location.isDirectory()) {
                                File parent = location.getParentFile();
                                if (parent != null && parent.exists()) {
                                        return parent;
                                }
                        }
                        return new File(System.getProperty("user.dir"));
                } catch (URISyntaxException e) {
                        return new File(System.getProperty("user.dir"));
                }
        }
}
