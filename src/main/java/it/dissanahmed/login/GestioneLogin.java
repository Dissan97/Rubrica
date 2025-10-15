package it.dissanahmed.login;


import it.dissanahmed.login.ex.ProblemaAutenticazione;
import it.dissanahmed.login.ex.UtenteGiaEsiste;
import it.dissanahmed.login.ex.UtenteNonTrovato;
import it.dissanahmed.login.persistenza.LoginDao;

import java.util.Locale;

public class GestioneLogin {

        private final LoginDao dao;
        private String loggedUser;
        public GestioneLogin() {
                this.dao = LoginDao.buildDao();
        }

        public void signUp(String username, String plainPassword) throws UtenteGiaEsiste {
                if (username == null || username.isBlank() || plainPassword == null || plainPassword.isBlank())
                        throw new IllegalArgumentException("Username e password sono obbligatori");
                username = username.toLowerCase(Locale.ROOT);
                UtenteLogin toStore = new UtenteLogin(username, plainPassword);
                dao.signUp(toStore);
        }

        public void signIn(String username, String plainPassword)
                throws UtenteNonTrovato, ProblemaAutenticazione {
                if (username == null || username.isBlank() || plainPassword == null || plainPassword.isBlank())
                        throw new IllegalArgumentException("Username e password sono obbligatori");
                username = username.toLowerCase(Locale.ROOT);
                UtenteLogin attempt = new UtenteLogin(username, plainPassword);
                dao.signIn(attempt);
                this.loggedUser = attempt.getUsername();
        }


        public String getLoggedUser() {
                return this.loggedUser;
        }
}
