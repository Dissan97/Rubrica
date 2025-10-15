package it.dissanahmed.gui;

import it.dissanahmed.rubrica.GestioneContatti;
import it.dissanahmed.rubrica.Persona;
import it.dissanahmed.rubrica.ex.PersonaException;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class EditorPersona {
        private GestioneContatti gestioneContatti;
        private static final JLabel[] labels = new JLabel[Persona.VALID_FIELDS.size()];
        private static final String[] FIELD_NAMES = Persona.VALID_FIELDS.keySet().toArray(new String[0]);
        private JFrame editorPersonaGUI;
        private JTextField[] textFields = new JTextField[labels.length];
        private boolean editMode = false;
        private String telToEdit = null;
        private String[] originalValues = null;
        private HomeMenu homeMenu;

        static {
                int i = 0;
                for (String field : Persona.VALID_FIELDS.keySet()) {
                        labels[i++] = new JLabel(StringUtils.capitalize(field));
                }
        }

        public EditorPersona(HomeMenu homeMenu) {
                this(homeMenu, "Nuovo Contatto", null, false, null);
        }

        public EditorPersona(HomeMenu homeMenu, String[] values, String telToEdit) {
                this(homeMenu, "Modifica Contatto", values, true, telToEdit);
        }

        private EditorPersona(HomeMenu homeMenu, String title, String[] values, boolean editMode, String tel) {
                this.gestioneContatti = homeMenu.getGestioneContatti();
                this.homeMenu = homeMenu;
                this.editMode = editMode;
                this.telToEdit = tel;
                this.originalValues = values != null ? values.clone() : null;

                this.editorPersonaGUI = new JFrame(title);
                this.editorPersonaGUI.setLayout(new BorderLayout(15, 15));

                SwingUtilities.invokeLater(() -> {
                        Font labelFont = new Font("SansSerif", Font.BOLD, 16);
                        Font textFont = new Font("SansSerif", Font.PLAIN, 16);

                        // ✅ TOOLBAR con Save e Undo
                        JToolBar toolBar = new JToolBar();
                        toolBar.setFloatable(false);
                        toolBar.setRollover(true);
                        toolBar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                        toolBar.setBackground(Color.WHITE);

                        JButton salvaBtn = new JButton("Salva");
                        JButton annullaBtn = new JButton("Annulla");

                        salvaBtn.setIcon(new ImageIcon(Objects.requireNonNull(getClass()
                                .getResource("/icons/save.png"))));
                        annullaBtn.setIcon(new ImageIcon(Objects.requireNonNull(getClass()
                                .getResource("/icons/undo.png"))));

                        salvaBtn.setToolTipText("Salva");
                        annullaBtn.setToolTipText("Annulla");

                        for (JButton b : new JButton[]{salvaBtn, annullaBtn}) {
                                b.setFocusable(false);
                                b.setBackground(Color.WHITE);

                        }

                        toolBar.add(Box.createHorizontalGlue());
                        toolBar.add(salvaBtn);
                        toolBar.add(annullaBtn);
                        this.editorPersonaGUI.add(toolBar, BorderLayout.NORTH);

                        // ✅ Form fields
                        JPanel jPanel = new JPanel(new GridBagLayout());
                        GridBagConstraints gbc = new GridBagConstraints();
                        gbc.insets = new Insets(10, 10, 10, 10);
                        gbc.fill = GridBagConstraints.VERTICAL;
                        gbc.weightx = 1.0;

                        for (int i = 0; i < textFields.length; i++) {
                                labels[i].setFont(labelFont);
                                textFields[i] = new JTextField(25);
                                textFields[i].setFont(textFont);

                                if (values != null && i < values.length && values[i] != null) {
                                        textFields[i].setText(values[i]);
                                }

                                gbc.gridx = 0;
                                gbc.gridy = i;
                                gbc.anchor = GridBagConstraints.LINE_END;
                                jPanel.add(labels[i], gbc);

                                gbc.gridx = 1;
                                gbc.anchor = GridBagConstraints.LINE_START;
                                jPanel.add(textFields[i], gbc);
                        }

                        this.editorPersonaGUI.add(jPanel, BorderLayout.CENTER);

                        // ✅ Actions
                        salvaBtn.addActionListener(e -> onSave());
                        annullaBtn.addActionListener(e -> this.editorPersonaGUI.dispose());

                        this.editorPersonaGUI.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        this.editorPersonaGUI.setSize(800, 600);
                        this.editorPersonaGUI.setLocationRelativeTo(null);
                        this.editorPersonaGUI.setVisible(true);
                });
        }

        private void onSave() {
                if (!editMode) {
                        StringBuilder builder = new StringBuilder();
                        for (JTextField textField : textFields) {
                                builder.append(textField.getText()).append(";");
                        }
                        try {
                                this.gestioneContatti.inserisciDatiPersona(
                                        builder.substring(0, builder.length() - 1)
                                );
                                this.homeMenu.updateListaPersone();
                                this.editorPersonaGUI.dispose();
                        } catch (PersonaException ex) {
                                JOptionPane.showMessageDialog(
                                        editorPersonaGUI,
                                        "Errore nel salvataggio: " + ex.getMessage(),
                                        "Errore",
                                        JOptionPane.ERROR_MESSAGE
                                );
                        }
                        return;
                }

                if (telToEdit == null) {
                        JOptionPane.showMessageDialog(
                                editorPersonaGUI,
                                "Telefono della persona da modificare non specificato.",
                                "Errore",
                                JOptionPane.ERROR_MESSAGE
                        );
                        return;
                }

                boolean anyChange = false;
                StringBuilder errors = new StringBuilder();

                for (int i = 0; i < textFields.length; i++) {
                        String fieldName = FIELD_NAMES[i];
                        String newValue = textFields[i].getText();
                        String oldValue = (originalValues != null && i < originalValues.length) ? originalValues[i] : null;

                        boolean changed;
                        if (oldValue == null && newValue == null) {
                                changed = false;
                        } else if (oldValue == null) {
                                changed = !newValue.isEmpty();
                        } else {
                                changed = !oldValue.equals(newValue);
                        }

                        if (changed) {
                                anyChange = true;
                                try {
                                        this.gestioneContatti.modificaPersona(telToEdit, fieldName, newValue);
                                } catch (PersonaException ex) {
                                        errors.append("Campo '")
                                                .append(fieldName)
                                                .append("': ")
                                                .append(ex.getMessage())
                                                .append("\n");
                                }
                        }
                }

                if (errors.length() > 0) {
                        JOptionPane.showMessageDialog(
                                editorPersonaGUI,
                                "Alcune modifiche non sono state applicate:\n\n" + errors,
                                "Attenzione",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                if (anyChange) {
                        this.homeMenu.updateListaPersone();
                }

                this.editorPersonaGUI.dispose();
        }

        public void launch() {
                this.editorPersonaGUI.setVisible(true);
        }
}
