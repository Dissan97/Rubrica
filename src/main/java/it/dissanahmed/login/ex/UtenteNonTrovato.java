package it.dissanahmed.login.ex;

public class UtenteNonTrovato extends Exception {
        public UtenteNonTrovato(String msg) { super("Utente" + msg + " non Esiste"); }
}