package it.dissanahmed.rubrica;

import it.dissanahmed.login.GestioneLogin;
import it.dissanahmed.rubrica.ex.PersonaException;
import it.dissanahmed.rubrica.persistenza.ContattiDao;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tutti i test qui controllano:
 *  - eccezioni e messaggi per input non validi
 *  - effetti lato "modello" (lista contatti in memoria)
 *  - interazioni con il DAO (verifica chiamate)
 * Non verifichiamo dettagli d'implementazione interni (riflessione, campi privati, ecc.),
 * ma solo API pubbliche/contratti osservabili.
 */
class GestioneContattiTest {

        private MockedStatic<ContattiDao> contattiDaoStatic;  // mock per getInstance(username)
        private ContattiDao dao;                               // mock dell'istanza DAO
        private GestioneLogin login;                         // mock del login controller
        private List<Persona> backingStore;                    // "db" in memoria per simulare il DAO

        private GestioneContatti nuovaGestioneContatti() {
                // il costruttore usa ContattiDao.getInstance(username) e dao.getContatti()
                when(login.getLoggedUser()).thenReturn("mario");
                when(dao.getContatti()).thenReturn(backingStore);
                contattiDaoStatic.when(() -> ContattiDao.getInstance("mario")).thenReturn(dao);
                return new GestioneContatti(login);
        }

        @BeforeEach
        void setUp() {
                contattiDaoStatic = mockStatic(ContattiDao.class);
                dao = mock(ContattiDao.class);
                login = mock(GestioneLogin.class);
                backingStore = new ArrayList<>();
        }

        @AfterEach
        void tearDown() {
                contattiDaoStatic.close();
        }

        // ------------------------------------------------------------
        // Costruttore / bootstrap
        // ------------------------------------------------------------

        @Test
        void costruzione_conLogin_impostaUsername_e_caricaContattiDaDao() {
                backingStore.add(new Persona("Anna", "Bianchi", "Via A", "+39111", 30));
                GestioneContatti gc = nuovaGestioneContatti();

                assertThat(gc.getUsername()).isEqualTo("mario");
                assertThat(gc.getContatti())
                        .extracting(Persona::getTelefono)
                        .containsExactly("+39111");

                verify(dao, times(1)).getContatti();
                verifyNoMoreInteractions(dao);
        }

        // ------------------------------------------------------------
        // inserisciDatiPersona (overload completo)
        // ------------------------------------------------------------

        @Test
        void inserisciDatiPersona_valida_aggiungePersona_e_salvaSuDao() throws Exception {
                GestioneContatti gc = nuovaGestioneContatti();

                gc.inserisciDatiPersona("Mario", "Rossi", "Via Roma 1", "+391234", 25);

                assertThat(gc.getContatti())
                        .hasSize(1)
                        .first()
                        .matches(p -> p.getNome().equals("Mario") &&
                                p.getCognome().equals("Rossi") &&
                                p.getIndirizzo().equals("Via Roma 1") &&
                                p.getTelefono().equals("+391234") &&
                                p.getEta() == 25);

                verify(dao, times(1)).salvaPersona(argThat(p ->
                        p.getNome().equals("Mario") &&
                                p.getCognome().equals("Rossi") &&
                                p.getTelefono().equals("+391234")));
        }

        @Test
        void inserisciDatiPersona_null_o_etaNegativa_lancia_INVALID_ENTRY() {
                GestioneContatti gc = nuovaGestioneContatti();

                assertThatThrownBy(() -> gc.inserisciDatiPersona(null, "Rossi", "Via", "+39", 1))
                        .isInstanceOf(PersonaException.class)
                        .hasMessageContaining("entry non valida");

                assertThatThrownBy(() -> gc.inserisciDatiPersona("Mario", "Rossi", "Via", "+39", -1))
                        .isInstanceOf(PersonaException.class)
                        .hasMessageContaining("entry non valida");
        }

