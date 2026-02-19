import MainMenu.MainMenuPanel;
import javax.swing.*;


public class Main {
    private static MainMenuPanel currentMenu = null;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            if (currentMenu != null) {
                currentMenu.dispose();
            }
            currentMenu = new MainMenuPanel();
        });
    }
}