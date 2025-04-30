package WATVA;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AbilityPanel extends JPanel {
    private final Player player;
    private final GamePanel gamePanel;
    private List<Ability> allAbilities;
    private JLabel coinsLabel;

    public AbilityPanel(GamePanel gamePanel, Player player) {
        this.gamePanel = gamePanel;
        this.player = player;
        initializeAbilities();
        initializeAbilityPanel();
    }

    private void initializeAbilities() {
        allAbilities = new ArrayList<>();
        allAbilities.add(new Ability("Double Shot", "Shoot two arrows at once", 0, "double", 1));
        allAbilities.add(new Ability("Backward Shot", "Shoot backward arrow", 0, "backward", 1));
        allAbilities.add(new Ability("Piercing Arrows", "Arrows pierce enemies", 0, "piercing", 3));
        allAbilities.add(new Ability("Slow Enemies", "Slow enemy movement", 0, "slow", 1));
        allAbilities.add(new Ability("Fire Arrows", "Burn enemies over time", 0, "fire", 3));
        allAbilities.add(new Ability("Speed Boost", "Increase movement speed", 0, "speed", 3));
        allAbilities.add(new Ability("Explosion+", "Bigger explosions", 0, "explosion", 3));
        allAbilities.add(new Ability("Health Regen", "Regenerate health", 0, "regen", 3));
        allAbilities.add(new Ability("Shield", "Damage absorption", 0, "shield", 3));
    }

    private void initializeAbilityPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(30, 30, 30, 200));
        setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3));
        setVisible(false);
        setBounds((GamePanel.PANEL_WIDTH - 500) / 2, (GamePanel.PANEL_HEIGHT - 400) / 2, 500, 400);
    }

    public void showRandomAbilities() {
        removeAll();
        Collections.shuffle(allAbilities);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(3, 1, 10, 10));
        buttonPanel.setBackground(new Color(30, 30, 30, 200));

        for (int i = 0; i < 3; i++) {
            Ability ability = allAbilities.get(i);
            JButton btn = createAbilityButton(ability);
            buttonPanel.add(btn);
        }

        coinsLabel = new JLabel("Coins: " + player.getCoins(), JLabel.CENTER);
        coinsLabel.setForeground(Color.WHITE);
        coinsLabel.setFont(new Font("Arial", Font.BOLD, 20));

        add(coinsLabel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);

        revalidate();
        repaint();
        setVisible(true);
    }

    private JButton createAbilityButton(Ability ability) {
        JButton btn = new JButton("<html><center>" + ability.getName() +
                "<br>" + ability.getDescription() + "</center></html>");
        btn.setFont(new Font("Arial", Font.PLAIN, 16));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(70, 70, 70));
        btn.setBorder(BorderFactory.createLineBorder(Color.ORANGE, 2));
        btn.setFocusPainted(false);

        btn.addActionListener(e -> {
            applyAbility(ability);
            hidePanel();
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
                player.setSlowEnemiesUnlocked(true);
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
        showRandomAbilities();
        setVisible(true);
        revalidate();
        repaint();
    }

    public void hidePanel() {
        setVisible(false);
    }
}