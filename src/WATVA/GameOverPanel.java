package WATVA;

import MainMenu.MainMenuPanel;

import javax.swing.*;
import java.awt.*;


public class GameOverPanel extends JPanel {

    private Game game;
    private GamePanel gamePanel;

    public GameOverPanel(Game game, GamePanel gamePanel) {
        this.game = game;
        this.gamePanel = gamePanel;
        initializeComponents();
    }

    private void initializeComponents() {
        setLayout(new GridLayout(5, 1, 10, 10));
        setBackground(new Color(0, 0, 0, 150));
        setBounds((GamePanel.PANEL_WIDTH - 300) / 2, (GamePanel.PANEL_HEIGHT - 400) / 2, 300, 400);

        JLabel gameOverLabel = new JLabel("YOU DIED", JLabel.CENTER);
        gameOverLabel.setFont(new Font("Arial", Font.BOLD, 36));
        gameOverLabel.setForeground(Color.WHITE);

        JButton restartButton = createButton("Play again");
        restartButton.addActionListener(e ->{
            gamePanel.restartGame();
        });

        JButton upgradeButton = createButton("Upgrade");
        upgradeButton.addActionListener(e -> {
            this.setVisible(false);
            gamePanel.initializeUpgradePanel();
        });


        JButton levelsButton = createButton("Levels");
        //levelsButton.addActionListener(e -> game.showLevelSelectPanel());

        JButton mainMenuButton = createButton("Main Menu");
        mainMenuButton.addActionListener(e -> {
            new MainMenuPanel();
            gamePanel.closeGame();
        });

        add(gameOverLabel);
        add(restartButton);
        add(upgradeButton);
        add(levelsButton);
        add(mainMenuButton);
    }

    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 20));
        button.setForeground(Color.WHITE);
        button.setBackground(Color.DARK_GRAY);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        return button;
    }
}
