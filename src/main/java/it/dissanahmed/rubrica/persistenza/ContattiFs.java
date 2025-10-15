package it.dissanahmed.rubrica.persistenza;

import it.dissanahmed.rubrica.Persona;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

public class ContattiFs implements ContattiDao {

        private static final String EXT = ".txt";
        private static final String SEP = ";";
        private static final Charset CHARSET = StandardCharsets.UTF_8;
        private static final String NL = System.lineSeparator();
        private static final String INFORMATION = "informazioni.txt";
        private static final String INFO_FILE = "info.txt";

        private final Path baseDir;
        private final @Nullable String username;

        private List<Persona> localCache;
        private boolean enableCleanup = false;

        /**
         * Costruttore principale che consente di specificare la directory base
         * e l’eventuale username.
         *
         * @param baseDir  la directory base di salvataggio.
         * @param username il nome utente (può essere {@code null}); se specificato,
         *                 abilita la modalità multiutente.
         */
        public ContattiFs(@NotNull Path baseDir, @Nullable String username) {
                this.baseDir = Objects.requireNonNull(baseDir);
                if (username != null) {
                        this.enableCleanup = true;
                }
                this.username = (username == null || username.isBlank()) ? null : username.trim();


        }
        /**
         * Restituisce la lista completa dei contatti caricandoli dal file system.
         * <p>
         * I dati vengono letti in modo lazy: se è presente una cache locale,
         * questa viene restituita; altrimenti viene effettuata una lettura completa.
         * </p>
         *
         * @return la lista dei contatti salvati.
         */
        @Override
        public synchronized List<Persona> getContatti() {
                if (localCache != null) return new ArrayList<>(localCache);
                localCache = readAllFromUserDir();
                System.out.println(baseDir.toString());
                return new ArrayList<>(localCache);
        }
        /**
         * Salva un’intera lista di contatti sul file system.
         * <p>
         * In modalità multiutente, ogni contatto è salvato in un file separato.
         * In modalità singola, tutti i contatti vengono salvati in un file unico.
         * </p>
         *
         * @param personaList la lista di persone da salvare.
         */
        @Override
        public synchronized void salvaContatti(List<Persona> personaList) {
                if (username == null) {
                        writeAllToSingleFile(personaList);
                        localCache = new ArrayList<>(personaList);
                        return;
                }

                List<Persona> nuovaLista = personaList == null ? Collections.emptyList() : new ArrayList<>(personaList);
                ensureUserDirExists();

                if (localCache == null) localCache = readAllFromUserDir();

                Map<String, Persona> attuali = indexByTelefono(localCache);
                Map<String, Persona> nuovi = indexByTelefono(nuovaLista);

                for (Map.Entry<String, Persona> e : nuovi.entrySet()) {
                        String tel = e.getKey();
                        Persona pNuova = e.getValue();
                        Persona pAttuale = attuali.get(tel);
                        upsertPersonaFile(pAttuale, pNuova);
                }

                for (String tel : attuali.keySet()) {
                        if (!nuovi.containsKey(tel)) doRimuoviPersonaDalFile(attuali.get(tel));
                }

                localCache = new ArrayList<>(nuovaLista);
                cleanupOrphans();
        }
        /**
         * Salva o aggiorna una singola persona nella rubrica.
         *
         * @param persona la persona da salvare o aggiornare.
         * @throws IllegalArgumentException se {@code persona} o il numero di telefono sono null.
         */
        @Override
        public synchronized void salvaPersona(Persona persona) {
                if (persona == null || persona.getTelefono() == null)
                        throw new IllegalArgumentException("Persona o telefono null");

                ensureUserDirExists();
                if (localCache == null) localCache = readAllFromUserDir();

                if (username == null) {
                        upsertSingleFile(persona);
                        int idx = indexByTelefono(localCache, persona.getTelefono());
                        if (idx >= 0) localCache.set(idx, persona);
                        else localCache.add(persona);
                        return;
                }

                Path dir = ensureUserDirExists();
                Path file = dir.resolve(fileNameFor(persona));
                try {
                        Files.writeString(file, serialize(persona) + NL, CHARSET,
                                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                } catch (IOException e) {
                        throw new RuntimeException("Errore salvataggio persona: " + e.getMessage(), e);
                }

                int idx = indexByTelefono(localCache, persona.getTelefono());
                if (idx >= 0) localCache.set(idx, persona);
                else localCache.add(persona);
        }


        /**
         * Modifica le informazioni di una persona già esistente, eventualmente rinominando il file.
         *
         * @param aggiornata la persona con i nuovi dati aggiornati.
         * @param oldTel     il vecchio numero di telefono per individuare la voce originale.
         * @throws IllegalArgumentException se {@code aggiornata} o il suo telefono sono null.
         */
        @Override
        public synchronized void modificaPersona(Persona aggiornata, String oldTel) {
                if (aggiornata == null || aggiornata.getTelefono() == null)
                        throw new IllegalArgumentException("Persona o telefono null");

                ensureUserDirExists();
                if (localCache == null) localCache = readAllFromUserDir();

                if (username == null) {
                        upsertSingleFile(aggiornata);
                        int idx = indexByTelefono(localCache, aggiornata.getTelefono());
                        if (idx >= 0) localCache.set(idx, aggiornata);
                        else localCache.add(aggiornata);
                        return;
                }

                Persona vecchia = null;
                int idxOld = indexByTelefono(localCache, oldTel);
                if (idxOld >= 0) {
                        vecchia = localCache.get(idxOld);
                } else {
                        for (Persona p : localCache) {
                                if (safeEq(p.getNome(), aggiornata.getNome()) && safeEq(p.getCognome(),
                                        aggiornata.getCognome())) {
                                        vecchia = p;
                                        break;
                                }
                        }
                }

                Path dir = ensureUserDirExists();
                Path nuovoFile = dir.resolve(fileNameFor(aggiornata));
                try {
                        Files.writeString(nuovoFile, serialize(aggiornata) + NL, CHARSET,
                                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                        if (vecchia != null) {
                                Path vecchioFile = dir.resolve(fileNameFor(vecchia));
                                if (!nuovoFile.equals(vecchioFile)) {
                                        Files.deleteIfExists(vecchioFile);
                                }
                        } else {
                                deleteDuplicatesByName(dir, aggiornata);
                        }
                } catch (IOException e) {
                        throw new RuntimeException("Errore modifica persona: " + e.getMessage(), e);
                }

                if (vecchia != null) {
                        localCache.remove(vecchia);
                } else {
                        int i = indexByTelefono(localCache, aggiornata.getTelefono());
                        if (i >= 0) localCache.remove(i);
                }
                localCache.add(aggiornata);

                cleanupOrphans();
        }

        private void upsertSingleFile(@NotNull Persona persona) {
                Path file = resolveUserDir().resolve(INFORMATION);
                List<Persona> all = readFromSingleFile(file);

                int idx = indexByTelefono(all, persona.getTelefono());
                if (idx >= 0) all.set(idx, persona);
                else all.add(persona);

                writeAllToSingleFile(all);
        }


        private boolean safeEq(String a, String b) {
                if (a == null && b == null) return true;
                if (a == null || b == null) return false;
                return a.equalsIgnoreCase(b);
        }

        private void deleteDuplicatesByName(Path dir, Persona aggiornata) {
                String basePrefix = sanitizeForPath(aggiornata.getNome()) + "-"
                        + sanitizeForPath(aggiornata.getCognome()) + "-";
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, basePrefix + "*.txt")) {
                        for (Path p : stream) {
                                if (!p.getFileName().toString().equals(fileNameFor(aggiornata))
                                        && !p.getFileName().toString().equals(INFORMATION)) {
                                        Files.deleteIfExists(p);
                                }
                        }
                } catch (IOException ignored) {
                }
        }
        /**
         * Rimuove una persona dal file system.
         *
         * @param daRimuovere la persona da eliminare.
         * @throws IllegalArgumentException se {@code daRimuovere} o il suo telefono sono null.
         */

