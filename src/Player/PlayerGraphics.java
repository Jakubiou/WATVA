package Player;

import Logic.GameLogic;
import UI.GamePanel;
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
     * Main rendering method that draws the player and associated UI elements.
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
        drawHealthBar(g);
    }

    /**
     * Draws the player's health bar with visual progression.
     * Shows different colors and frames based on current health level.
     *
     * @param g The Graphics context to render to
     */
    private void drawHealthBar(Graphics g) {
        int hpBarWidth = (int)(270 * Game.getScaleFactor());
        int hpBarHeight = (int)(30 * Game.getScaleFactor());
        int hpBarX = GamePanel.PANEL_WIDTH - hpBarWidth - 10 + GameLogic.cameraX;
        int hpBarY = 35 + GameLogic.cameraY;

        if (player.getHp() > 0) {
            g.setColor(Color.BLACK);
            g.drawRect(hpBarX, hpBarY, hpBarWidth, hpBarHeight);
        }
        if (player.getShieldLevel() > 0) {
            int shieldWidth = player.getShieldHP() * hpBarWidth / Player.MAX_SHIELD_HP;
            g.setColor(Color.BLUE);
            g.fillRect(hpBarX, hpBarY + hpBarHeight + 5, shieldWidth, hpBarHeight);
            g.setColor(Color.BLACK);
            g.drawRect(hpBarX, hpBarY + hpBarHeight + 5, hpBarWidth, hpBarHeight);
            g.setColor(Color.BLACK);
            for (int i = 1; i <= 9; i++) {
                int dividerX = hpBarX + (i * hpBarWidth / 10);
                g.drawLine(dividerX, hpBarY + hpBarHeight + 5, dividerX, hpBarY + hpBarHeight + 5 + hpBarHeight);
            }
        }

        if (player.getHp() > 0) {
            int redWidth = Math.min(player.getHp(), 100) * hpBarWidth / 100;
            g.setColor(Color.RED);
            g.fillRect(hpBarX, hpBarY, redWidth, hpBarHeight);
            if (hpBarFrame1 != null) {
                g.drawImage(hpBarFrame1, hpBarX - 37, hpBarY - 30, hpBarWidth + 40, hpBarHeight + 45, null);
            }
        }

        if (player.getHp() > 100) {
            int purpleWidth = Math.min(player.getHp() - 100, 200) * hpBarWidth / 200;
            g.setColor(Color.MAGENTA);
            g.fillRect(hpBarX, hpBarY, purpleWidth, hpBarHeight);
            if (hpBarFrame2 != null) {
                g.drawImage(hpBarFrame2, hpBarX - 37, hpBarY - 30, hpBarWidth + 40, hpBarHeight + 45, null);
            }
        }

        if (player.getHp() > 300) {
            int goldWidth = Math.min(player.getHp() - 300, 200) * hpBarWidth / 200;
            g.setColor(Color.YELLOW);
            g.fillRect(hpBarX, hpBarY, goldWidth, hpBarHeight);
            if (hpBarFrame3 != null) {
                g.drawImage(hpBarFrame3, hpBarX - 37, hpBarY - 30, hpBarWidth + 40, hpBarHeight + 45, null);
            }
        }

        if (player.getHp() > 0 ) {
            g.setColor(Color.BLACK);
            for (int i = 1; i <= 9; i++) {
                int dividerX = hpBarX + (i * hpBarWidth / 10);
                g.drawLine(dividerX, hpBarY , dividerX, hpBarY + hpBarHeight);
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

            int radius = 30;
            int centerX = 50 + GameLogic.cameraX;
            int centerY = GamePanel.PANEL_HEIGHT - 50 + GameLogic.cameraY;

            g.setColor(Color.RED);
            g.fillArc(centerX - radius, centerY - radius, radius * 2, radius * 2, 90, (int) (360 * percentage));
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

            int radius = 30;
            int centerX = 100 + GameLogic.cameraX;
            int centerY = GamePanel.PANEL_HEIGHT - 50 + GameLogic.cameraY;

            g.setColor(Color.ORANGE);
            g.fillArc(centerX - radius, centerY - radius, radius * 2, radius * 2, 90, (int) (360 * percentage));
        }
    }

    public Image[] getIdleTextures() {
        return idleTextures;
    }
}