        @Test
        void inserisciDatiPersona_vuoti_lancia_EMPTY_FIELD() {
                GestioneContatti gc = nuovaGestioneContatti();

                assertThatThrownBy(() -> gc.inserisciDatiPersona("", "Rossi", "Via", "+39", 10))
                        .isInstanceOf(PersonaException.class)
                        .hasMessageContaining("entry vuote");
                assertThatThrownBy(() -> gc.inserisciDatiPersona("Mario", "", "Via", "+39", 10))
                        .isInstanceOf(PersonaException.class)
                        .hasMessageContaining("entry vuote");
                assertThatThrownBy(() -> gc.inserisciDatiPersona("Mario", "Rossi", "", "+39", 10))
                        .isInstanceOf(PersonaException.class)
                        .hasMessageContaining("entry vuote");
                assertThatThrownBy(() -> gc.inserisciDatiPersona("Mario", "Rossi", "Via", "", 10))
                        .isInstanceOf(PersonaException.class)
                        .hasMessageContaining("entry vuote");
        }

        @Test
        void inserisciDatiPersona_telefonoInvalido_lancia_WRONG_PHONE_NUMBER() {
                GestioneContatti gc = nuovaGestioneContatti();

                assertThatThrownBy(() -> gc.inserisciDatiPersona("Mario", "Rossi", "Via", "abc123", 10))
                        .isInstanceOf(PersonaException.class)
                        .hasMessageContaining("numero di telefono non è valido");
        }

        @Test
        void inserisciDatiPersona_nomeCognomeInvalidi_lancia_INVALID_ENTRY() {
                GestioneContatti gc = nuovaGestioneContatti();

                assertThatThrownBy(() -> gc.inserisciDatiPersona("Mar1o", "Rossi", "Via", "+39", 10))
                        .isInstanceOf(PersonaException.class)
                        .hasMessageContaining("entry non valida");
                assertThatThrownBy(() -> gc.inserisciDatiPersona("Mario", "Ro$$i", "Via", "+39", 10))
                        .isInstanceOf(PersonaException.class)
                        .hasMessageContaining("entry non valida");
        }

        @Test
        void inserisciDatiPersona_evitaDuplicati_perTelefono() throws Exception {
                GestioneContatti gc = nuovaGestioneContatti();

                gc.inserisciDatiPersona("Mario", "Rossi", "Via", "+391", 10);

                assertThatThrownBy(() -> gc.inserisciDatiPersona("Luigi", "Verdi", "Altrove", "+391", 22))
                        .isInstanceOf(PersonaException.class)
                        .hasMessageContaining("Esiste un'altra persona con il numero di telefono");
        }

        // ------------------------------------------------------------
        // inserisciDatiPersona(String data formattata)
        // ------------------------------------------------------------

        @Test
        void inserisciDatiPersona_stringaFormatoErrato_lancia_INVALID_ENTRY() {
                GestioneContatti gc = nuovaGestioneContatti();

                assertThatThrownBy(() -> gc.inserisciDatiPersona("Mario;Rossi;Via;Tel")) // 4 campi
                        .isInstanceOf(PersonaException.class)
                        .hasMessageContaining("entry non valida");
        }

        @Test
        void inserisciDatiPersona_etaNonNumerica_lancia_INVALID_ENTRY_conMessaggio() {
                GestioneContatti gc = nuovaGestioneContatti();

                assertThatThrownBy(() -> gc.inserisciDatiPersona("Mario;Rossi;Via;+39;eta"))
                        .isInstanceOf(PersonaException.class)
                        .hasMessageContaining("entry non valida")
                        .hasMessageContaining("l'età deve essere un numero");
        }

        @Test
        void inserisciDatiPersona_stringaValida_funzionaComeOverloadCompleto() throws Exception {
                GestioneContatti gc = nuovaGestioneContatti();
                gc.inserisciDatiPersona("Mario;Rossi;Via;+39123;33");

                assertThat(gc.getContatti()).hasSize(1);
                verify(dao, times(1)).salvaPersona(any(Persona.class));
        }

        // ------------------------------------------------------------
        // modificaPersona
        // ------------------------------------------------------------

        @Test
        void modificaPersona_suContattoInesistente_lancia_NOT_EXISTS() {
                GestioneContatti gc = nuovaGestioneContatti();

                assertThatThrownBy(() -> gc.modificaPersona("+39", "indirizzo", "Nuovo"))
                        .isInstanceOf(PersonaException.class)
                        .hasMessageContaining("Non esiste la persona");
        }

        @Test
        void modificaPersona_fieldNonValido_lancia_INVALID_ENTRY() throws Exception {
                GestioneContatti gc = nuovaGestioneContatti();
                gc.inserisciDatiPersona("Mario", "Rossi", "Via", "+39", 10);

                assertThatThrownBy(() -> gc.modificaPersona("+39", "nonEsiste", "x"))
                        .isInstanceOf(PersonaException.class)
                        .hasMessageContaining("entry non valida")
                        .hasMessageContaining("campo non valido");
        }