        @Override
        public synchronized void rimuoviPersona(Persona daRimuovere) {
                if (daRimuovere == null || daRimuovere.getTelefono() == null)
                        throw new IllegalArgumentException("Persona o telefono null");

                if (localCache == null) localCache = readAllFromUserDir();

                if (username == null) {
                        removeFromSingleFile(daRimuovere);
                        localCache.removeIf(p -> Objects.equals(p.getTelefono(), daRimuovere.getTelefono()));
                        return;
                }

                String tel = daRimuovere.getTelefono();
                Path dir = ensureUserDirExists();
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*-" + sanitizeForPath(tel) + ".txt")) {
                        for (Path p : stream) Files.deleteIfExists(p);
                } catch (Exception e) {
                        throw new RuntimeException("Errore durante l'eliminazione per telefono '" + tel + "': " + e.getMessage(), e);
                }
                localCache.removeIf(p -> Objects.equals(p.getTelefono(), tel));
        }


        private @NotNull List<Persona> readAllFromUserDir() {
                Path dir = resolveUserDir();
                if (!Files.exists(dir)) return new ArrayList<>();

                if (username == null) return readFromSingleFile(dir.resolve(INFORMATION));

                List<Persona> res = new ArrayList<>();
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*" + EXT)) {
                        for (Path p : stream) {
                                if (p.getFileName().toString().equals(INFORMATION)) continue;
                                Persona persona = readPersonaFile(p);
                                if (persona != null) res.add(persona);
                        }
                } catch (IOException ignore) {
                        // ignored
                }
                return res;
        }

        private void upsertPersonaFile(@Nullable Persona attuale, @NotNull Persona nuova) {
                Path dir = ensureUserDirExists();
                Path nuovoFile = dir.resolve(fileNameFor(nuova));

                try {
                        String contenuto = serialize(nuova) + NL;

                        Path vecchioFile = attuale == null ? null : dir.resolve(fileNameFor(attuale));

                        if (vecchioFile == null || !Files.exists(vecchioFile)) {
                                Files.writeString(nuovoFile, contenuto, CHARSET,
                                        StandardOpenOption.CREATE,
                                        StandardOpenOption.TRUNCATE_EXISTING);
                                return;
                        }

                        if (!vecchioFile.equals(nuovoFile)) {
                                Files.writeString(nuovoFile, contenuto, CHARSET,
                                        StandardOpenOption.CREATE,
                                        StandardOpenOption.TRUNCATE_EXISTING);
                                Files.deleteIfExists(vecchioFile);
                        } else {
                                Files.writeString(vecchioFile, contenuto, CHARSET, StandardOpenOption.TRUNCATE_EXISTING);
                        }
                } catch (IOException e) {
                        throw new RuntimeException("Errore durante upsert della persona: " + e.getMessage(), e);
                }
        }

        private void doRimuoviPersonaDalFile(@NotNull Persona p) {
                Path dir = ensureUserDirExists();
                Path file = dir.resolve(fileNameFor(p));
                try {
                        Files.deleteIfExists(file);
                } catch (IOException e) {
                        throw new RuntimeException("Errore durante l'eliminazione del file: " + e.getMessage(), e);
                }
        }


        private void writeAllToSingleFile(List<Persona> persone) {
                Path file = resolveUserDir().resolve(INFORMATION);
                ensureUserDirExists();
                StringBuilder sb = new StringBuilder();
                for (Persona p : persone) sb.append(serialize(p)).append(NL);
                try {
                        Files.writeString(file, sb.toString(), CHARSET,
                                StandardOpenOption.CREATE,
                                StandardOpenOption.TRUNCATE_EXISTING);
                } catch (IOException e) {
                        throw new RuntimeException("Errore nel salvataggio in file unico: " + e.getMessage(), e);
                }
        }

        private List<Persona> readFromSingleFile(Path file) {
                List<Persona> res = new ArrayList<>();
                if (!Files.exists(file)) return res;
                try {
                        List<String> lines = Files.readAllLines(file, CHARSET);
                        for (String l : lines) {
                                Persona p = deserialize(l);
                                if (p != null) res.add(p);
                        }
                } catch (IOException ignored) {
                }
                return res;
        }

        private void updateSingleFile(Persona persona) {
                Path file = resolveUserDir().resolve(INFORMATION);
                List<Persona> all = readFromSingleFile(file);

                int idx = -1;
                for (int i = 0; i < all.size(); i++)
                        if (Objects.equals(all.get(i).getTelefono(), persona.getTelefono())) idx = i;

                if (idx >= 0) all.set(idx, persona);
                else all.add(persona);

                writeAllToSingleFile(all);
        }

        private void removeFromSingleFile(Persona daRimuovere) {
                Path file = resolveUserDir().resolve(INFORMATION);
                List<Persona> all = readFromSingleFile(file);
                all.removeIf(p -> Objects.equals(p.getTelefono(), daRimuovere.getTelefono()));
                writeAllToSingleFile(all);
        }

        private @Nullable Persona readPersonaFile(@NotNull Path file) {
                try {
                        String line = Files.readString(file, CHARSET).trim();
                        if (line.isEmpty()) return null;
                        return deserialize(line);
                } catch (IOException e) {
                        return null;
                }
        }

        private @NotNull Path resolveUserDir() {
                return (username == null) ? baseDir : baseDir.resolve(sanitizeForPath(username));
        }

        private @NotNull Path ensureUserDirExists() {
                Path dir = resolveUserDir();
                try {
                        Files.createDirectories(dir);
                } catch (IOException e) {
                        throw new RuntimeException("Impossibile creare la cartella utente '" + dir + "': " + e.getMessage(), e);
                }
                return dir;
        }

        private @NotNull String fileNameFor(@NotNull Persona p) {
                String nome = (p.getNome() == null ? "" : p.getNome());
                String cognome = (p.getCognome() == null ? "" : p.getCognome());
                String tel = Objects.requireNonNull(p.getTelefono(), "telefono obbligatorio");
                return sanitizeForPath(nome) + "-" + sanitizeForPath(cognome) + "-" + sanitizeForPath(tel) + EXT;
        }

        private @NotNull String sanitizeForPath(String s) {
                if (s == null) return "";
                return s.replaceAll("[^A-Za-z0-9._-]", "_");
        }

        private @NotNull String safe(String s) {
                if (s == null) return "";
                return s.replace("\r", " ").replace("\n", " ");
        }

        private @NotNull String serialize(@NotNull Persona p) {
                return String.join(SEP,
                        safe(p.getNome()),
                        safe(p.getCognome()),
                        safe(p.getIndirizzo()),
                        safe(p.getTelefono()),
                        String.valueOf(p.getEta()));
        }

        private @Nullable Persona deserialize(@NotNull String line) {
                String[] parts = line.split("\\s*" + Pattern.quote(SEP) + "\\s*", -1);
                if (parts.length != 5) return null;
                try {
                        return new Persona(parts[0], parts[1], parts[2], parts[3], Integer.parseInt(parts[4].trim()));
                } catch (Exception e) {
                        return null;
                }
        }

        private int indexByTelefono(@NotNull List<Persona> list, String tel) {
                for (int i = 0; i < list.size(); i++)
                        if (Objects.equals(tel, list.get(i).getTelefono())) return i;
                return -1;
        }

        private @NotNull Map<String, Persona> indexByTelefono(@NotNull List<Persona> list) {
                Map<String, Persona> map = new LinkedHashMap<>();
                for (Persona p : list)
                        if (p.getTelefono() != null) map.put(p.getTelefono(), p);
                return map;
        }

        /**
         * Pulisce i file orfani nel caso in cui siano presenti file non associati
         * a contatti effettivi nella cache locale.
         * <p>
         * L’operazione è abilitata solo in modalità multiutente.
         * </p>
         */
        private void cleanupOrphans() {
                if (!enableCleanup || username == null) return;
                Path dir = ensureUserDirExists();
                if (localCache == null) return;

                Set<String> validFiles = new HashSet<>();
                for (Persona p : localCache)
                        validFiles.add(sanitizeForPath(p.getNome()) + "-" +
                                sanitizeForPath(p.getCognome()) + "-" + sanitizeForPath(p.getTelefono()) + EXT);

                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.txt")) {
                        for (Path p : stream) {
                                String fileName = p.getFileName().toString();
                                if (fileName.equals(INFORMATION) || fileName.equals(INFO_FILE)) continue;
                                if (!validFiles.contains(fileName))
                                        Files.deleteIfExists(p);
                        }
                } catch (IOException e) {
                        throw new RuntimeException("Errore cleanup orfani: " + e.getMessage(), e);
                }
        }
}
