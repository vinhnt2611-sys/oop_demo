package logic;

import logic.ui.NormalizerFrame;
import java.awt.Font;
import javax.swing.*;

public final class Main {
    private Main() {}
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
            catch (Exception ignored) { /* The standard Swing default remains available. */ }
            UIManager.put("Button.font", new Font("Segoe UI", Font.PLAIN, 12));
            UIManager.put("ComboBox.font", new Font("Segoe UI", Font.PLAIN, 13));
            UIManager.put("TabbedPane.contentBorderInsets", new java.awt.Insets(0, 0, 0, 0));
            UIManager.put("TabbedPane.selected", java.awt.Color.WHITE);
            new NormalizerFrame().setVisible(true);
        });
    }
}
