package it.dissanahmed.rubrica;

import it.dissanahmed.login.GestioneLogin;
import it.dissanahmed.rubrica.ex.PersonaException;
import it.dissanahmed.rubrica.persistenza.ContattiDao;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Classe che gestisce la logica applicativa della rubrica contatti.
 * <p>
 * Fornisce le funzionalità principali per:
 * <ul>
 *   <li>inserire nuovi contatti,</li>
 *   <li>modificare o rimuovere contatti esistenti,</li>
 *   <li>recuperare l’elenco completo dei contatti ordinati,</li>
 *   <li>validare i dati (nome, cognome, telefono, età).</li>
 * </ul>
 * <p>
 * I dati vengono memorizzati tramite un’istanza di {@link ContattiDao},
 * che può operare su file system o database, in base alla configurazione.
 * </p>
 */
public class GestioneContatti {

        /** Oggetto contenitore dei contatti in memoria. */
        private final Contatti contatti;

        /** DAO utilizzato per la persistenza dei contatti. */
        private final ContattiDao daoContatti;

        /** Espressione regolare per la validazione del numero di telefono. */
        private static final String telefonoRegex = "^\\+?[0-9]+$";

        /** Espressione regolare per la validazione di nome e cognome. */
        private static final String nomeCognomeRegex = "^[A-Za-zÀ-ÖØ-öø-ÿ'\\-\\s]+$";

        /** Username dell’utente loggato, se disponibile. */
        private String username = null;
        /**
         * Costruttore predefinito (senza autenticazione).
         * <p>
         * Utilizza una configurazione di default per la persistenza.
         * </p>
         */
        public GestioneContatti() {
                this(null);
        }
        /**
         * Costruttore principale che inizializza la gestione dei contatti
         * in base al controller di login fornito.
         * <p>
         * Se un utente è autenticato, i dati vengono salvati in una sezione
         * dedicata (ad esempio una sottodirectory specifica).
         * </p>
         * <p>
         * Inoltre, viene registrato un {@link Runtime#addShutdownHook(Thread)}
         * per salvare automaticamente i contatti all’arresto dell’applicazione.
         * </p>
         *
         * @param gestioneLogin il controller di login, o {@code null} se non presente.
         */
        public GestioneContatti(GestioneLogin gestioneLogin) {
                if (gestioneLogin != null) {
                        username = gestioneLogin.getLoggedUser();
                }
                this.daoContatti = ContattiDao.getInstance(username);
                this.contatti = new Contatti(daoContatti.getContatti());
                Runtime.getRuntime().addShutdownHook(new Thread(() ->
                        daoContatti.salvaContatti(this.getContatti())));
        }


