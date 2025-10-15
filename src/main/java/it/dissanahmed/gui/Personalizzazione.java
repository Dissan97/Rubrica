package it.dissanahmed.gui;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;

public class Personalizzazione {

        private Personalizzazione() {}

        // ======== COLOR CONSTANTS ========
        private static final Color ORANGE_PRIMARY = new Color(253, 104, 2);
        private static final Color ORANGE_SEMITRANSPARENT = new Color(253, 104, 2, 204);
        private static final Color ORANGE_PRESSED = new Color(255, 120, 20);
        private static final Color ORANGE_HOVER_LIGHT = new Color(253, 104, 2, 60);
        private static final Color ORANGE_HOVER_STRONG = new Color(253, 104, 2, 100);
        private static final Color TEXT_DARK = new Color(69, 68, 69);
        private static final Color WHITE = Color.WHITE;
        private static final Color BORDER_LIGHT = new Color(210, 210, 210);

        // ======== FONT CONSTANTS ========
        private static final Font FONT_DEFAULT = new Font("Segoe UI", Font.PLAIN, 14);
        private static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 14);
        private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 16);

        // ======== DIMENSIONS ========
        private static final int ARC_RADIUS = 20;
        private static final int ARC_BUTTON = 999;

        public static void init() {
                FlatLightLaf.setup();

                // --- Base Backgrounds ---
                UIManager.put("Panel.background", WHITE);
                UIManager.put("Frame.background", WHITE);
                UIManager.put("RootPane.background", WHITE);
                UIManager.put("OptionPane.background", WHITE);

                // --- Button ---
                UIManager.put("Button.arc", ARC_BUTTON);
                UIManager.put("Button.background", ORANGE_SEMITRANSPARENT);
                UIManager.put("Button.foreground", TEXT_DARK);
                UIManager.put("Button.hoverBackground", ORANGE_PRIMARY);
                UIManager.put("Button.focusedBackground", ORANGE_PRIMARY);
                UIManager.put("Button.pressedBackground", ORANGE_PRESSED);
                UIManager.put("Button.borderWidth", 0);
                UIManager.put("Button.font", FONT_BOLD);

                // --- OptionPane ---
                UIManager.put("OptionPane.yesButtonText", "Sì");
                UIManager.put("OptionPane.noButtonText", "No");
                UIManager.put("OptionPane.cancelButtonText", "Annulla");
                UIManager.put("OptionPane.okButtonText", "OK");
                UIManager.put("OptionPane.messageForeground", TEXT_DARK);
                UIManager.put("OptionPane.messageFont", FONT_DEFAULT);
                UIManager.put("OptionPane.foreground", TEXT_DARK);
                UIManager.put("OptionPane.questionDialog.border.background", WHITE);
                UIManager.put("OptionPane.informationDialog.border.background", WHITE);

                // --- Label ---
                UIManager.put("Label.foreground", TEXT_DARK);

                // --- TextComponent ---
                UIManager.put("TextComponent.arc", ARC_RADIUS);
                UIManager.put("TextComponent.background", WHITE);
                UIManager.put("TextComponent.foreground", TEXT_DARK);
                UIManager.put("TextComponent.borderColor", BORDER_LIGHT);
                UIManager.put("TextComponent.selectionBackground", new Color(253, 104, 2, 150));
                UIManager.put("TextComponent.selectionForeground", WHITE);

                // --- Focus & Hover ---
                UIManager.put("Component.focusColor", ORANGE_PRIMARY);
                UIManager.put("Component.focusWidth", 2);
                UIManager.put("Component.hoverColor", ORANGE_HOVER_LIGHT);

                // --- Table & List ---
                UIManager.put("Table.selectionBackground", new Color(253, 104, 2, 180));
                UIManager.put("Table.selectionForeground", WHITE);
                UIManager.put("List.selectionBackground", new Color(253, 104, 2, 180));
                UIManager.put("List.selectionForeground", WHITE);

                // --- TitlePane ---
                UIManager.put("TitlePane.background", WHITE);
                UIManager.put("TitlePane.foreground", TEXT_DARK);
                UIManager.put("TitlePane.centerTitle", true);
                UIManager.put("TitlePane.unifiedBackground", true);
                UIManager.put("TitlePane.font", FONT_TITLE);
                UIManager.put("TitlePane.buttonHoverBackground", ORANGE_HOVER_LIGHT);
                UIManager.put("TitlePane.closeHoverBackground", ORANGE_HOVER_STRONG);
                UIManager.put("TitlePane.closePressedBackground", ORANGE_PRIMARY);
        }
}
