package UI;

import Logic.GameLogic;
import MainMenu.MainMenuPanel;
import Core.Game;

import javax.swing.*;
import java.awt.*;

/**
 * The in-game menu panel that provides game control options during gameplay.
 * This panel appears when the game is paused and offers options to resume, restart,
 * return to main menu, adjust settings, or quit the game entirely.
 */
public class MenuPanel extends JPanel {
    private JButton resumeButton;
    private JButton levelMapButton;  // NOVÉ
    private JButton restartButton;
    private JButton mainMenuButton;
    private JButton quitButton;
    private JButton settingsButton;
    private Font pixelPurlFont;
    private SettingsPanel settingsPanel;
    private GamePanel gamePanel;

    /**
     * Constructs a new MenuPanel with all necessary game control options.
     * Initializes the panel layout, loads custom font, and creates all interactive buttons.
     *
     * @param game The main Game instance that manages the application window
     * @param gamePanel The GamePanel instance that handles game rendering and input
     * @param gameLogic The GameLogic instance that manages game state and rules
     */
    public MenuPanel(Game game, GamePanel gamePanel, GameLogic gameLogic) {
        this.gamePanel = gamePanel;

        try {
            pixelPurlFont = Font.createFont(Font.TRUETYPE_FONT,
                    getClass().getResourceAsStream("/fonts/PixelPurl.ttf")).deriveFont(24f);
        } catch (Exception e) {
            pixelPurlFont = new Font("Arial", Font.BOLD, 24);
            e.printStackTrace();
        }

        setOpaque(false);
        setLayout(null);
        int menuPanelWidth = 400;
        int menuPanelHeight = 700;
        int menuPanelX = (GamePanel.PANEL_WIDTH - menuPanelWidth) / 2;
        int menuPanelY = (GamePanel.PANEL_HEIGHT - menuPanelHeight) / 2;
        setBounds(menuPanelX, menuPanelY, menuPanelWidth, menuPanelHeight);
        setOpaque(false);

        resumeButton = createMenuButton("RESUME", 200, 50);
        resumeButton.setBounds((menuPanelWidth - 200) / 2, 50, 200, 50);
        resumeButton.addActionListener(e -> {
            gamePanel.toggleMenu();
            gameLogic.resumeGame();
        });
        add(resumeButton);

        levelMapButton = createMenuButton("LEVEL MAP", 300, 50);
        levelMapButton.setBounds((menuPanelWidth - 300) / 2, 150, 300, 50);
        levelMapButton.addActionListener(e -> {
            setVisible(false);
            gamePanel.showLevelMap();
        });
        add(levelMapButton);

        restartButton = createMenuButton("RESTART", 300, 50);
        restartButton.setBounds((menuPanelWidth - 300) / 2, 250, 300, 50);
        restartButton.addActionListener(e -> gamePanel.restartGame());
        add(restartButton);

        mainMenuButton = createMenuButton("MAIN MENU", 300, 50);
        mainMenuButton.setBounds((menuPanelWidth - 300) / 2, 350, 300, 50);
        mainMenuButton.addActionListener(e -> {
            gamePanel.closeGame();
            new MainMenuPanel();
            gameLogic.savePlayerCoins();
        });
        add(mainMenuButton);

        settingsButton = createMenuButton("SETTINGS", 300, 50);
        settingsButton.setBounds((menuPanelWidth - 300) / 2, 450, 300, 50);
        settingsButton.addActionListener(e -> {
            setVisible(false);
            if (settingsPanel == null) {
                settingsPanel = new SettingsPanel(GameLogic.backgroundMusic, this);
                gamePanel.add(settingsPanel);
            }
            settingsPanel.setVisible(true);
        });
        add(settingsButton);

        quitButton = createMenuButton("QUIT GAME", 300, 50);
        quitButton.setBounds((menuPanelWidth - 300) / 2, 550, 300, 50);
        quitButton.addActionListener(e -> System.exit(0));
        add(quitButton);

        setVisible(false);
    }

    /**
     * Creates a styled menu button with consistent appearance and hover effects.
     * All buttons in the menu use this standardized styling.
     *
     * @param text The display text for the button
     * @param width The width of the button in pixels
     * @param height The height of the button in pixels
     * @return A configured JButton with the specified styling
     */
    private JButton createMenuButton(String text, int width, int height) {
        JButton button = new JButton(text);
        button.setFont(pixelPurlFont);
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(40, 40, 40));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.ORANGE, 2),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(60, 60, 60));
                button.setForeground(Color.ORANGE);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(40, 40, 40));
                button.setForeground(Color.WHITE);
            }
        });

        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(width, height));

        return button;
    }

    /**
     * Renders the menu panel with a semi-transparent rounded rectangle background.
     * This provides visual separation from the game while maintaining visibility.
     *
     * @param g The Graphics object used for rendering
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int arc = 40;
        g2.setColor(new Color(30, 30, 30, 220));
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
    }
}