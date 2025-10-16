package it.dissanahmed.login.persistenza;

import it.dissanahmed.login.ex.ProblemaAutenticazione;
import it.dissanahmed.login.UtenteLogin;
import it.dissanahmed.login.ex.UtenteGiaEsiste;
import it.dissanahmed.login.ex.UtenteNonTrovato;
import it.dissanahmed.rubrica.persistenza.ContattiDao;
import it.dissanahmed.util.PathUtils;

import java.io.*;
import java.util.Properties;


public interface LoginDao {

        void signUp(UtenteLogin user) throws UtenteGiaEsiste;

        void signIn(UtenteLogin user) throws UtenteNonTrovato, ProblemaAutenticazione;

        static LoginDao buildDao() {
                Properties props = new Properties();
                try (InputStream fis = openConfStream()) {
                        props.load(fis);
                } catch (IOException e) {
                        props.setProperty("database.instance", "fs");
                }

                String instance = props.getProperty("database.instance", "fs").trim().toLowerCase();
                switch (instance) {
                        case "fs":
                                return new LoginDaoFs(PathUtils.getBaseDirectory(LoginDao.class).toPath());
                        case "dbms":
                                String url  = props.getProperty("db.url");
                                String user = props.getProperty("db.user");
                                String pwd  = props.getProperty("db.password");
                                return new LoginDaoDBMS(url, user, pwd);
                        default:
                                throw new IllegalStateException("database.instance non riconosciuto: " + instance);
                }
        }

        static InputStream openConfStream() throws IOException {

                File baseDir = PathUtils.getBaseDirectory(LoginDao.class);
                File confFile = new File(baseDir, "conf" + File.separator + "conf.properties");
                if (confFile.exists()) {
                        return new FileInputStream(confFile);
                }

                confFile = new File(baseDir,"credenziali_database.properties");
                if (confFile.exists()) {
                        return new FileInputStream(confFile);
                }
                InputStream is = ContattiDao.class.getClassLoader().getResourceAsStream("conf.properties");
                if (is == null) {
                        throw new FileNotFoundException(
                                "conf.properties non trovato né accanto al JAR né nelle risorse!");
                }
                return is;
        }


}
