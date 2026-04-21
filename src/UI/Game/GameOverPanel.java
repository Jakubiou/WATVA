package UI.Game;

import Core.Game;
import MainMenu.MainMenuPanel;
import javax.swing.*;
import java.awt.*;

public class GameOverPanel extends JPanel {
    private final Game game;
    private final GamePanel gamePanel;
    private Font pixelFont;
    private Font smallFont;
    private final long showTime = System.currentTimeMillis();

    public GameOverPanel(Core.Game game, GamePanel gamePanel) {
        this.game = game;
        this.gamePanel = gamePanel;
        try {
            pixelFont = Font.createFont(Font.TRUETYPE_FONT,
                    getClass().getResourceAsStream("/fonts/PixelPurl.ttf")).deriveFont(Font.BOLD, 22f);
            smallFont = pixelFont.deriveFont(Font.PLAIN, 14f);
        } catch (Exception e) {
            pixelFont = new Font("Courier New", Font.BOLD, 22);
            smallFont = new Font("Courier New", Font.PLAIN, 14);
        }
        initComponents();
    }

    private void initComponents() {
        setLayout(null);
        setOpaque(false);
        int pw = 300, ph = 340;
        setBounds((GamePanel.PANEL_WIDTH - pw) / 2,
                (GamePanel.PANEL_HEIGHT - ph) / 2, pw, ph);

        int btnW = 220, btnH = 44, bx = (pw - btnW) / 2;
        // All buttons use the same neutral dark style — no colour coding
        addBtn("Play Again", bx, 158, btnW, btnH, e -> gamePanel.restartGame());
        addBtn("Upgrades",   bx, 214, btnW, btnH, e -> { setVisible(false); gamePanel.initializeUpgradePanel(); });
        addBtn("Main Menu",  bx, 270, btnW, btnH, e -> { new MainMenuPanel(); gamePanel.closeGame(); });
    }

    private void addBtn(String label, int x, int y, int w, int h,
                        java.awt.event.ActionListener al) {
        Color base = new Color(30, 26, 52);
        Color hov  = new Color(55, 48, 90);
        JButton btn = new JButton(label) {
            boolean hv = false;
            { addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) { hv = true;  repaint(); }
                public void mouseExited (java.awt.event.MouseEvent e) { hv = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hv ? hov : base);
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                g2.setStroke(new BasicStroke(1.4f));
                g2.setColor(hv ? new Color(140, 120, 200) : new Color(80, 70, 130));
                g2.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 10, 10);
                g2.setStroke(new BasicStroke(1));
                g2.setFont(pixelFont.deriveFont(Font.BOLD, 17f));
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(hv ? Color.WHITE : new Color(210, 205, 230));
                g2.drawString(getText(),
                        (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
        };
        btn.setOpaque(false); btn.setContentAreaFilled(false);
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setBounds(x, y, w, h);
        btn.addActionListener(al);
        add(btn);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        long elapsed = System.currentTimeMillis() - showTime;
        float alpha = Math.min(1f, elapsed / 300f);
        Composite saved = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        // Background
        g2.setColor(new Color(6, 4, 16, 232));
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);

        // Border
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(140, 20, 20));
        g2.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 15, 15);
        g2.setStroke(new BasicStroke(1));

        // Divider
        g2.setColor(new Color(100, 15, 15, 120));
        g2.fillRect(18, 138, getWidth() - 36, 1);

        // "YOU DIED"
        g2.setFont(pixelFont.deriveFont(Font.BOLD, 32f));
        FontMetrics fm = g2.getFontMetrics();
        String title = "YOU DIED";
        int tx = (getWidth() - fm.stringWidth(title)) / 2;
        // dark outline
        g2.setColor(new Color(100, 0, 0, 150));
        for (int ox = -2; ox <= 2; ox++)
            for (int oy = -2; oy <= 2; oy++)
                if (ox != 0 || oy != 0)
                    g2.drawString(title, tx+ox, 78+oy);
        g2.setColor(new Color(220, 60, 60));
        g2.drawString(title, tx, 78);

        // Subtitle
        g2.setFont(smallFont.deriveFont(13f));
        fm = g2.getFontMetrics();
        String sub = "Your journey ends here.";
        g2.setColor(new Color(130, 95, 100));
        g2.drawString(sub, (getWidth()-fm.stringWidth(sub))/2, 106);

        // Wave reached
        String wave = "Wave reached: " + GamePanel.getWaveNumber();
        g2.setColor(new Color(150, 125, 90));
        g2.drawString(wave, (getWidth()-fm.stringWidth(wave))/2, 126);

        g2.setComposite(saved);
    }
}