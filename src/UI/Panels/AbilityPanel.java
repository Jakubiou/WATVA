package UI.Panels;

import Core.Game;
import Logic.Ability.Ability;
import Player.Player;
import UI.Game.GamePanel;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AbilityPanel extends JPanel {
    private final Player player;
    private final GamePanel gamePanel;
    private List<Ability> allAbilities;
    private Timer delayTimer;
    private boolean buttonsVisible = false;
    private Font pixelFont;
    private Font smallFont;
    private BufferedImage demonQueenImg;

    // Ability buttons stored so we can repaint them
    private final List<AbilityButton> shownButtons = new ArrayList<>();

    public AbilityPanel(GamePanel gamePanel, Player player) {
        this.gamePanel = gamePanel;
        this.player = player;
        loadDemonQueen();
        initializeAbilities();
        initPanel();
        try {
            pixelFont = Font.createFont(Font.TRUETYPE_FONT,
                    getClass().getResourceAsStream("/fonts/PixelPurl.ttf")).deriveFont(Font.BOLD, 20f);
            smallFont = pixelFont.deriveFont(Font.PLAIN, 15f);
        } catch (Exception e) {
            pixelFont = new Font("Courier New", Font.BOLD, 20);
            smallFont = new Font("Courier New", Font.PLAIN, 15);
        }
    }

    private void loadDemonQueen() {
        String[] paths = {
                "/WATVA/enemy/Enemy_bossDemonQueen1.png",
                "/WATVA/enemy/DemonQueenBoss.png",
                "/WATVA/Boss/DemonQueen/DemonQueen1.png"
        };
        for (String p : paths) {
            try {
                var stream = getClass().getResourceAsStream(p);
                if (stream != null) { demonQueenImg = ImageIO.read(stream); break; }
            } catch (Exception ignored) {}
        }
    }

    private void initializeAbilities() {
        allAbilities = new ArrayList<>();
        allAbilities.add(new Ability("Double Shot",     "Fire two arrows simultaneously",   "double"));
        allAbilities.add(new Ability("Backward Shot",   "Also fires an arrow behind you",   "backward"));
        allAbilities.add(new Ability("Piercing Arrows", "Arrows pass through enemies",      "piercing"));
        allAbilities.add(new Ability("Slow Enemies",    "Hit enemies move slower",          "slow"));
        allAbilities.add(new Ability("Fire Arrows",     "Arrows ignite enemies over time",  "fire"));
        allAbilities.add(new Ability("Speed Boost",     "Increase your movement speed",     "speed"));
        allAbilities.add(new Ability("Explosion+",      "Expand explosion radius",          "explosion"));
        allAbilities.add(new Ability("Health Regen",    "Slowly regenerate HP",             "regen"));
        allAbilities.add(new Ability("Shield",          "Absorb incoming damage",           "shield"));
        allAbilities.add(new Ability("Ricochet",        "Arrows bounce off walls",          "ricochet"));
    }

    private void initPanel() {
        setLayout(null);
        setOpaque(false);
        int pw = 720, ph = 500;
        setBounds((GamePanel.PANEL_WIDTH - pw) / 2, (GamePanel.PANEL_HEIGHT - ph) / 2, pw, ph);
        setVisible(false);
    }

    private boolean isAvailable(Ability a) {
        return switch (a.getType()) {
            case "double"    -> !player.isDoubleShotActive();
            case "backward"  -> !player.isForwardBackwardShotActive();
            case "piercing"  -> player.getPiercingLevel() < 3;
            case "slow"      -> !player.hasSlowEnemies() || player.getSlowLevel() < 3;
            case "fire"      -> player.getFireLevel() < 3;
            case "speed"     -> player.getSpeed() < Game.scale(11);
            case "explosion" -> player.getExplosionRangeLevel() < 3;
            case "regen"     -> player.getRegenerationLevel() < 3;
            case "shield"    -> player.getShieldLevel() < 3;
            case "ricochet"  -> !player.hasRicochetAbility();
            default -> true;
        };
    }

    private List<Ability> getAvailable() {
        List<Ability> list = new ArrayList<>();
        for (Ability a : allAbilities) if (isAvailable(a)) list.add(a);
        return list;
    }

    public void showRandomAbilities() {
        removeAll();
        shownButtons.clear();
        buttonsVisible = false;
        if (delayTimer != null) delayTimer.stop();

        List<Ability> avail = getAvailable();
        if (avail.isEmpty()) {
            setVisible(false);
            gamePanel.startNextWaveAfterAbility();
            return;
        }

        Collections.shuffle(avail);
        setVisible(true);
        repaint();

        delayTimer = new Timer(200, e -> showButtons(avail));
        delayTimer.setRepeats(false);
        delayTimer.start();
    }

    private void showButtons(List<Ability> avail) {
        buttonsVisible = true;
        int pw = getWidth(), ph = getHeight();
        int imgW = demonQueenImg != null ? 240 : 0;
        int leftW = pw - imgW;

        int num = Math.min(3, avail.size());
        int btnH = 108, gap = 12;
        int totalH = num * btnH + (num - 1) * gap;
        int startY = (ph - totalH - 50) / 2 + 50;

        for (int i = 0; i < num; i++) {
            AbilityButton btn = new AbilityButton(avail.get(i));
            btn.setBounds(16, startY + i * (btnH + gap), leftW - 28, btnH);
            add(btn);
            shownButtons.add(btn);
        }

        if (demonQueenImg != null) {
            JPanel imgPanel = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2.drawImage(demonQueenImg, 0, 0, getWidth(), getHeight(), null);
                }
            };
            imgPanel.setOpaque(false);
            imgPanel.setBounds(leftW, 10, imgW - 10, ph - 20);
            add(imgPanel);
        }

        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background
        g2.setColor(new Color(12, 8, 28, 240));
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);

        // Border
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(100, 60, 160));
        g2.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 18, 18);
        g2.setStroke(new BasicStroke(1));

        // Vertical divider if image present
        if (demonQueenImg != null) {
            int lw = getWidth() - 240;
            g2.setColor(new Color(70, 40, 110, 140));
            g2.drawLine(lw, 16, lw, getHeight() - 16);
        }

        // Title
        if (pixelFont != null) g2.setFont(pixelFont.deriveFont(Font.BOLD, 22f));
        else g2.setFont(new Font("Courier New", Font.BOLD, 22));
        FontMetrics fm = g2.getFontMetrics();
        String title = "CHOOSE AN ABILITY";
        int tx = ((demonQueenImg != null ? getWidth()-240 : getWidth()) - fm.stringWidth(title)) / 2;
        g2.setColor(new Color(80, 40, 140, 100));
        g2.drawString(title, tx + 1, 34);
        g2.setColor(new Color(190, 160, 240));
        g2.drawString(title, tx, 33);
    }

    private void applyAbility(Ability a) {
        switch (a.getType()) {
            case "double"    -> player.setDoubleShotActive(true);
            case "backward"  -> player.setForwardBackwardShotActive(true);
            case "piercing"  -> player.upgradePiercing();
            case "slow"      -> { if (!player.hasSlowEnemies()) player.setSlowEnemiesUnlocked(true); else player.upgradeSlow(); }
            case "fire"      -> player.upgradeFire();
            case "speed"     -> player.upgradeSpeed();
            case "explosion" -> player.upgradeExplosion();
            case "regen"     -> player.upgradeRegeneration();
            case "shield"    -> player.upgradeShield();
            case "ricochet"  -> player.setRicochetAbility(true);
        }
    }

    public void showPanel() {
        if (getAvailable().isEmpty()) { gamePanel.startNextWaveAfterAbility(); return; }
        showRandomAbilities();
    }

    public void hidePanel() {
        setVisible(false);
        if (delayTimer != null) delayTimer.stop();
        buttonsVisible = false;
        shownButtons.clear();
    }

    public void updateAbilityPanel() { repaint(); }

    // ── Ability button ──────────────────────────────────────────────────────
    private class AbilityButton extends JPanel {
        private final Ability ability;
        private boolean hovered = false;

        AbilityButton(Ability ab) {
            this.ability = ab;
            setOpaque(false);
            addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) { hovered = true;  repaint(); }
                public void mouseExited (java.awt.event.MouseEvent e) { hovered = false; repaint(); }
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (buttonsVisible) { applyAbility(ability); gamePanel.startNextWaveAfterAbility(); }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color accent = accentFor(ability.getType());

            // Background
            g2.setColor(hovered ? new Color(35, 26, 60, 235) : new Color(18, 12, 38, 220));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

            // Border
            g2.setStroke(new BasicStroke(hovered ? 2.2f : 1.4f));
            g2.setColor(hovered ? accent : accent.darker().darker());
            g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 12, 12);
            g2.setStroke(new BasicStroke(1));

            // Left accent bar
            g2.setColor(accent);
            g2.fillRoundRect(0, 10, 4, getHeight()-20, 3, 3);

            // Name — bigger, no emoji
            Font nameFont = (pixelFont != null) ? pixelFont.deriveFont(Font.BOLD, 20f)
                    : new Font("Courier New", Font.BOLD, 20);
            g2.setFont(nameFont);
            FontMetrics fm = g2.getFontMetrics();
            String name = ability.getName() + getLevelSuffix(ability);
            g2.setColor(hovered ? Color.WHITE : new Color(215, 205, 255));
            g2.drawString(name, 16, fm.getAscent() + 12);

            // Description
            Font descFont = (smallFont != null) ? smallFont.deriveFont(14f)
                    : new Font("Courier New", Font.PLAIN, 14);
            g2.setFont(descFont);
            fm = g2.getFontMetrics();
            g2.setColor(new Color(150, 145, 180));
            g2.drawString(ability.getDescription(), 16, getHeight() - fm.getDescent() - 14);

            // Level pips in middle
            int maxL = 3;
            int curL = levelOf(ability);
            if (curL >= 0) {
                int pipW = 22, pipH = 7, pipGap = 5;
                int totalPipW = maxL * pipW + (maxL-1) * pipGap;
                int pipX = getWidth() - totalPipW - 14;
                int pipY = (getHeight() - pipH) / 2;
                for (int i = 0; i < maxL; i++) {
                    g2.setColor(i < curL ? accent : new Color(44, 40, 64));
                    g2.fillRoundRect(pipX + i*(pipW+pipGap), pipY, pipW, pipH, 4, 4);
                    if (i < curL) {
                        g2.setColor(new Color(255,255,255,60));
                        g2.fillRoundRect(pipX + i*(pipW+pipGap), pipY, pipW, pipH/2, 4, 4);
                    }
                }
            }

            // Hover shimmer
            if (hovered) {
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 18));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            }
        }
    }

    private Color accentFor(String type) {
        return switch (type) {
            case "double"    -> new Color(255, 210, 60);
            case "backward"  -> new Color(190, 140, 255);
            case "piercing"  -> new Color(90, 210, 255);
            case "slow"      -> new Color(140, 200, 255);
            case "fire"      -> new Color(255, 100, 50);
            case "speed"     -> new Color(240, 220, 0);
            case "explosion" -> new Color(255, 140, 30);
            case "regen"     -> new Color(70, 215, 120);
            case "shield"    -> new Color(70, 155, 255);
            case "ricochet"  -> new Color(185, 255, 140);
            default          -> new Color(170, 170, 210);
        };
    }

    private int levelOf(Ability a) {
        return switch (a.getType()) {
            case "piercing"  -> player.getPiercingLevel();
            case "slow"      -> player.getSlowLevel();
            case "fire"      -> player.getFireLevel();
            case "speed"     -> (int)((player.getSpeed() - Game.scale(5)) / (double)Game.scale(2));
            case "explosion" -> player.getExplosionRangeLevel();
            case "regen"     -> player.getRegenerationLevel();
            case "shield"    -> player.getShieldLevel();
            default          -> -1; // no pips for these
        };
    }

    private String getLevelSuffix(Ability a) {
        int lv = levelOf(a);
        if (lv < 0) return "";
        return "  [" + lv + "/3]";
    }
}