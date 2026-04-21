package Player;

import Logic.GameLogic;
import UI.Game.GamePanel;
import Core.Game;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;

/**
 * Handles all visual rendering for the player character.
 * Manages player animations, health bar display, and ability cooldown indicators.
 */
public class PlayerGraphics {
    private Player player;
    private transient Image[] rightTextures, leftTextures, idleTextures, upTextures, downTextures;
    private transient Image hpBarFrame1, hpBarFrame2, hpBarFrame3;

    /**
     * Creates a new PlayerGraphics instance tied to a specific player.
     *
     * @param player The Player instance this graphics object will represent
     */
    public PlayerGraphics(Player player) {
        this.player = player;
        loadTextures();
    }

    /**
     * Loads all texture assets required for player rendering.
     * Includes directional sprites, idle animations, and health bar frames.
     */
    private void loadTextures() {
        rightTextures = loadTextures("Player1", "Player2", "Player3", "Player4");
        leftTextures = loadTextures("Player5", "Player6", "Player7", "Player8");
        upTextures = loadTextures("Player9", "Player10", "Player11", "Player12");
        downTextures = loadTextures("Player13", "Player14", "Player15", "Player16");
        idleTextures = loadTextures("Player17", "Player18", "Player19", "Player20");
        try {
            hpBarFrame1 = ImageIO.read(getClass().getResourceAsStream("/WATVA/Player/HPBar1.png"));
            hpBarFrame2 = ImageIO.read(getClass().getResourceAsStream("/WATVA/Player/HPBar2.png"));
            hpBarFrame3 = ImageIO.read(getClass().getResourceAsStream("/WATVA/Player/HPBar3.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method to load a set of textures from resource files.
     *
     * @param filenames Array of filenames (without extension) to load
     * @return Array of loaded Image objects
     */
    private Image[] loadTextures(String... filenames) {
        Image[] textures = new Image[filenames.length];
        try {
            for (int i = 0; i < filenames.length; i++) {
                textures[i] = ImageIO.read(getClass().getResourceAsStream("/WATVA/Player/" + filenames[i] + ".png"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return textures;
    }

    /**
     * Core.Main rendering method that draws the player and associated UI elements.
     *
     * @param g The Graphics context to render to
     */
    public void draw(Graphics g) {
        Image[] textures = idleTextures;

        if(!player.isIdle()) {
            if (player.isRight()) {
                textures = rightTextures;
            } else if (player.isLeft()) {
                textures = leftTextures;
            } else if (player.isUp()) {
                textures = upTextures;
            } else if (player.isDown()) {
                textures = downTextures;
            }
        }

        g.drawImage(textures[player.getCurrentFrame()], player.getX(), player.getY(),
                Player.WIDTH, Player.HEIGHT, null);

        drawDashCooldown(g);
        for (Explosion explosion : player.getExplosions()) {
            explosion.draw(g);
        }
        drawExplosionCooldown(g);
        drawShieldBeam(g);
        drawShieldBeamCooldown(g);
        drawHealthBar(g);
    }

    /**
     * Draws the player's health bar with visual progression.
     * Shows different colors and frames based on current health level.
     *
     * @param g The Graphics context to render to
     */
    private void drawHealthBar(Graphics g) {
        int hpBarWidth = Game.scale(270);
        int hpBarHeight = Game.scale(30);
        int hpBarX = GamePanel.PANEL_WIDTH - hpBarWidth - Game.scale(10) + GameLogic.cameraX;
        int hpBarY = Game.scale(35) + GameLogic.cameraY;

        if (player.getHp() > 0) {
            g.setColor(Color.BLACK);
            g.drawRect(hpBarX, hpBarY, hpBarWidth, hpBarHeight);
        }

        if (player.getShieldLevel() > 0) {
            int shieldWidth = player.getShieldHP() * hpBarWidth / Player.MAX_SHIELD_HP;
            g.setColor(Color.BLUE);
            g.fillRect(hpBarX, hpBarY + hpBarHeight + Game.scale(5), shieldWidth, hpBarHeight);
            g.setColor(Color.BLACK);
            g.drawRect(hpBarX, hpBarY + hpBarHeight + Game.scale(5), hpBarWidth, hpBarHeight);
            g.setColor(Color.BLACK);
            for (int i = 1; i <= 9; i++) {
                int dividerX = hpBarX + (i * hpBarWidth / 10);
                g.drawLine(dividerX, hpBarY + hpBarHeight + Game.scale(5),
                        dividerX, hpBarY + hpBarHeight + Game.scale(5) + hpBarHeight);
            }
        }

        if (player.getHp() > 0) {
            int redWidth = Math.min(player.getHp(), 100) * hpBarWidth / 100;
            g.setColor(Color.RED);
            g.fillRect(hpBarX, hpBarY, redWidth, hpBarHeight);
            if (hpBarFrame1 != null) {
                int frameWidth = Game.scale(307);
                int frameHeight = Game.scale(75);
                int frameX = hpBarX - Game.scale(37);
                int frameY = hpBarY - Game.scale(30);
                g.drawImage(hpBarFrame1, frameX, frameY, frameWidth, frameHeight, null);
            }
        }

        if (player.getHp() > 100) {
            int purpleWidth = Math.min(player.getHp() - 100, 200) * hpBarWidth / 200;
            g.setColor(Color.MAGENTA);
            g.fillRect(hpBarX, hpBarY, purpleWidth, hpBarHeight);
            if (hpBarFrame2 != null) {
                int frameWidth = Game.scale(307);
                int frameHeight = Game.scale(75);
                int frameX = hpBarX - Game.scale(37);
                int frameY = hpBarY - Game.scale(30);
                g.drawImage(hpBarFrame2, frameX, frameY, frameWidth, frameHeight, null);
            }
        }

        if (player.getHp() > 300) {
            int goldWidth = Math.min(player.getHp() - 300, 200) * hpBarWidth / 200;
            g.setColor(Color.YELLOW);
            g.fillRect(hpBarX, hpBarY, goldWidth, hpBarHeight);
            if (hpBarFrame3 != null) {
                int frameWidth = Game.scale(307);
                int frameHeight = Game.scale(75);
                int frameX = hpBarX - Game.scale(37);
                int frameY = hpBarY - Game.scale(30);
                g.drawImage(hpBarFrame3, frameX, frameY, frameWidth, frameHeight, null);
            }
        }

        if (player.getHp() > 0) {
            g.setColor(Color.BLACK);
            for (int i = 1; i <= 9; i++) {
                int dividerX = hpBarX + (i * hpBarWidth / 10);
                g.drawLine(dividerX, hpBarY, dividerX, hpBarY + hpBarHeight);
            }
        }
    }

    /**
     * Draws the dash ability cooldown indicator.
     * Shows remaining cooldown as a circular progress meter.
     *
     * @param g The Graphics context to render to
     */
    private void drawDashCooldown(Graphics g) {
        long timeSinceLastDash = System.currentTimeMillis() - player.getLastDashTime();
        if (timeSinceLastDash < player.getDashCooldown()) {
            double percentage = 1 - (double) timeSinceLastDash / player.getDashCooldown();

            int radius = Game.scale(30);
            int centerX = Game.scale(50) + GameLogic.cameraX;
            int centerY = GamePanel.PANEL_HEIGHT - Game.scale(50) + GameLogic.cameraY;

            g.setColor(Color.RED);
            g.fillArc(centerX - radius, centerY - radius, radius * 2, radius * 2,
                    90, (int) (360 * percentage));
        }
    }

    /**
     * Draws the explosion ability cooldown indicator.
     * Shows remaining cooldown as a circular progress meter.
     *
     * @param g The Graphics context to render to
     */
    private void drawExplosionCooldown(Graphics g) {
        long timeSinceLastExplosion = System.currentTimeMillis() - player.getLastExplosionTime();
        if (timeSinceLastExplosion < player.getExplosionCooldown()) {
            double percentage = 1 - (double) timeSinceLastExplosion / player.getExplosionCooldown();

            int radius = Game.scale(30);
            int centerX = Game.scale(100) + GameLogic.cameraX;
            int centerY = GamePanel.PANEL_HEIGHT - Game.scale(50) + GameLogic.cameraY;

            g.setColor(Color.ORANGE);
            g.fillArc(centerX - radius, centerY - radius, radius * 2, radius * 2,
                    90, (int) (360 * percentage));
        }
    }

    private void drawShieldBeam(Graphics g) {
        if (!player.isShieldBeamActive()) return;
        Graphics2D g2d = (Graphics2D) g;

        int cx = player.getX() + Player.WIDTH / 2;
        int cy = player.getY() + Player.HEIGHT / 2;
        int r = (int)(Player.WIDTH * 1.35);

        double dx = player.getShieldMouseX() - cx;
        double dy = player.getShieldMouseY() - cy;
        double angleDeg = Math.toDegrees(Math.atan2(-dy, dx));
        int arcStart = (int)(angleDeg - 90.0);
        int arcExtent = 180;

        long elapsed = System.currentTimeMillis() - player.getShieldBeamStartTime();
        float pulse = (float)(0.72 + 0.28 * Math.sin(elapsed / 90.0));

        Composite saved = g2d.getComposite();

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f * pulse));
        g2d.setColor(new Color(40, 140, 255));
        int gr = r + Game.scale(8);
        g2d.fillArc(cx - gr, cy - gr, gr * 2, gr * 2, arcStart, arcExtent);

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.38f * pulse));
        g2d.setColor(new Color(70, 170, 255));
        g2d.fillArc(cx - r, cy - r, r * 2, r * 2, arcStart, arcExtent);

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.92f));
        g2d.setColor(new Color(160, 220, 255));
        g2d.setStroke(new java.awt.BasicStroke(Game.scale(3),
                java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
        g2d.drawArc(cx - r, cy - r, r * 2, r * 2, arcStart, arcExtent);

        g2d.setStroke(new java.awt.BasicStroke(Game.scale(2),
                java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
        g2d.setColor(new Color(200, 235, 255));
        double a1r = Math.toRadians(arcStart);
        double a2r = Math.toRadians(arcStart + arcExtent);
        g2d.drawLine(cx, cy,
                cx + (int)(Math.cos(a1r) * r),
                cy - (int)(Math.sin(a1r) * r));
        g2d.drawLine(cx, cy,
                cx + (int)(Math.cos(a2r) * r),
                cy - (int)(Math.sin(a2r) * r));

        int maxAbs = player.getShieldAbsorbLevel();
        int remAbs = player.getShieldBeamAbsorbsLeft();
        int dotR   = Game.scale(5);
        for (int i = 0; i < maxAbs; i++) {
            double dotAngleDeg = arcStart + (i + 0.5) * ((double)arcExtent / maxAbs);
            double dar = Math.toRadians(dotAngleDeg);
            int dotX = cx + (int)(Math.cos(dar) * (r - Game.scale(6)));
            int dotY = cy - (int)(Math.sin(dar) * (r - Game.scale(6)));
            g2d.setColor(i < remAbs
                    ? new Color(100, 255, 190)
                    : new Color(45, 65, 90));
            g2d.fillOval(dotX - dotR, dotY - dotR, dotR * 2, dotR * 2);
        }

        g2d.setComposite(saved);
        g2d.setStroke(new java.awt.BasicStroke(1));
    }

    private void drawShieldBeamCooldown(Graphics g) {
        long timeSince = System.currentTimeMillis() - player.getLastShieldBeamTime();
        if (player.isShieldBeamActive() || timeSince >= player.getShieldBeamCooldown()) return;
        double pct = 1.0 - (double) timeSince / player.getShieldBeamCooldown();
        int radius = Game.scale(30);
        int centerX = Game.scale(150) + GameLogic.cameraX;
        int centerY = GamePanel.PANEL_HEIGHT - Game.scale(50) + GameLogic.cameraY;
        g.setColor(new Color(80, 180, 255));
        ((Graphics2D)g).fillArc(centerX - radius, centerY - radius, radius * 2, radius * 2,
                90, (int)(360 * pct));
    }

    public Image[] getIdleTextures() {
        return idleTextures;
    }
}