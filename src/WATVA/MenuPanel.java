package WATVA;

import MainMenu.MainMenuPanel;
import javax.swing.*;
import java.awt.*;

public class MenuPanel extends JPanel {
    private JButton resumeButton;
    private JButton restartButton;
    private JButton mainMenuButton;
    private JButton quitButton;
    private JButton settingsButton;
    private Soundtrack backgroundMusic;


    public MenuPanel(Game game, GamePanel gamePanel) {
        setLayout(null);
        int menuPanelWidth = 400;
        int menuPanelHeight = 600;
        int menuPanelX = (GamePanel.PANEL_WIDTH - menuPanelWidth) / 2;
        int menuPanelY = (GamePanel.PANEL_HEIGHT - menuPanelHeight) / 2;
        setBounds(menuPanelX, menuPanelY, menuPanelWidth, menuPanelHeight);
        setBackground(new Color(0, 0, 0, 200));

        resumeButton = new JButton("Resume");
        resumeButton.setBounds((menuPanelWidth - 200) / 2, 50, 200, 50);
        resumeButton.addActionListener(e -> gamePanel.toggleMenu());
        add(resumeButton);

        restartButton = new JButton("Restart Game");
        restartButton.setBounds((menuPanelWidth - 300) / 2, 150, 300, 50);
        restartButton.addActionListener(e -> gamePanel.restartGame());
        add(restartButton);

        mainMenuButton = new JButton("Main Menu");
        mainMenuButton.setBounds((menuPanelWidth - 300) / 2, 250, 300, 50);
        mainMenuButton.addActionListener(e -> {
            gamePanel.closeGame();
            new MainMenuPanel();
            gamePanel.savePlayerCoins();
        });
        add(mainMenuButton);

        quitButton = new JButton("Quit Game");
        quitButton.setBounds((menuPanelWidth - 300) / 2, 450, 300, 50);
        quitButton.addActionListener(e -> System.exit(0));
        add(quitButton);

        settingsButton = new JButton("Settings");
        settingsButton.setBounds((menuPanelWidth - 300) / 2, 350, 300, 50);
        settingsButton.addActionListener(e -> {
            Settings settings = new Settings(GamePanel.backgroundMusic);
            settings.setVisible(true);
        });
        add(settingsButton);

        setVisible(false);
    }
}
