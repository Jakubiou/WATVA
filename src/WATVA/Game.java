package WATVA;

import javax.swing.*;
import java.awt.*;

/**
 * The main game window and entry point for the WATVA game application.
 * Handles window creation, scaling, and serves as the root container for the game panel.
 */
public class Game extends JFrame {
    private GamePanel gamePanel;
    private Player player;
    private static double scaleFactor = 1.0;
    private static double realScreenWidth;
    private static double realScreenHeight;

    /**
     * Constructs and initializes the main game window.
     * Sets up fullscreen display, calculates scaling factors, and creates game components.
     * The window is undecorated (no borders/titlebar) and maximized to full screen.
     */
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