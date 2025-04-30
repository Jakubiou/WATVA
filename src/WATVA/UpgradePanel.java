package WATVA;
import java.awt.*;
import javax.swing.*;

public class UpgradePanel extends JPanel {
    private final Player player;
    private final GamePanel gamePanel;
    private boolean visible;

    public UpgradePanel(GamePanel gamePanel,Player player) {
        this.gamePanel = gamePanel;
        this.player = player;
        initializeUpgradePanel();
    }

    private void initializeUpgradePanel() {
        setLayout(new GridLayout(3, 1));

        int panelWidth = 400;
        int panelHeight = 300;
        setBounds((GamePanel.PANEL_WIDTH - panelWidth) / 2, (GamePanel.PANEL_HEIGHT - panelHeight) / 2, panelWidth, panelHeight);

        setBackground(Color.DARK_GRAY);
        setBorder(BorderFactory.createLineBorder(Color.BLACK, 5));

        JLabel coinsLabel = new JLabel("Coins: " + player.getCoins(), JLabel.CENTER);
        coinsLabel.setForeground(Color.WHITE);
        coinsLabel.setFont(new Font("Arial", Font.BOLD, 24));
        add(coinsLabel);

        JButton damageButton = createUpgradeButton("Damage");
        JButton hpButton = createUpgradeButton("HP");
        JButton defenseButton = createUpgradeButton("Defense");
        JButton playAgainButton = createUpgradeButton("Play again");

        damageButton.addActionListener(e -> upgradeStat("damage", 10));
        hpButton.addActionListener(e -> upgradeStat("HP", 10));
        defenseButton.addActionListener(e -> upgradeStat("defense", 10));
        playAgainButton.addActionListener(e -> upgradeStat("play again", 0));

        add(damageButton);
        add(hpButton);
        add(defenseButton);
        add(playAgainButton);

        setVisible(false);
    }

    private JButton createUpgradeButton(String statName) {
        JButton button = new JButton() {
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 20));
                String text = String.format("%s +\nCurrent: %d", statName, getCurrentStatValue(statName));
                drawCenteredString(g, text, getWidth(), getHeight());
            }
        };
        styleAbilityButton(button);
        return button;
    }

    private void drawCenteredString(Graphics g, String text, int width, int height) {
        FontMetrics metrics = g.getFontMetrics();
        String[] lines = text.split("\\n");
        int y = (height - metrics.getHeight() * lines.length) / 2 + metrics.getAscent();
        for (String line : lines) {
            int x = (width - metrics.stringWidth(line)) / 2;
            g.drawString(line, x, y);
            y += metrics.getHeight();
        }
    }

    private int getCurrentStatValue(String statName) {
        switch (statName) {
            case "Damage":
                return player.getDamage();
            case "HP":
                return player.getHp();
            case "Defense":
                return player.getDefense();
            default:
                return 0;
        }
    }

    private void styleAbilityButton(JButton button) {
        button.setFont(new Font("Arial", Font.BOLD, 20));
        button.setBackground(Color.GRAY);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        button.setFocusPainted(false);
    }

    private void upgradeStat(String stat, int cost) {
        if (player.getCoins() >= cost) {
            player.setCoins(player.getCoins() - cost);
            switch (stat.toLowerCase()) {
                case "damage":
                    player.increaseDamage();
                    break;
                case "hp":
                    player.increaseHp();
                    break;
                case "defense":
                    player.increaseDefense();
                    break;
                case "play again":
                    gamePanel.restartGame();
                    break;
                default:
                    System.err.println("Unknown stat: " + stat);
            }
            player.saveState("player_save.dat");
            updateAbilityPanel();
        } else {
            System.out.println("Not enough coins!");
        }
    }

    public void updateAbilityPanel() {
        ((JLabel) getComponent(0)).setText("Coins: " + player.getCoins());
        repaint();
    }
    public void showPanel() {
        setVisible(true);
        updateAbilityPanel();
        visible = true;
    }

    public void hidePanel() {
        setVisible(false);
        visible = false;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }
}