        /**
         * Restituisce l’username dell’utente attualmente autenticato.
         *
         * @return l’username, oppure {@code null} se non autenticato.
         */
        public String getUsername() {
                return username;
        }
        /**
         * Inserisce una nuova persona nella rubrica, dopo aver validato tutti i campi.
         *
         * @param nome      il nome della persona.
         * @param cognome   il cognome della persona.
         * @param indirizzo l’indirizzo della persona.
         * @param telefono  il numero di telefono (solo cifre, opzionalmente con “+”).
         * @param eta       l’età della persona (deve essere ≥ 0).
         * @throws PersonaException se uno dei campi è nullo, vuoto o non valido.
         */
        public synchronized void inserisciDatiPersona(String nome, String cognome, String indirizzo,
                                                      String telefono, int eta) throws PersonaException {

                if (nome == null || cognome == null || indirizzo == null || telefono == null || eta < 0) {
                        throw new PersonaException(PersonaException.ExceptionType.INVALID_ENTRY);
                }

                if (nome.isEmpty() || cognome.isEmpty() || indirizzo.isEmpty() || telefono.isEmpty()) {
                        throw new PersonaException(PersonaException.ExceptionType.EMPTY_FIELD);
                }

                checkNomeCognome(nome, cognome);
                checkTelefono(telefono);

                Persona persona = new Persona(nome, cognome, indirizzo, telefono, eta);
                contatti.addPersona(persona);
                daoContatti.salvaPersona(persona);
        }
        /**
         * Inserisce una persona a partire da una singola stringa di input formattata.
         * <p>
         * Il formato richiesto è:
         * <pre>
         * nome;cognome;indirizzo;telefono;eta
         * </pre>
         * </p>
         *
         * @param data stringa contenente i dati della persona separati da “;”.
         * @throws PersonaException se il formato o i valori sono invalidi.
         */
        public void inserisciDatiPersona(String data) throws PersonaException {
                int eta;
                String[] info = data.split(";");
                if (info.length != 5) {
                        throw new PersonaException(PersonaException.ExceptionType.INVALID_ENTRY);
                }
                try {
                        eta = Integer.parseInt(info[4]);
                } catch (NumberFormatException nfe) {
                        throw new PersonaException(PersonaException.ExceptionType.INVALID_ENTRY,
                                " l'età deve essere un numero");
                }

                inserisciDatiPersona(info[0], info[1], info[2], info[3], eta);
        }
        /**
         * Modifica un campo di una persona esistente nella rubrica.
         * <p>
         * L’aggiornamento avviene tramite riflessione, utilizzando i metodi “set”
         * definiti nella classe {@link Persona}.
         * </p>
         *
         * @param tel   il numero di telefono della persona da modificare.
         * @param field il nome del campo da aggiornare (es. “nome”, “telefono”, “eta”).
         * @param update il nuovo valore da impostare.
         * @throws PersonaException se il campo è inesistente, il valore non è valido
         *                          o il telefono è duplicato.
         */
        public void modificaPersona(String tel, String field, String update) throws PersonaException {
                if (update == null || update.isEmpty())
                        throw new PersonaException(PersonaException.ExceptionType.INVALID_ENTRY, update);

                Persona persona = new Persona(tel);
                int index = contatti.getContatti().indexOf(persona);
                if (index < 0)
                        throw new PersonaException(PersonaException.ExceptionType.NOT_EXISTS, tel);

                Persona daModificare = contatti.getContatti().get(index);
                try {
                        if (!Persona.VALID_FIELDS.containsKey(field)) throw new NoSuchFieldException();
                        Field fieldToModify = daModificare.getClass().getDeclaredField(field);
                        Method method = daModificare.getClass().getMethod("set" + StringUtils.capitalize(field),
                                fieldToModify.getType());

                        if (fieldToModify.getType() == int.class) {
                                int eta = Integer.parseInt(update);
                                if (eta < 0) throw new NumberFormatException();
                                method.invoke(daModificare, eta);
                        } else {
                                if (field.equals("telefono")) {
                                        checkTelefono(update);
                                        checkUnAltroTelefono(daModificare, new Persona(update));
                                } else if (field.equals("nome") || field.equals("cognome")) {
                                        checkNomeCognome(
                                                field.equals("nome") ? update : daModificare.getNome(),
                                                field.equals("cognome") ? update : daModificare.getCognome()
                                        );
                                }
                                method.invoke(daModificare, update);
                        }

                        daoContatti.modificaPersona(daModificare, tel);

                } catch (NoSuchFieldException e) {
                        throw new PersonaException(PersonaException.ExceptionType.INVALID_ENTRY, " campo non valido: " + field);
                } catch (NumberFormatException nfe) {
                        throw new PersonaException(PersonaException.ExceptionType.INVALID_ENTRY, " l'età deve essere un numero positivo");
                } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                        throw new PersonaException(PersonaException.ExceptionType.INVALID_ENTRY,
                                " errore nell'accesso al metodo set" + StringUtils.capitalize(field));
                }
        }
        /**
         * Verifica che nome e cognome contengano solo caratteri validi.
         *
         * @param nome    il nome da validare.
         * @param cognome il cognome da validare.
         * @throws PersonaException se contengono caratteri non consentiti.
         */
        private void checkNomeCognome(String nome, String cognome) throws PersonaException {
                if (!nome.matches(nomeCognomeRegex) || !cognome.matches(nomeCognomeRegex)) {
                        throw new PersonaException(PersonaException.ExceptionType.INVALID_ENTRY,
                                "nome e cognome devono contenere solo lettere, spazi, apostrofi o trattini");
                }
        }
        /**
         * Verifica che la modifica del telefono non crei duplicati di persone esistenti.
         *
         * @param daModificare la persona originale da modificare.
         * @param modPersona   la persona aggiornata da confrontare.
         * @throws PersonaException se il nuovo telefono è già utilizzato da un’altra persona.
         */
        private void checkUnAltroTelefono(@NotNull Persona daModificare, Persona modPersona) throws PersonaException {
                if (!daModificare.equals(modPersona) && this.contatti.isPersonaIn(modPersona)) {
                        throw new PersonaException(PersonaException.ExceptionType.ALREADY_EXIST,
                                "non posso modificare i dati di un'altra persona");
                }
        }
        /**
         * Valida il formato del numero di telefono.
         *
         * @param telefono il numero da verificare.
         * @throws PersonaException se il formato non è valido.
         */
        private void checkTelefono(@NotNull String telefono) throws PersonaException {
                if (!telefono.matches(telefonoRegex)) {
                        throw new PersonaException(PersonaException.ExceptionType.WRONG_PHONE_NUMBER, telefono);
                }
        }
        /**
         * Restituisce la lista di contatti ordinata alfabeticamente.
         *
         * @return una lista di {@link Persona}, ordinata secondo {@link Persona#compareTo(Persona)}.
         */
        public synchronized List<Persona> getContatti() {
                return contatti.getContatti().stream().sorted(Persona::compareTo).toList();
        }
        /**
         * Rimuove una persona dalla rubrica in base al numero di telefono.
         *
         * @param telefono il numero di telefono della persona da eliminare.
         * @return {@code true} se la persona è stata rimossa, {@code false} altrimenti.
         */
        public boolean rimuoviPersona(String telefono) {
                List<Persona> persone = this.contatti.getContatti();
                Persona daEliminare = persone.get(persone.indexOf(new Persona(telefono)));
                this.daoContatti.rimuoviPersona(daEliminare);
                return this.contatti.getContatti().remove(daEliminare);
        }
}
