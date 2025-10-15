package it.dissanahmed.login;

public class UtenteLogin {
        private final String username;
        private final String password;
        private final String token;
        public UtenteLogin(String username, String password) {
                this(username, password, null);
        }

        public UtenteLogin(String username, String password, String token) {
                if (username == null || username.isBlank()) {
                        throw new IllegalArgumentException("username mancante");
                }
                if (password == null || password.isEmpty()) {
                        throw new IllegalArgumentException("password mancante");
                }
                this.username = username;
                this.password = password;
                this.token = token;
        }

        public String getUsername() { return username; }
        public String getPassword() { return password; }
}
