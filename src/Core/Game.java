package Core;

import Player.Player;
import UI.GamePanel;

import javax.swing.*;
import java.awt.*;

/**
 * The main game window and entry point for the WATVA game application.
 * Handles window creation, scaling, and serves as the root container for the game panel.
 */
public class Game extends JFrame {
    private GamePanel gamePanel;
    private Player player;

    private static final int BASE_WIDTH = 1920;
    private static final int BASE_HEIGHT = 1080;

    private static double scaleFactor = 1.0;

    private static double realScreenWidth;
    private static double realScreenHeight;

    private static int scaledGameWidth;
    private static int scaledGameHeight;

    /**
     * Constructs and initializes the main game window.
     * Sets up fullscreen display, calculates scaling factors, and creates game components.
     * The window is undecorated (no borders/titlebar) and maximized to full screen.
     */
    public Game() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        realScreenWidth = screenSize.getWidth();
        realScreenHeight = screenSize.getHeight();

        scaleFactor = Math.min(realScreenWidth / BASE_WIDTH, realScreenHeight / BASE_HEIGHT);

        scaledGameWidth = (int)(BASE_WIDTH * scaleFactor);
        scaledGameHeight = (int)(BASE_HEIGHT * scaleFactor);

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

    public static int getScaledGameWidth() {
        return scaledGameWidth;
    }

    public static int getScaledGameHeight() {
        return scaledGameHeight;
    }

    public static int scale(int value) {
        return (int)(value * scaleFactor);
    }

    public static double scale(double value) {
        return value * scaleFactor;
    }
}