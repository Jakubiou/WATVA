package WATVA;

import java.awt.*;
import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Upgrade panel that appears between waves, allowing players to upgrade their stats.
 * Displays available upgrades and current player statistics.
 */
public class UpgradePanel extends JPanel {
    private final Player player;
    private final GamePanel gamePanel;
    private boolean visible;
    private JLabel coinsLabel;
    private Map<String, Integer> baseCosts;
    private Map<String, Integer> maxLevels;
    private Font pixelPurlFont;

    /**
     * Creates a new upgrade panel.
     *
     * @param gamePanel Reference to the game panel
     * @param player Reference to the player
     */
    public UpgradePanel(GamePanel gamePanel, Player player) {
        this.gamePanel = gamePanel;
        this.player = player;
        initializeBaseCosts();
        initializeUpgradePanel();
        try {
            pixelPurlFont = Font.createFont(Font.TRUETYPE_FONT,
                    getClass().getResourceAsStream("/fonts/PixelPurl.ttf")).deriveFont(24f);
        } catch (Exception e) {
            pixelPurlFont = new Font("Arial", Font.BOLD, 24);
            e.printStackTrace();
        }
    }

    /**
     * Initializes base costs for all upgrades.
     */
    private void initializeBaseCosts() {
        baseCosts = new HashMap<>();
        baseCosts.put("Damage", 50);
        baseCosts.put("HP", 55);
        baseCosts.put("Defense", 100);

        maxLevels = new HashMap<>();
        maxLevels.put("Damage", 999);
        maxLevels.put("HP", 40);
        maxLevels.put("Defense", 50);
    }

    /**
     * Initializes panel components and layout.
     */
    private void initializeUpgradePanel() {
        setLayout(new BorderLayout());

        int panelWidth = 500;
        int panelHeight = 400;
        setBounds((GamePanel.PANEL_WIDTH - panelWidth) / 2, (GamePanel.PANEL_HEIGHT - panelHeight) / 2, panelWidth, panelHeight);

        setBackground(new Color(40, 40, 50));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.ORANGE, 3),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        coinsLabel = new JLabel("Coins: " + player.getCoins(), JLabel.CENTER);
        coinsLabel.setForeground(Color.YELLOW);
        coinsLabel.setFont(pixelPurlFont);
        coinsLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        add(coinsLabel, BorderLayout.NORTH);

        JPanel upgradesPanel = new JPanel(new GridLayout(3, 1, 0, 15));
        upgradesPanel.setBackground(new Color(40, 40, 50));

        upgradesPanel.add(createUpgradeButton("Damage"));
        upgradesPanel.add(createUpgradeButton("HP"));
        upgradesPanel.add(createUpgradeButton("Defense"));

        add(upgradesPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout());
        bottomPanel.setBackground(new Color(40, 40, 50));

        JButton playAgainButton = new JButton("Play Again");
        playAgainButton.setFont(pixelPurlFont);
        playAgainButton.setBackground(new Color(60, 120, 60));
        playAgainButton.setForeground(Color.WHITE);
        playAgainButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GREEN, 2),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        playAgainButton.setFocusPainted(false);
        playAgainButton.addActionListener(e -> {
            gamePanel.restartGame();
            hidePanel();
        });

        bottomPanel.add(playAgainButton);
        add(bottomPanel, BorderLayout.SOUTH);

        setVisible(false);
    }

    /**
     * Creates a custom upgrade button for a specific stat.
     *
     * @param statName The stat to upgrade
     * @return Configured JButton
     */
    private JButton createUpgradeButton(String statName) {
        JButton button = new JButton() {
            @Override
            public void paintComponent(Graphics g) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());

                g.setColor(getForeground());
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                g.drawRect(1, 1, getWidth() - 3, getHeight() - 3);

                g.setColor(Color.WHITE);
                g.setFont(pixelPurlFont);

                int currentValue = getCurrentStatValue(statName);
                int cost = calculateCost(statName);
                boolean isMaxed = isStatMaxed(statName);

                String mainText = statName + " Upgrade";
                String currentText = "Current: " + currentValue;
                String costText = isMaxed ? "MAX LEVEL" : "Cost: " + cost + " coins";

                FontMetrics fm = g.getFontMetrics();
                int textHeight = fm.getHeight();
                int y = (getHeight() - (textHeight * 3)) / 2 + fm.getAscent();

                int x = (getWidth() - fm.stringWidth(mainText)) / 2;
                g.drawString(mainText, x, y);
                y += textHeight;

                g.setColor(Color.CYAN);
                x = (getWidth() - fm.stringWidth(currentText)) / 2;
                g.drawString(currentText, x, y);
                y += textHeight;

                if (isMaxed) {
                    g.setColor(Color.RED);
                } else {
                    g.setColor(player.getCoins() >= cost ? Color.GREEN : Color.RED);
                }
                x = (getWidth() - fm.stringWidth(costText)) / 2;
                g.drawString(costText, x, y);
            }
        };

        button.setPreferredSize(new Dimension(400, 80));
        button.setBackground(new Color(70, 70, 80));
        button.setForeground(Color.WHITE);
        button.setBorder(null);
        button.setFocusPainted(false);

        button.addActionListener(e -> upgradeStat(statName));

        return button;
    }

    /**
     * Gets current value of specified stat.
     *
     * @param statName The stat name
     * @return Current stat value
     */
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

    /**
     * Calculates current upgrade cost for a stat.
     *
     * @param statName The stat name
     * @return Current upgrade cost
     */
    private int calculateCost(String statName) {
        int baseCost = baseCosts.get(statName);
        int currentLevel = getCurrentUpgradeLevel(statName);

        return (int) (baseCost * Math.pow(1.2, currentLevel));
    }

    /**
     * Gets current upgrade level for a stat.
     *
     * @param statName The stat name
     * @return Current upgrade level
     */
    private int getCurrentUpgradeLevel(String statName) {
        switch (statName) {
            case "Damage":
                return player.getDamage() - 1;
            case "HP":
                int baseHp = 100;
                return Math.max(0, (player.getHp() - baseHp) / 10);
            case "Defense":
                return player.getDefense();
            default:
                return 0;
        }
    }

    /**
     * Checks if stat is at maximum level.
     *
     * @param statName The stat name
     * @return True if max level reached
     */
    private boolean isStatMaxed(String statName) {
        int currentLevel = getCurrentUpgradeLevel(statName);
        return currentLevel >= maxLevels.get(statName);
    }

    /**
     * Upgrades the specified player stat.
     *
     * @param stat The stat to upgrade
     */
    private void upgradeStat(String stat) {
        if (isStatMaxed(stat)) {
            return;
        }

        int cost = calculateCost(stat);

        if (player.getCoins() >= cost) {
            player.setCoins(player.getCoins() - cost);

            switch (stat) {
                case "Damage":
                    player.increaseDamage();
                    break;
                case "HP":
                    player.increaseHp();
                    break;
                case "Defense":
                    player.increaseDefense();
                    break;
            }

            player.saveState("player_save.dat");
            updateAbilityPanel();
        }
    }

    /**
     * Updates the panel display.
     */
    public void updateAbilityPanel() {
        coinsLabel.setFont(pixelPurlFont);
        coinsLabel.setText("Coins: " + player.getCoins());
        repaint();
    }

    /**
     * Shows the panel.
     */
    public void showPanel() {
        setVisible(true);
        updateAbilityPanel();
        visible = true;
    }

    /**
     * Hides the panel.
     */
    public void hidePanel() {
        setVisible(false);
        visible = false;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }
}