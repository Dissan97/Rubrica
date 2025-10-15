package it.dissanahmed.gui;

import it.dissanahmed.login.GestioneLogin;
import it.dissanahmed.rubrica.GestioneContatti;
import it.dissanahmed.rubrica.Persona;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Objects;

public class HomeMenu {
        private final GestioneContatti gestioneContatti;
        public static final String ERR_MSG_ELIMINA =
                "Per eliminare una persona dalla lista bisogna prima selezionarla";
        private static final String ERR_MSG_MOD = "Per modificare una persona nella lista bisogna prima selezionarla";
        public static final String ERRORE_ELIMINAZIONE = "Errore Eliminazione";
        private static final String ERRORE_MODIFICA = "Errore Modifica";
        public static final String ELIMINARE_LA_PERSONA = "Eliminare la persona: ";
        public static final String SCELTA = "Confermare la scelta";
        public static final String ELIMINATO = "Eliminato: ";
        public static final String RISULTATO_ELIMINAZIONE = "Risultato eliminazione";

        private static final int TEL_INDEX = 3;

        private JTable listaPersone;
        private JScrollPane jScrollPane;
        private JButton nuovaPersona;
        private JButton modificaPersona;
        private JButton eliminaPersona;
        private JButton esciBtn;
        private final JFrame homeFrame;

        public HomeMenu() {
                this(new GestioneContatti());
        }

        public HomeMenu(GestioneContatti gestioneContatti) {
                homeFrame = new JFrame();
                homeFrame.setTitle("Gestion Rubrica Home");
                homeFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                creaInterfaccia();
                this.gestioneContatti = gestioneContatti;
        }

        private void creaInterfaccia() {
                SwingUtilities.invokeLater(() -> {
                        DefaultTableModel tableModel = new DefaultTableModel(
                                Persona.VALID_FIELDS.keySet().stream().map(StringUtils::capitalize).toArray(),
                                0
                        );

                        this.jScrollPane = new JScrollPane();
                        this.listaPersone = new JTable(tableModel);
                        listaPersone.setFillsViewportHeight(true);
                        listaPersone.setPreferredScrollableViewportSize(new Dimension(400, 100));
                        listaPersone.getTableHeader().setReorderingAllowed(false);
                        this.jScrollPane.setViewportView(listaPersone);

                        JLabel titolo = new JLabel("Rubrica", SwingConstants.CENTER);
                        titolo.setFont(new Font("Segoe UI", Font.BOLD, 28));
                        titolo.setForeground(new Color(69, 68, 69));
                        titolo.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));

                        JPanel centro = new JPanel(new BorderLayout());
                        centro.setBackground(Color.WHITE);
                        centro.add(titolo, BorderLayout.NORTH);
                        centro.add(jScrollPane, BorderLayout.CENTER);

                        this.homeFrame.add(centro, BorderLayout.CENTER);

                        setupButton();
                        this.updateListaPersone();
                });
        }

        private void setupButton() {
                JToolBar toolBar = new JToolBar();
                toolBar.setFloatable(false);
                toolBar.setRollover(true);
                toolBar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                toolBar.setBackground(Color.WHITE);

                this.nuovaPersona = new JButton("Nuovo");
                this.modificaPersona = new JButton("Modifica");
                this.eliminaPersona = new JButton("Elimina");
                this.esciBtn = new JButton("Esci");

                for (JButton b : new JButton[]{nuovaPersona, modificaPersona, eliminaPersona, esciBtn}) {
                        b.setFocusable(false);
                        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
                        b.setHorizontalTextPosition(SwingConstants.CENTER);
                        b.setVerticalTextPosition(SwingConstants.BOTTOM);
                        b.setPreferredSize(new Dimension(90, 70));
                }

                nuovaPersona.setIcon(new ImageIcon(Objects.requireNonNull(getClass()
                        .getResource("/icons/add_user.png"))));
                modificaPersona.setIcon(new ImageIcon(Objects.requireNonNull(getClass()
                        .getResource("/icons/edit_user.png"))));
                eliminaPersona.setIcon(new ImageIcon(Objects.requireNonNull(getClass()
                        .getResource("/icons/delete_user.png"))));
                esciBtn.setIcon(new ImageIcon(Objects.requireNonNull(getClass()
                        .getResource("/icons/logout.png"))));

                toolBar.add(nuovaPersona);
                toolBar.add(modificaPersona);
                toolBar.add(eliminaPersona);
                toolBar.add(Box.createHorizontalGlue());
                if (this.gestioneContatti.getUsername() != null){
                        toolBar.add(esciBtn);
                }


                this.homeFrame.add(toolBar, BorderLayout.NORTH);

                nuovaPersona.addActionListener(e -> new EditorPersona(this).launch());

                modificaPersona.addActionListener(e -> {
                        int selectedRow = listaPersone.getSelectedRow();
                        if (selectedRow >= 0 && selectedRow < listaPersone.getRowCount()) {
                                String[] values = new String[Persona.VALID_FIELDS.size()];
                                for (int col = 0; col < values.length; col++) {
                                        values[col] = "" + listaPersone.getValueAt(selectedRow, col);
                                }
                                new EditorPersona(this, values,
                                        "" + listaPersone.getValueAt(selectedRow, TEL_INDEX)).launch();
                        } else {
                                JOptionPane.showMessageDialog(
                                        this.homeFrame,
                                        ERR_MSG_MOD,
                                        ERRORE_MODIFICA,
                                        JOptionPane.ERROR_MESSAGE
                                );
                        }
                });

                eliminaPersona.addActionListener(e -> {
                        int selectedRow = listaPersone.getSelectedRow();
                        if (selectedRow >= 0 && selectedRow < listaPersone.getRowCount()) {
                                String nomeECognome = listaPersone.getValueAt(selectedRow, 0)
                                        + " " + listaPersone.getValueAt(selectedRow, 1);
                                int scelta = JOptionPane.showConfirmDialog(
                                        this.homeFrame,
                                        ELIMINARE_LA_PERSONA + nomeECognome,
                                        SCELTA,
                                        JOptionPane.YES_NO_OPTION,
                                        JOptionPane.QUESTION_MESSAGE
                                );

                                if (scelta == JOptionPane.YES_OPTION &&
                                        this.gestioneContatti.rimuoviPersona("" + listaPersone.getValueAt(selectedRow, TEL_INDEX))) {
                                        JOptionPane.showMessageDialog(
                                                this.homeFrame,
                                                ELIMINATO + nomeECognome,
                                                RISULTATO_ELIMINAZIONE,
                                                JOptionPane.INFORMATION_MESSAGE
                                        );
                                        this.updateListaPersone();
                                }
                        } else {
                                JOptionPane.showMessageDialog(
                                        this.homeFrame,
                                        ERR_MSG_ELIMINA,
                                        ERRORE_ELIMINAZIONE,
                                        JOptionPane.ERROR_MESSAGE
                                );
                        }
                });

                esciBtn.addActionListener(e -> {
                        this.homeFrame.dispose();
                        new LoginGUI(new GestioneLogin()).setVisible(true);
                });
        }

        public void updateListaPersone() {
                DefaultTableModel model = (DefaultTableModel) listaPersone.getModel();
                model.setRowCount(0);
                for (Persona persona : this.getGestioneContatti().getContatti()) {
                        model.addRow(persona.getRawPersona());
                }
        }

        public void launch() {
                homeFrame.setSize(1920, 1080);
                homeFrame.setLocationRelativeTo(null);
                homeFrame.setVisible(true);
        }

        protected GestioneContatti getGestioneContatti() {
                return this.gestioneContatti;
        }
}
