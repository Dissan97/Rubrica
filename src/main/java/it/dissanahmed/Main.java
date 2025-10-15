package it.dissanahmed;


import it.dissanahmed.gui.HomeMenu;
import it.dissanahmed.gui.LoginGUI;
import it.dissanahmed.gui.Personalizzazione;
import it.dissanahmed.login.GestioneLogin;
import it.dissanahmed.rubrica.ex.PersonaException;


import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

import static it.dissanahmed.login.persistenza.LoginDao.openConfStream;


public class Main {


        static final Logger LOGGER = Logger.getLogger("Main");

        public static void main(String[] args) throws PersonaException {
                Personalizzazione.init();
                if (args.length > 0 && args[0].equals("normal")) {
                        try {
                                Properties props = new Properties();
                                try (InputStream fis = openConfStream()) {
                                        props.load(fis);
                                        main(new String[0]);
                                        return;
                                }
                        } catch (IOException ignored) {
                                // ignored
                        }
                        HomeMenu homeMenu = new HomeMenu();
                        homeMenu.launch();
                        return;
                }
                SwingUtilities.invokeLater(() -> {
                        GestioneLogin controller = new GestioneLogin();
                        new LoginGUI(controller).setVisible(true);
                });
        }

}