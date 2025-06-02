package WATVA;

import MainMenu.MainMenuPanel;

import javax.swing.*;
import java.awt.*;
import java.io.FileInputStream;

/**
 * A JPanel that displays when the player dies, providing game over options.
 * Includes buttons to restart the game, view upgrades, or return to main menu.
 */
public class GameOverPanel extends JPanel {
    private Game game;
    private GamePanel gamePanel;
    private Font pixelPurlFont;

    /**
     * Constructs a GameOverPanel with references to game components.
     *
     * @param game The main Game instance
     * @param gamePanel The GamePanel instance for game control
     */
    public GameOverPanel(Game game, GamePanel gamePanel) {
        this.game = game;
        this.gamePanel = gamePanel;
        loadCustomFont();
        initializeComponents();
    }

    /**
     * Loads the custom pixel font from resources.
     * Falls back to Arial if custom font cannot be loaded.
     */
    private void loadCustomFont() {
        try {
            pixelPurlFont = Font.createFont(Font.TRUETYPE_FONT,
                    getClass().getResourceAsStream("/fonts/PixelPurl.ttf")).deriveFont(24f);
        } catch (Exception e) {
            pixelPurlFont = new Font("Arial", Font.BOLD, 24);
            e.printStackTrace();
        }
    }

    /**
     * Initializes and lays out all UI components.
     * Creates game over message and action buttons with event handlers.
     */
    private void initializeComponents() {
        setLayout(new GridLayout(4, 1, 10, 10));
        setBackground(new Color(0, 0, 0, 150));
        setBounds((GamePanel.PANEL_WIDTH - 300) / 2, (GamePanel.PANEL_HEIGHT - 400) / 2, 300, 400);

        JLabel gameOverLabel = new JLabel("YOU DIED", JLabel.CENTER);
        gameOverLabel.setFont(pixelPurlFont.deriveFont(36f));
        gameOverLabel.setForeground(Color.WHITE);

        JButton restartButton = createButton("Play again");
        restartButton.addActionListener(e -> {
            gamePanel.restartGame();
        });

        JButton upgradeButton = createButton("Upgrades");
        upgradeButton.addActionListener(e -> {
            this.setVisible(false);
            gamePanel.initializeUpgradePanel();
        });

        JButton mainMenuButton = createButton("Main Menu");
        mainMenuButton.addActionListener(e -> {
            new MainMenuPanel();
            gamePanel.closeGame();
        });

        add(gameOverLabel);
        add(restartButton);
        add(upgradeButton);
        add(mainMenuButton);
    }

    /**
     * Creates a styled button with consistent appearance.
     *
     * @param text The button text to display
     * @return The created JButton with custom styling
     */
    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setFont(pixelPurlFont);
        button.setForeground(Color.WHITE);
        button.setBackground(Color.DARK_GRAY);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        return button;
    }
}