        @Test
        void modificaPersona_etaNegativa_lancia_INVALID_ENTRY() throws Exception {
                GestioneContatti gc = nuovaGestioneContatti();
                gc.inserisciDatiPersona("Mario", "Rossi", "Via", "+39", 10);

                assertThatThrownBy(() -> gc.modificaPersona("+39", "eta", "-1"))
                        .isInstanceOf(PersonaException.class)
                        .hasMessageContaining("entry non valida")
                        .hasMessageContaining("numero positivo");
        }

        @Test
        void modificaPersona_telefonoFormatoErrato_lancia_WRONG_PHONE_NUMBER() throws Exception {
                GestioneContatti gc = nuovaGestioneContatti();
                gc.inserisciDatiPersona("Mario", "Rossi", "Via", "+39", 10);

                assertThatThrownBy(() -> gc.modificaPersona("+39", "telefono", "ABC#"))
                        .isInstanceOf(PersonaException.class)
                        .hasMessageContaining("numero di telefono non è valido");
        }

        @Test
        void modificaPersona_telefonoInUsoDaAltraPersona_lancia_ALREADY_EXIST() throws Exception {
                GestioneContatti gc = nuovaGestioneContatti();
                gc.inserisciDatiPersona("Mario", "Rossi", "Via", "+391", 10);
                gc.inserisciDatiPersona("Luigi", "Verdi", "Via", "+392", 20);

                assertThatThrownBy(() -> gc.modificaPersona("+391", "telefono", "+392"))
                        .isInstanceOf(PersonaException.class)
                        .hasMessageContaining("Esiste un'altra persona con il numero di telefono")
                        .hasMessageContaining("non posso modificare i dati di un'altra persona");
        }

        @Test
        void modificaPersona_nomeOCognomeInvalido_lancia_INVALID_ENTRY() throws Exception {
                GestioneContatti gc = nuovaGestioneContatti();
                gc.inserisciDatiPersona("Mario", "Rossi", "Via", "+39", 10);

                assertThatThrownBy(() -> gc.modificaPersona("+39", "nome", "Mar1o"))
                        .isInstanceOf(PersonaException.class)
                        .hasMessageContaining("entry non valida");

                assertThatThrownBy(() -> gc.modificaPersona("+39", "cognome", "Ro$$i"))
                        .isInstanceOf(PersonaException.class)
                        .hasMessageContaining("entry non valida");
        }

        @Test
        void modificaPersona_ok_aggiornaValori_e_chiamaDao() throws Exception {
                GestioneContatti gc = nuovaGestioneContatti();
                gc.inserisciDatiPersona("Mario", "Rossi", "Via", "+39", 10);

                gc.modificaPersona("+39", "indirizzo", "Via Nuova 5");
                gc.modificaPersona("+39", "eta", "31");

                Persona p = gc.getContatti().getFirst();
                assertThat(p.getIndirizzo()).isEqualTo("Via Nuova 5");
                assertThat(p.getEta()).isEqualTo(31);

                verify(dao, atLeastOnce()).modificaPersona(any(Persona.class), eq("+39"));
        }

        // ------------------------------------------------------------
        // getContatti (ordinamento) e rimuoviPersona
        // ------------------------------------------------------------

        @Test
        void getContatti_restituisceListaOrdinata_perNomeCognome() throws Exception {
                GestioneContatti gc = nuovaGestioneContatti();
                gc.inserisciDatiPersona("Zoe", "Verdi", "X", "+391", 10);
                gc.inserisciDatiPersona("Anna", "Bianchi", "Y", "+392", 20);

                assertThat(gc.getContatti())
                        .extracting(Persona::getNome)
                        .containsExactly("Anna", "Zoe");
        }

        @Test
        void rimuoviPersona_rimuove_daMemoria_e_chiamaDao() throws Exception {
                GestioneContatti gc = nuovaGestioneContatti();
                gc.inserisciDatiPersona("Mario", "Rossi", "Via", "+39", 10);

                boolean removed = gc.rimuoviPersona("+39");
                assertThat(removed).isTrue();
                assertThat(gc.getContatti()).isEmpty();

                verify(dao, times(1)).rimuoviPersona(argThat(p -> "+39".equals(p.getTelefono())));
        }
}
