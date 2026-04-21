package UI.Panels;

import Logic.Level.LevelManager;
import Player.Player;
import UI.Game.GamePanel;

import java.awt.*;
import javax.swing.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class UpgradePanel extends JPanel {
    private final Player player;
    private final GamePanel gamePanel;
    private final LevelManager levelManager;
    private boolean visible;
    private JLabel coinsLabel;
    private Font pixelFont;
    private Font smallFont;

    private static final Map<String, Integer> BASE_COSTS = new LinkedHashMap<>();
    static {
        BASE_COSTS.put("Damage",        500);
        BASE_COSTS.put("HP",            450);
        BASE_COSTS.put("Defense",      800);
        BASE_COSTS.put("Crit Chance",  10000);
        BASE_COSTS.put("Shield Absorb", 5000);
    }

    private static final Map<String, Color> ACCENT = new LinkedHashMap<>();
    static {
        ACCENT.put("Damage",        new Color(255, 80,  80));
        ACCENT.put("HP",            new Color(80,  220, 100));
        ACCENT.put("Defense",       new Color(80,  160, 255));
        ACCENT.put("Crit Chance",   new Color(220, 80,  255));
        ACCENT.put("Shield Absorb", new Color(80,  220, 255));
    }

    public UpgradePanel(GamePanel gamePanel, Player player, LevelManager levelManager) {
        this.gamePanel = gamePanel;
        this.player = player;
        this.levelManager = levelManager;
        try {
            pixelFont = Font.createFont(Font.TRUETYPE_FONT,
                    getClass().getResourceAsStream("/fonts/PixelPurl.ttf")).deriveFont(Font.BOLD, 19f);
            smallFont = pixelFont.deriveFont(Font.PLAIN, 14f);
        } catch (Exception e) {
            pixelFont = new Font("Courier New", Font.BOLD, 19);
            smallFont = new Font("Courier New", Font.PLAIN, 14);
        }
        initPanel();
    }

    private void initPanel() {
        setLayout(null);
        setOpaque(false);

        // Wider panel: 3 cols × 2 rows, each card 200×130
        int cols = 3, cardW = 200, cardH = 130, gapX = 18, gapY = 16;
        int gridW = cols * cardW + (cols-1) * gapX;
        int pw = gridW + 48;     // padding
        int ph = 2 * cardH + gapY + 200; // title area + 2 rows + play button
        setBounds((GamePanel.PANEL_WIDTH - pw) / 2, (GamePanel.PANEL_HEIGHT - ph) / 2, pw, ph);

        // Coins label
        coinsLabel = new JLabel("", JLabel.CENTER);
        coinsLabel.setForeground(new Color(255, 215, 0));
        coinsLabel.setFont(pixelFont.deriveFont(Font.BOLD, 22f));
        coinsLabel.setBounds(0, 60, pw, 32);
        add(coinsLabel);

        // Cards
        int startX = 24, startY = 90;
        int idx = 0;
        for (String stat : BASE_COSTS.keySet()) {
            int col = idx % cols, row = idx / cols;
            int bx = startX + col * (cardW + gapX);
            int by = startY + row * (cardH + gapY);
            UpgradeCard card = new UpgradeCard(stat);
            card.setBounds(bx, by, cardW, cardH);
            add(card);
            idx++;
        }

        // Play Again button
        int btnW = 200, btnH = 44;
        JButton play = makeBtn("Play Again", new Color(38, 148, 62), new Color(55, 190, 82));
        play.setBounds((pw - btnW) / 2, ph - btnH - 14, btnW, btnH);
        play.addActionListener(e -> { gamePanel.restartGame(); hidePanel(); });
        add(play);

        setVisible(false);
    }

    private JButton makeBtn(String txt, Color bg, Color hov) {
        JButton btn = new JButton(txt) {
            boolean h = false;
            { addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) { h = true;  repaint(); }
                public void mouseExited (java.awt.event.MouseEvent e) { h = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(h ? hov : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setStroke(new BasicStroke(1.5f));
                g2.setColor(h ? Color.WHITE : hov);
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 12, 12);
                g2.setStroke(new BasicStroke(1));
                g2.setFont(pixelFont.deriveFont(Font.BOLD, 17f));
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                        (getWidth()-fm.stringWidth(getText()))/2,
                        (getHeight()+fm.getAscent()-fm.getDescent())/2);
            }
        };
        btn.setOpaque(false); btn.setContentAreaFilled(false);
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        return btn;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(10, 8, 22, 238));
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(75, 55, 155));
        g2.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 20, 20);
        g2.setStroke(new BasicStroke(1));

        if (pixelFont != null) g2.setFont(pixelFont.deriveFont(Font.BOLD, 24f));
        FontMetrics fm = g2.getFontMetrics();
        String title = "UPGRADES";
        int tx = (getWidth() - fm.stringWidth(title)) / 2;
        g2.setColor(new Color(100, 70, 210, 90));
        g2.drawString(title, tx+1, 44);
        g2.setColor(new Color(185, 160, 255));
        g2.drawString(title, tx, 43);
    }

    // ── helpers ──────────────────────────────────────────────────────────────
    private int getMaxLevel(String stat) {
        if (levelManager != null) {
            var ld = levelManager.getLevel(levelManager.getCurrentLevel());
            if (ld != null) switch (stat) {
                case "Damage":  return ld.getMaxDamageUpgrade();
                case "HP":      return ld.getMaxHpUpgrade();
                case "Defense": return ld.getMaxDefenseUpgrade();
            }
        }
        return switch (stat) {
            case "Crit Chance" -> 5;
            case "Shield Absorb" -> 5;
            default -> 999;
        };
    }

    private int getCurrentLevel(String stat) {
        return switch (stat) {
            case "Damage"        -> player.getDamage() - 1;
            case "HP"            -> Math.max(0, (player.getHp() - 100) / 10);
            case "Defense"       -> player.getDefense();
            case "Crit Chance"   -> player.getCritChanceLevel();
            case "Shield Absorb" -> player.getShieldAbsorbLevel() - 1;
            default -> 0;
        };
    }

    private int calcCost(String stat) {
        return (int)(BASE_COSTS.getOrDefault(stat, 100) * Math.pow(1.2, getCurrentLevel(stat)));
    }

    private boolean isMaxed(String stat) {
        return getCurrentLevel(stat) >= getMaxLevel(stat);
    }

    private String valueText(String stat) {
        return switch (stat) {
            case "Damage"        -> "DMG  " + player.getDamage();
            case "HP"            -> "HP   " + player.getHp();
            case "Defense"       -> "DEF  " + player.getDefense() + "%";
            case "Crit Chance"   -> "CRIT " + player.getCritChance() + "%";
            case "Shield Absorb" -> "ABS  " + player.getShieldAbsorbLevel();
            default -> "";
        };
    }

    private void doUpgrade(String stat) {
        if (isMaxed(stat)) return;
        int cost = calcCost(stat);
        if (player.getCoins() < cost) return;
        player.setCoins(player.getCoins() - cost);
        switch (stat) {
            case "Damage"        -> player.increaseDamage();
            case "HP"            -> player.increaseHp();
            case "Defense"       -> player.increaseDefense();
            case "Crit Chance"   -> player.upgradeCritChance();
            case "Shield Absorb" -> player.upgradeShieldAbsorb();
        }
        player.saveState("player_save.dat");
        updatePanel();
    }

    public void updatePanel() {
        coinsLabel.setText("Coins: " + player.getCoins());
        repaint();
        for (Component c : getComponents()) c.repaint();
    }

    public void showPanel()  { setVisible(true);  updatePanel(); visible = true; }
    public void hidePanel()  { setVisible(false); visible = false; }
    @Override public boolean isVisible() { return visible; }

    // ── Upgrade card ─────────────────────────────────────────────────────────
    private class UpgradeCard extends JPanel {
        private final String stat;
        private boolean hovered = false;

        UpgradeCard(String stat) {
            this.stat = stat;
            setOpaque(false);
            addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) { hovered = true;  repaint(); }
                public void mouseExited (java.awt.event.MouseEvent e) { hovered = false; repaint(); }
                public void mouseClicked(java.awt.event.MouseEvent e) { doUpgrade(stat); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            boolean maxed  = isMaxed(stat);
            boolean canBuy = !maxed && player.getCoins() >= calcCost(stat);
            Color ac = ACCENT.getOrDefault(stat, Color.WHITE);

            // Card background
            Color bg = maxed   ? new Color(22, 68, 28, 220)
                    : hovered ? new Color(34, 28, 62, 235)
                    :           new Color(16, 12, 34, 215);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

            // Border
            g2.setStroke(new BasicStroke(maxed ? 2.2f : hovered ? 1.8f : 1.2f));
            g2.setColor(maxed ? new Color(50, 200, 70) : hovered ? ac : ac.darker().darker());
            g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 12, 12);
            g2.setStroke(new BasicStroke(1));

            // Top accent bar
            g2.setColor(new Color(ac.getRed(), ac.getGreen(), ac.getBlue(), maxed ? 200 : 120));
            g2.fillRoundRect(10, 0, getWidth()-20, 4, 2, 2);

            int y = 22;

            // Stat name
            Font nf = (pixelFont != null) ? pixelFont.deriveFont(Font.BOLD, 17f)
                    : new Font("Courier New", Font.BOLD, 17);
            g2.setFont(nf);
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(ac);
            String nameStr = stat.toUpperCase();
            g2.drawString(nameStr, (getWidth() - fm.stringWidth(nameStr)) / 2, y);
            y += 20;

            // Current value
            Font vf = (smallFont != null) ? smallFont.deriveFont(14f)
                    : new Font("Courier New", Font.PLAIN, 14);
            g2.setFont(vf);
            fm = g2.getFontMetrics();
            g2.setColor(new Color(195, 195, 220));
            String val = valueText(stat);
            g2.drawString(val, (getWidth() - fm.stringWidth(val)) / 2, y);
            y += 18;

            // Level pips — sized to fit maxLevel
            int maxL  = Math.min(getMaxLevel(stat), 10);
            int curL  = Math.min(getCurrentLevel(stat), maxL);
            int avail = getWidth() - 20;
            int pipW  = Math.min(22, (avail - (maxL-1)*4) / maxL);
            int pipH  = 8;
            int pipGap= 4;
            int totalW= maxL * pipW + (maxL-1) * pipGap;
            int pipX  = (getWidth() - totalW) / 2;
            for (int i = 0; i < maxL; i++) {
                int px = pipX + i*(pipW+pipGap);
                if (i < curL) {
                    g2.setColor(ac);
                    g2.fillRoundRect(px, y, pipW, pipH, 4, 4);
                    // shine
                    g2.setColor(new Color(255,255,255,60));
                    g2.fillRoundRect(px, y, pipW, pipH/2, 4, 4);
                } else {
                    g2.setColor(new Color(40, 38, 60));
                    g2.fillRoundRect(px, y, pipW, pipH, 4, 4);
                }
            }
            y += pipH + 14;

            // Cost or MAX
            Font cf = (pixelFont != null) ? pixelFont.deriveFont(Font.BOLD, 15f)
                    : new Font("Courier New", Font.BOLD, 15);
            g2.setFont(cf);
            fm = g2.getFontMetrics();
            if (maxed) {
                g2.setColor(new Color(55, 215, 75));
                String mx = "MAXED";
                g2.drawString(mx, (getWidth() - fm.stringWidth(mx)) / 2, y);
            } else {
                String costStr = "Cost: " + calcCost(stat);
                g2.setColor(canBuy ? new Color(255, 210, 0) : new Color(185, 60, 60));
                g2.drawString(costStr, (getWidth() - fm.stringWidth(costStr)) / 2, y);
            }
        }
    }
}