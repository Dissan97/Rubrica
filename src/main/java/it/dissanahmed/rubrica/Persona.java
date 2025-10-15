package it.dissanahmed.rubrica;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Classe che rappresenta una persona nella rubrica.
 * <p>
 * Ogni persona è identificata in modo univoco dal numero di telefono,
 * che viene utilizzato nei metodi {@link #equals(Object)} e {@link #hashCode()}.
 * </p>
 * <p>
 * La classe implementa {@link Comparable} per consentire l’ordinamento
 * alfabetico per nome e cognome.
 * </p>
 */
public class Persona implements Comparable<Persona> {

        /**
         * Mappa contenente i campi validi e i relativi nomi dei metodi setter.
         * <p>
         * Questa mappa è generata dinamicamente tramite riflessione al caricamento della classe
         * e viene utilizzata per validare e modificare i campi in modo dinamico.
         * </p>
         * <p>
         * La chiave è il nome del campo (es. "nome"), mentre il valore è il nome
         * del metodo setter corrispondente (es. "setNome").
         * </p>
         */
        public static final Map<String, String> VALID_FIELDS;

        static {
                Map<String, String> map = new LinkedHashMap<>();
                for (Field field : Persona.class.getDeclaredFields()) {
                        if (field.getName().equals("VALID_FIELDS")) continue;
                        String setterName = "set" + StringUtils.capitalize(field.getName());
                        map.put(field.getName(), setterName);
                }
                VALID_FIELDS = Collections.unmodifiableMap(map);
        }

        private String nome;
        private String cognome;
        private String indirizzo;
        private String telefono;
        private int eta;

        /**
         * Costruttore completo che inizializza tutti i campi di una persona.
         *
         * @param nome      il nome della persona.
         * @param cognome   il cognome della persona.
         * @param indirizzo l’indirizzo della persona.
         * @param telefono  il numero di telefono, utilizzato come identificatore univoco.
         * @param eta       l’età della persona.
         */
        public Persona(String nome, String cognome, String indirizzo, String telefono, int eta) {
                this.nome = nome;
                this.cognome = cognome;
                this.indirizzo = indirizzo;
                this.telefono = telefono;
                this.eta = eta;
        }

        /**
         * Costruttore che crea una persona con solo il numero di telefono.
         * <p>
         * Utile per operazioni di ricerca, confronto o eliminazione,
         * dove il telefono è l’unico identificatore necessario.
         * </p>
         *
         * @param tel il numero di telefono della persona.
         */
        public Persona(String tel) {
                this.telefono = tel;
        }

        /** @return il nome della persona. */
        public String getNome() { return nome; }

        /** @param nome imposta il nome della persona. */
        public void setNome(String nome) { this.nome = nome; }

        /** @return il cognome della persona. */
        public String getCognome() { return cognome; }

        /** @param cognome imposta il cognome della persona. */
        public void setCognome(String cognome) { this.cognome = cognome; }

        /** @return l’indirizzo della persona. */
        public String getIndirizzo() { return indirizzo; }

        /** @param indirizzo imposta l’indirizzo della persona. */
        public void setIndirizzo(String indirizzo) { this.indirizzo = indirizzo; }

        /** @return il numero di telefono della persona. */
        public String getTelefono() { return telefono; }

        /** @param telefono imposta il numero di telefono della persona. */
        public void setTelefono(String telefono) { this.telefono = telefono; }

        /** @return l’età della persona. */
        public int getEta() { return eta; }

        /** @param eta imposta l’età della persona. */
        public void setEta(int eta) { this.eta = eta; }

        /**
         * Restituisce una rappresentazione testuale della persona.
         *
         * @return una stringa nel formato:
         *         <pre>{nome; cognome; indirizzo; telefono; eta}</pre>
         */
        @Override
        public String toString() {
                return "{" + nome +
                        "; " + cognome +
                        "; " + indirizzo +
                        "; " + telefono +
                        "; " + eta + '}';
        }

        /**
         * Due persone sono considerate uguali se hanno lo stesso numero di telefono.
         *
         * @param o l’oggetto da confrontare.
         * @return {@code true} se i numeri di telefono coincidono, {@code false} altrimenti.
         */
        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Persona persona)) return false;
                return Objects.equals(getTelefono(), persona.getTelefono());
        }

        /**
         * Calcola l’hash della persona basato sul numero di telefono.
         *
         * @return l’hash code derivato da {@link #getTelefono()}.
         */
        @Override
        public int hashCode() {
                return Objects.hash(getTelefono());
        }

        /**
         * Restituisce un array contenente tutti i campi della persona in ordine:
         * nome, cognome, indirizzo, telefono, età.
         *
         * @return un array di oggetti rappresentante la persona.
         */
        public Object[] getRawPersona() {
                return new Object[] {
                        this.getNome(),
                        this.getCognome(),
                        this.getIndirizzo(),
                        this.getTelefono(),
                        this.getEta()
                };
        }

        /**
         * Confronta due persone in base a nome e cognome.
         * <p>
         * L’ordinamento è alfabetico crescente ed è utile per visualizzare
         * la rubrica in ordine logico.
         * </p>
         *
         * @param persona la persona da confrontare.
         * @return un valore negativo, zero o positivo a seconda dell’ordine.
         */
        @Override
        public int compareTo(@NotNull Persona persona) {
                return (this.getNome() + this.getCognome())
                        .compareTo(persona.getNome() + persona.getCognome());
        }
}
