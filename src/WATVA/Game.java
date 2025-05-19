package WATVA;

import javax.swing.*;
import java.awt.*;

public class Game extends JFrame {
    private GamePanel gamePanel;
    private Player player;
    private static double scaleFactor = 1.0;
    private static double realScreenWidth;
    private static double realScreenHeight;

    public Game() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = (int) screenSize.getWidth();
        int screenHeight = (int) screenSize.getHeight();
        realScreenWidth = (int) screenSize.getWidth();
        realScreenHeight = (int) screenSize.getHeight();

        scaleFactor = Math.min((double)screenWidth / GamePanel.PANEL_WIDTH,
                (double)screenHeight / GamePanel.PANEL_HEIGHT);

        player = new Player(0, 0, 100);
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

    public static double getRealScreenWidth() {
        return realScreenWidth;
    }

    public static double getRealScreenHeight() {
        return realScreenHeight;
    }
}