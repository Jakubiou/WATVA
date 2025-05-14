package WATVA;

import javax.swing.*;
import java.awt.*;

public class Game extends JFrame {
    private GamePanel gamePanel;
    private Player player;
    private static double scaleFactor = 1.0;

    public Game() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = (int) screenSize.getWidth();
        int screenHeight = (int) screenSize.getHeight();

        scaleFactor = Math.min((double)screenWidth / 1530, (double)screenHeight / 900);

        player = new Player((int)(Player.PANEL_WIDTH * scaleFactor), (int)(Player.PANEL_HEIGHT * scaleFactor), 100);
        gamePanel = new GamePanel(this, player);

        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        add(gamePanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    public static double getScaleFactor() {
        return scaleFactor;
    }
}