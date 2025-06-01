package WATVA;

import javax.swing.*;
import java.awt.*;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AbilityPanel extends JPanel {
    private final Player player;
    private final GamePanel gamePanel;
    private List<Ability> allAbilities;
    private JLabel coinsLabel;
    private JPanel buttonPanel;
    private Timer delayTimer;
    private boolean buttonsVisible = false;
    private Font pixelPurlFont;

    public AbilityPanel(GamePanel gamePanel, Player player) {
        this.gamePanel = gamePanel;
        this.player = player;
        initializeAbilities();
        initializeAbilityPanel();
        try {
            FileInputStream fontStream = new FileInputStream("res/fonts/PixelPurl.ttf");
            pixelPurlFont = Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont(24f);
        } catch (Exception e) {
            pixelPurlFont = new Font("Arial", Font.BOLD, 24);
            e.printStackTrace();
        }
    }

    private void initializeAbilities() {
        allAbilities = new ArrayList<>();
        allAbilities.add(new Ability("Double Shot", "Shoot two arrows at once",  "double"));
        allAbilities.add(new Ability("Backward Shot", "Shoot backward arrow",  "backward"));
        allAbilities.add(new Ability("Piercing Arrows", "Arrows pierce enemies",  "piercing"));
        allAbilities.add(new Ability("Slow Enemies", "Slow enemy movement",  "slow"));
        allAbilities.add(new Ability("Fire Arrows", "Burn enemies over time",  "fire"));
        allAbilities.add(new Ability("Speed Boost", "Increase movement speed",  "speed"));
        allAbilities.add(new Ability("Explosion+", "Bigger explosions",  "explosion"));
        allAbilities.add(new Ability("Health Regen", "Regenerate health",  "regen"));
        allAbilities.add(new Ability("Shield", "Damage absorption",  "shield"));
    }

    private void initializeAbilityPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(30, 30, 30, 200));
        setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3));
        setVisible(false);
        setBounds((GamePanel.PANEL_WIDTH - 500) / 2, (GamePanel.PANEL_HEIGHT - 400) / 2, 500, 400);
    }

    private boolean isAbilityAvailable(Ability ability) {
        switch (ability.getType()) {
            case "double":
                return !player.isDoubleShotActive();
            case "backward":
                return !player.isForwardBackwardShotActive();
            case "piercing":
                return player.getPiercingLevel() < 3;
            case "slow":
                return !player.hasSlowEnemies() || player.getSlowLevel() < 3;
            case "fire":
                return player.getFireLevel() < 3;
            case "speed":
                return player.getSpeed() < 11;
            case "explosion":
                return player.getExplosionRangeLevel() < 3;
            case "regen":
                return player.getRegenerationLevel() < 3;
            case "shield":
                return player.getShieldLevel() < 3;
            default:
                return true;
        }
    }

    private List<Ability> getAvailableAbilities() {
        List<Ability> availableAbilities = new ArrayList<>();
        for (Ability ability : allAbilities) {
            if (isAbilityAvailable(ability)) {
                availableAbilities.add(ability);
            }
        }
        return availableAbilities;
    }

    public void showRandomAbilities() {
        removeAll();
        buttonsVisible = false;

        if (delayTimer != null && delayTimer.isRunning()) {
            delayTimer.stop();
        }

        List<Ability> availableAbilities = getAvailableAbilities();

        if (availableAbilities.isEmpty()) {
            setVisible(false);
            gamePanel.startNextWaveAfterAbility();
            return;
        }

        Collections.shuffle(availableAbilities);

        coinsLabel = new JLabel("Coins: " + player.getCoins(), JLabel.CENTER);
        coinsLabel.setForeground(Color.WHITE);
        coinsLabel.setFont(pixelPurlFont);

        buttonPanel = new JPanel();
        int numAbilities = Math.min(3, availableAbilities.size());
        buttonPanel.setLayout(new GridLayout(numAbilities, 1, 10, 10));
        buttonPanel.setBackground(new Color(30, 30, 30, 200));

        add(coinsLabel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);

        revalidate();
        repaint();
        setVisible(true);

        delayTimer = new Timer(500, e -> showButtons(availableAbilities));
        delayTimer.setRepeats(false);
        delayTimer.start();
    }

    private void showButtons(List<Ability> availableAbilities) {
        buttonsVisible = true;
        buttonPanel.removeAll();

        int numToShow = Math.min(3, availableAbilities.size());
        for (int i = 0; i < numToShow; i++) {
            Ability ability = availableAbilities.get(i);
            JButton btn = createAbilityButton(ability);
            buttonPanel.add(btn);
        }

        buttonPanel.revalidate();
        buttonPanel.repaint();
    }

    private JButton createAbilityButton(Ability ability) {
        String buttonText = "<html><center>" + ability.getName();

        switch (ability.getType()) {
            case "piercing":
                buttonText += " (Level " + (player.getPiercingLevel() + 1) + "/3)";
                break;
            case "slow":
                if (player.hasSlowEnemies()) {
                    buttonText += " (Level " + (player.getSlowLevel() + 1) + "/3)";
                }
                break;
            case "fire":
                buttonText += " (Level " + (player.getFireLevel() + 1) + "/3)";
                break;
            case "speed":
                int speedLevel = (player.getSpeed() - 5) / 2;
                buttonText += " (Level " + (speedLevel + 1) + "/3)";
                break;
            case "explosion":
                buttonText += " (Level " + (player.getExplosionRangeLevel() + 1) + "/3)";
                break;
            case "regen":
                buttonText += " (Level " + (player.getRegenerationLevel() + 1) + "/3)";
                break;
            case "shield":
                buttonText += " (Level " + (player.getShieldLevel() + 1) + "/3)";
                break;
        }

        buttonText += "<br>" + ability.getDescription() + "</center></html>";

        JButton btn = new JButton(buttonText);
        btn.setFont(pixelPurlFont);
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(70, 70, 70));
        btn.setBorder(BorderFactory.createLineBorder(Color.ORANGE, 2));
        btn.setFocusPainted(false);

        btn.addActionListener(e -> {
            if (buttonsVisible) {
                applyAbility(ability);
                gamePanel.startNextWaveAfterAbility();
            }
        });
        return btn;
    }

    private void applyAbility(Ability ability) {
        switch (ability.getType()) {
            case "double":
                player.setDoubleShotActive(true);
                break;
            case "backward":
                player.setForwardBackwardShotActive(true);
                break;
            case "piercing":
                player.upgradePiercing();
                break;
            case "slow":
                if (!player.hasSlowEnemies()) {
                    player.setSlowEnemiesUnlocked(true);
                } else {
                    player.upgradeSlow();
                }
                break;
            case "fire":
                player.upgradeFire();
                break;
            case "speed":
                player.upgradeSpeed();
                break;
            case "explosion":
                player.upgradeExplosion();
                break;
            case "regen":
                player.upgradeRegeneration();
                break;
            case "shield":
                player.upgradeShield();
                break;
        }
        updateAbilityPanel();
    }

    public void updateAbilityPanel() {
        if (coinsLabel != null) {
            coinsLabel.setText("Coins: " + player.getCoins());
            repaint();
        }
    }

    public void showPanel() {
        List<Ability> availableAbilities = getAvailableAbilities();
        if (availableAbilities.isEmpty()) {
            gamePanel.startNextWaveAfterAbility();
            return;
        }

        showRandomAbilities();
        setVisible(true);
        revalidate();
        repaint();
    }

    public void hidePanel() {
        setVisible(false);
        if (delayTimer != null && delayTimer.isRunning()) {
            delayTimer.stop();
        }
        buttonsVisible = false;
    }
}