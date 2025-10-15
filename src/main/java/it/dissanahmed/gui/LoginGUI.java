package it.dissanahmed.gui;

import it.dissanahmed.login.GestioneLogin;
import it.dissanahmed.rubrica.GestioneContatti;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class LoginGUI extends JFrame {

        private final JTextField usernameField = new JTextField(22);
        private final JPasswordField passwordField = new JPasswordField(22);
        private final JButton signInBtn = new JButton("Accedi");
        private final JButton signUpBtn = new JButton("Registrati");
        private final JButton togglePwdBtn = new JButton();
        private boolean passwordVisible = false;
        private char defaultEchoChar;
        private final GestioneLogin controller;

        public LoginGUI(GestioneLogin controller) {
                super("Accesso Rubrica");
                this.controller = controller;
                initUI();
        }

        private void initUI() {
                setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                setLayout(new BorderLayout(0, 0));

                JPanel header = getjPanel();
                add(header, BorderLayout.NORTH);

                JPanel center = new JPanel(new GridBagLayout());
                center.setBorder(BorderFactory.createEmptyBorder(12, 28, 28, 28));
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(12, 10, 12, 10);
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.weightx = 1.0;

                Dimension fieldSize = new Dimension(320, 38);
                usernameField.putClientProperty("JTextField.placeholderText", "Inserisci username");
                passwordField.putClientProperty("JTextField.placeholderText", "Inserisci password");
                usernameField.setPreferredSize(fieldSize);
                passwordField.setPreferredSize(fieldSize);
                defaultEchoChar = passwordField.getEchoChar();

                gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.LINE_END; gbc.weightx = 0;
                JLabel lUser = new JLabel("Username:");
                lUser.setFont(new Font("Segoe UI", Font.BOLD, 14));
                center.add(lUser, gbc);
                gbc.gridx = 1; gbc.gridy = 0; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.LINE_START; gbc.weightx = 1.0;
                center.add(usernameField, gbc);

                gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.LINE_END; gbc.weightx = 0;
                JLabel lPwd = new JLabel("Password:");
                lPwd.setFont(new Font("Segoe UI", Font.BOLD, 14));
                center.add(lPwd, gbc);
                gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.LINE_START; gbc.weightx = 1.0;
                center.add(passwordField, gbc);

                togglePwdBtn.setFocusable(false);
                togglePwdBtn.setBorderPainted(false);
                togglePwdBtn.setContentAreaFilled(false);
                togglePwdBtn.setOpaque(false);
                togglePwdBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                togglePwdBtn.setPreferredSize(new Dimension(38, 38));
                updateTogglePwdIcon();
                togglePwdBtn.addActionListener(e -> togglePasswordVisibility());
                gbc.gridx = 2; gbc.gridy = 1; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.LINE_START; gbc.weightx = 0;
                center.add(togglePwdBtn, gbc);

                add(center, BorderLayout.CENTER);

                setupBtn();



                pack();
                setMinimumSize(new Dimension(560, 420));
                setSize(new Dimension(600, 460));
                setLocationRelativeTo(null);
        }

        private void setupBtn() {
                JToolBar bar = new JToolBar();
                bar.setFloatable(false);
                bar.setBorder(BorderFactory.createEmptyBorder(12, 28, 28, 28));
                signInBtn.setIcon(
                        new ImageIcon(Objects.requireNonNull(getClass()
                                .getResource("/icons/login.png")))
                );

                signUpBtn.setIcon(
                        new ImageIcon(Objects.requireNonNull(getClass()
                                .getResource("/icons/register.png")))
                );
                for (JButton b : new JButton[]{signInBtn, signUpBtn}) {
                        b.setFocusable(false);
                        b.setRolloverEnabled(true);
                        b.putClientProperty("JButton.buttonType", "roundRect");

                }
                bar.add(Box.createHorizontalGlue());
                bar.add(signInBtn);
                bar.add(Box.createHorizontalStrut(12));
                bar.add(signUpBtn);
                bar.add(Box.createHorizontalGlue());
                add(bar, BorderLayout.SOUTH);
                signInBtn.addActionListener(e -> doSignIn());
                signUpBtn.addActionListener(e -> doSignUp());
        }

        @NotNull
        private static JPanel getjPanel() {
                JPanel header = new JPanel(new BorderLayout());
                header.setBorder(BorderFactory.createEmptyBorder(28, 28, 8, 28));
                JLabel title = new JLabel("Benvenuto nella Rubrica");
                title.setFont(new Font("Segoe UI", Font.BOLD, 24));
                title.setHorizontalAlignment(SwingConstants.CENTER);
                JLabel subtitle = new JLabel("Accedi o crea un nuovo account per continuare");
                subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 15));
                subtitle.setHorizontalAlignment(SwingConstants.CENTER);
                subtitle.setForeground(new Color(69, 68, 69));
                header.add(title, BorderLayout.NORTH);
                header.add(subtitle, BorderLayout.SOUTH);
                return header;
        }

        private void doSignIn() {
                String username = usernameField.getText() == null ? "" : usernameField.getText();
                String password = new String(passwordField.getPassword());
                setBusy(true);
                try {
                        controller.signIn(username, password);
                        GestioneContatti gc = new GestioneContatti(controller);
                        HomeMenu home = new HomeMenu(gc);
                        home.launch();
                        dispose();
                } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, ex.getMessage(),
                                "Errore", JOptionPane.ERROR_MESSAGE);
                } finally {
                        setBusy(false);
                }
        }

        private void doSignUp() {
                String username = usernameField.getText() == null ? "" : usernameField.getText();
                String password = new String(passwordField.getPassword());
                setBusy(true);
                try {
                        controller.signUp(username, password);
                        controller.signIn(username, password);
                        it.dissanahmed.rubrica.GestioneContatti gc = new it.dissanahmed.rubrica.GestioneContatti(controller);
                        HomeMenu home = new HomeMenu(gc);
                        home.launch();
                        dispose();
                } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, ex.getMessage(), "Errore",
                                JOptionPane.ERROR_MESSAGE);
                } finally {
                        setBusy(false);
                }
        }

        private void updateTogglePwdIcon() {
                Icon icon = loadIcon(passwordVisible ? "/icons/eye_off.png" : "/icons/eye.png");
                togglePwdBtn.setIcon(icon);
                togglePwdBtn.setToolTipText(passwordVisible ? "Nascondi password" : "Mostra password");
        }

        private void togglePasswordVisibility() {
                passwordVisible = !passwordVisible;
                passwordField.setEchoChar(passwordVisible ? (char) 0 : defaultEchoChar);
                updateTogglePwdIcon();
        }

        private Icon loadIcon(String path) {
                try {
                        java.net.URL url = getClass().getResource(path);
                        if (url == null) return null;
                        return new ImageIcon(url);
                } catch (Exception e) {
                        return null;
                }
        }

        private void setBusy(boolean busy) {
                signInBtn.setEnabled(!busy);
                signUpBtn.setEnabled(!busy);
                usernameField.setEnabled(!busy);
                passwordField.setEnabled(!busy);
                togglePwdBtn.setEnabled(!busy);
                setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
        }
}
