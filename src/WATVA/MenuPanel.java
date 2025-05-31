package WATVA;

import MainMenu.MainMenuPanel;
import javax.swing.*;
import java.awt.*;
import java.io.FileInputStream;

public class MenuPanel extends JPanel {
    private JButton resumeButton;
    private JButton restartButton;
    private JButton mainMenuButton;
    private JButton quitButton;
    private JButton settingsButton;
    private Soundtrack backgroundMusic;
    private Font pixelPurlFont;

    public MenuPanel(Game game, GamePanel gamePanel) {
        try {
            FileInputStream fontStream = new FileInputStream("res/fonts/PixelPurl.ttf");
            pixelPurlFont = Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont(24f);
        } catch (Exception e) {
            pixelPurlFont = new Font("Arial", Font.BOLD, 24);
            e.printStackTrace();
        }

        setOpaque(false);
        setLayout(null);
        int menuPanelWidth = 400;
        int menuPanelHeight = 600;
        int menuPanelX = (GamePanel.PANEL_WIDTH - menuPanelWidth) / 2;
        int menuPanelY = (GamePanel.PANEL_HEIGHT - menuPanelHeight) / 2;
        setBounds(menuPanelX, menuPanelY, menuPanelWidth, menuPanelHeight);
        setOpaque(false);

        resumeButton = createMenuButton("RESUME", 200, 50);
        resumeButton.setBounds((menuPanelWidth - 200) / 2, 50, 200, 50);
        resumeButton.addActionListener(e -> gamePanel.toggleMenu());
        add(resumeButton);

        restartButton = createMenuButton("RESTART", 300, 50);
        restartButton.setBounds((menuPanelWidth - 300) / 2, 150, 300, 50);
        restartButton.addActionListener(e -> gamePanel.restartGame());
        add(restartButton);

        mainMenuButton = createMenuButton("MAIN MENU", 300, 50);
        mainMenuButton.setBounds((menuPanelWidth - 300) / 2, 250, 300, 50);
        mainMenuButton.addActionListener(e -> {
            gamePanel.closeGame();
            new MainMenuPanel();
            gamePanel.savePlayerCoins();
        });
        add(mainMenuButton);

        settingsButton = createMenuButton("SETTINGS", 300, 50);
        settingsButton.setBounds((menuPanelWidth - 300) / 2, 350, 300, 50);
        settingsButton.addActionListener(e -> {
            Settings settings = new Settings(GamePanel.backgroundMusic);
            settings.setVisible(true);
        });
        add(settingsButton);

        quitButton = createMenuButton("QUIT GAME", 300, 50);
        quitButton.setBounds((menuPanelWidth - 300) / 2, 450, 300, 50);
        quitButton.addActionListener(e -> System.exit(0));
        add(quitButton);

        setVisible(false);
    }

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