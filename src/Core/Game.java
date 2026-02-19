package Core;

import Player.Player;
import UI.GamePanel;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.InputStream;

/**
 * The main game window and entry point for the WATVA game application.
 * Handles window creation, scaling, and serves as the root container for the game panel.
 */
public class Game extends JFrame {
    private GamePanel gamePanel;
    private Player player;
    private boolean isTutorialMode;

    private static final int BASE_WIDTH = 1920;
    private static final int BASE_HEIGHT = 1080;

    private static double scaleFactor = 1.0;

    private static double realScreenWidth;
    private static double realScreenHeight;

    private static int scaledGameWidth;
    private static int scaledGameHeight;

    /**
     * Constructs and initializes the main game window in normal mode.
     */
    public Game() {
        this(false);
    }

    /**
     * Constructs and initializes the main game window.
     * Sets up fullscreen display, calculates scaling factors, and creates game components.
     * The window is undecorated (no borders/titlebar) and maximized to full screen.
     *
     * @param tutorialMode If true, starts in tutorial mode
     */
    public Game(boolean tutorialMode) {
        this.isTutorialMode = tutorialMode;

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        realScreenWidth = screenSize.getWidth();
        realScreenHeight = screenSize.getHeight();

        scaleFactor = Math.min(realScreenWidth / BASE_WIDTH, realScreenHeight / BASE_HEIGHT);

        scaledGameWidth = (int)(BASE_WIDTH * scaleFactor);
        scaledGameHeight = (int)(BASE_HEIGHT * scaleFactor);

        scaleFactor *= 1.6;

        player = new Player(0, 0, 100);
        gamePanel = new GamePanel(this, player, tutorialMode);

        setCustomCursor();

        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        add(gamePanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    /**
     * Loads and sets the custom cursor for the game.
     */
    private void setCustomCursor() {
        try {
            InputStream cursorStream = getClass().getResourceAsStream("/WATVA/Other/Cursor.png");
            if (cursorStream != null) {
                Image cursorImage = ImageIO.read(cursorStream);

                int targetSize = 32;

                Image scaledCursor = cursorImage.getScaledInstance(targetSize, targetSize, Image.SCALE_SMOOTH);

                ImageIcon tempIcon = new ImageIcon(scaledCursor);
                int actualWidth = tempIcon.getIconWidth();
                int actualHeight = tempIcon.getIconHeight();

                if (actualWidth <= 0 || actualHeight <= 0) {
                    System.err.println("Invalid cursor dimensions: " + actualWidth + "x" + actualHeight);
                    setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
                    return;
                }

                int hotspotX = actualWidth / 2;
                int hotspotY = actualHeight / 2;

                if (hotspotX >= actualWidth) hotspotX = actualWidth - 1;
                if (hotspotY >= actualHeight) hotspotY = actualHeight - 1;

                if (hotspotX < 0) hotspotX = 0;
                if (hotspotY < 0) hotspotY = 0;

                Point hotspot = new Point(hotspotX, hotspotY);

                Cursor customCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                        scaledCursor, hotspot, "CustomCursor"
                );

                setCursor(customCursor);
            } else {
                System.err.println("Cursor image not found at /WATVA/Other/Cursor.png");
                setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
            }
        } catch (Exception e) {
            System.err.println("Error loading custom cursor: " + e.getMessage());
            e.printStackTrace();
            setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        }
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

    public boolean isTutorialMode() {
        return isTutorialMode;
    }
}