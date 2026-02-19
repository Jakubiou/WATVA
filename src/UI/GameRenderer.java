package UI;

import Core.Game;
import Enemies.Enemy;
import Logic.CrystalExplosion;
import Logic.DamageNumber.DamageNumberManager;
import Logic.GameLogic;
import Logic.MapManager;
import Logic.WallManager;
import Player.Player;
import Player.PlayerProjectile;

import java.awt.*;
import java.io.InputStream;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.imageio.ImageIO;

/**
 * Handles all rendering operations for the game.
 * Manages drawing of game objects, UI elements, and visual effects.
 */
public class GameRenderer {
    private GamePanel gamePanel;
    private Image[] blockImages;
    private Font pixelPurlFont;

    /**
     * Creates a new GameRenderer with references to game panel and font.
     *
     * @param gamePanel The game panel to render to
     * @param pixelPurlFont The custom font to use for UI elements
     */
    public GameRenderer(GamePanel gamePanel, Font pixelPurlFont) {
        this.gamePanel = gamePanel;
        this.pixelPurlFont = pixelPurlFont;
        loadBlockImages();
    }

    /**
     * Loads all block images used for background tiles.
     * Images are scaled to match the game's block size.
     */
    private void loadBlockImages() {
        blockImages = new Image[26];
        for (int i = 0; i < blockImages.length; i++) {
            try (InputStream is = getClass().getResourceAsStream("/WATVA/Background/Block" + i + ".png")) {
                if (is != null) {
                    Image original = ImageIO.read(is);
                    if (original != null) {
                        blockImages[i] = original.getScaledInstance(
                                GamePanel.BLOCK_SIZE,
                                GamePanel.BLOCK_SIZE,
                                Image.SCALE_SMOOTH
                        );
                    }
                }
            } catch (Exception e) {
                System.err.println("Cannot load Block" + i + ".png - using empty block.");
            }

            if (blockImages[i] == null) {
                blockImages[i] = new java.awt.image.BufferedImage(
                        GamePanel.BLOCK_SIZE, GamePanel.BLOCK_SIZE,
                        java.awt.image.BufferedImage.TYPE_INT_ARGB
                );
            }
        }
    }

    /**
     * Main rendering method that draws all game elements.
     */
    public void render(Graphics g, Player player, CopyOnWriteArrayList<Enemy> enemies,
                       CopyOnWriteArrayList<PlayerProjectile> playerProjectiles,
                       boolean gameOver, boolean isPaused, boolean abilityPanelVisible,
                       boolean upgradePanelVisible, int killCount, DamageNumberManager damageManager,
                       CrystalExplosion crystalExplosion, boolean menuVisible) {

        updateCamera(player);
        Graphics2D g2d = (Graphics2D) g;

        g2d.translate(-GameLogic.cameraX, -GameLogic.cameraY);

        drawBackground(g2d, player);
        drawWalls(g2d);
        drawEnemies(g2d, enemies);
        damageManager.draw(g);
        drawArrows(g2d, playerProjectiles);
        drawBossEnemies(g2d, enemies);
        drawUI(g2d, player);

        drawPlayer(g2d, player);

        if (crystalExplosion != null) {
            crystalExplosion.draw(g2d, GameLogic.cameraX, GameLogic.cameraY);
        }

        drawWaveProgressBar(g2d, gameOver, isPaused, enemies, killCount,
                crystalExplosion != null, menuVisible);

        g2d.translate(GameLogic.cameraX, GameLogic.cameraY);

        Enemy.drawAllProjectiles(g);

        if (abilityPanelVisible) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, gamePanel.getWidth(), gamePanel.getHeight());
        }
    }


    /**
     * Updates camera position to follow player while staying within map bounds.
     */
    private void updateCamera(Player player) {
        int targetCameraX = player.getX() - GamePanel.CAMERA_WIDTH * 2;
        int targetCameraY = player.getY() - GamePanel.CAMERA_HEIGHT * 2;

        GameLogic.cameraX = targetCameraX;
        GameLogic.cameraY = targetCameraY;
    }

    /**
     * Draws boss enemies with special handling.
     */
    private void drawBossEnemies(Graphics g, CopyOnWriteArrayList<Enemy> enemies) {
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            if (enemy.getType() == Enemy.Type.BUNNY_BOSS || enemy.getType() == Enemy.Type.DARK_MAGE_BOSS) {
                enemy.draw(g);
            }
        }
    }

    /**
     * Draws all dynamic walls
     */
    private void drawWalls(Graphics2D g2d) {
        WallManager wallManager = gamePanel.getGameLogic().getWallManager();
        if (wallManager != null) {
            wallManager.draw(g2d, GameLogic.cameraX, GameLogic.cameraY);
        }
    }

    /**
     * Draws the game background using tile images.
     */
    private void drawBackground(Graphics g, Player player) {
        MapManager mm = gamePanel.getGameLogic().getMapManager();
        if (mm != null) {
            mm.drawBackground(g, player);
        }
    }

    /**
     * Draws the player character.
     */
    private void drawPlayer(Graphics g, Player player) {
        player.getGraphics().draw(g);
    }

    /**
     * Draws all regular enemies.
     */
    private void drawEnemies(Graphics g, CopyOnWriteArrayList<Enemy> enemies) {
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            enemy.draw(g);
        }
    }

    /**
     * Draws all player projectiles (arrows).
     */
    private void drawArrows(Graphics g, CopyOnWriteArrayList<PlayerProjectile> playerProjectiles) {
        for (PlayerProjectile playerProjectile : playerProjectiles) {
            playerProjectile.draw(g);
        }
    }

    /**
     * Draws the game UI including wave number and coin count.
     */
    private void drawUI(Graphics g, Player player) {
        Graphics2D g2d = (Graphics2D) g;
        int fontSize = Game.scale(48);
        pixelPurlFont = pixelPurlFont.deriveFont((float)fontSize);
        g2d.setFont(pixelPurlFont);
        drawOutlinedText(g2d, "Wave: " + GamePanel.getWaveNumber(),
                Game.scale(20) + GameLogic.cameraX,
                Game.scale(40) + GameLogic.cameraY);
        drawOutlinedText(g2d, "Coins: " + player.getCoins(),
                Game.scale(10) + GameLogic.cameraX,
                Game.scale(80) + GameLogic.cameraY);
    }

    /**
     * Draws text with an outline effect for better visibility.
     */
    private void drawOutlinedText(Graphics2D g2d, String text, int x, int y) {
        int outlineSize = Game.scale(3);

        g2d.setColor(Color.BLACK);
        g2d.drawString(text, x - outlineSize, y);
        g2d.drawString(text, x + outlineSize, y);
        g2d.drawString(text, x, y - outlineSize);
        g2d.drawString(text, x, y + outlineSize);

        g2d.setColor(Color.WHITE);
        g2d.drawString(text, x, y);
    }

    /**
     * Draws the wave progress bar showing kill progress toward next wave.
     * Hidden when menu is visible, during explosion, or when paused.
     */
    private void drawWaveProgressBar(Graphics2D g2d, boolean gameOver, boolean isPaused,
                                     CopyOnWriteArrayList<Enemy> enemies, int killCount,
                                     boolean waveCompletionActive, boolean menuVisible) {
        if (gameOver || isPaused || enemies.isEmpty() || gamePanel.getWaveNumber() % 10 == 0
                || waveCompletionActive || menuVisible) return;

        int barWidth = Game.scale(390);
        int barHeight = Game.scale(30);

        int x = (GamePanel.PANEL_WIDTH / 2) - (barWidth / 2) + GameLogic.cameraX;
        int y = Game.scale(60) + GameLogic.cameraY;

        int maxKills = 50 * gamePanel.getWaveNumber();
        float progress = Math.min((float) killCount / maxKills, 1.0f);
        int filledWidth = (int) (barWidth * progress);

        g2d.setColor(new Color(50, 50, 50, 180));
        g2d.fillRoundRect(x, y, barWidth, barHeight, Game.scale(15), Game.scale(15));

        if (filledWidth > 0) {
            GradientPaint gradient = new GradientPaint(x, y, Color.BLUE, x + filledWidth, y, Color.CYAN);
            g2d.setPaint(gradient);
            g2d.fillRoundRect(x, y, filledWidth, barHeight, Game.scale(15), Game.scale(15));
        }

        g2d.setColor(Color.BLACK);
        g2d.drawRoundRect(x, y, barWidth, barHeight, Game.scale(15), Game.scale(15));

        g2d.setFont(pixelPurlFont.deriveFont((float)Game.scale(16)));
        g2d.setColor(Color.WHITE);
        String text = "Wave Progress: " + killCount + " / " + maxKills;
        int textWidth = g2d.getFontMetrics().stringWidth(text);
        g2d.drawString(text, x + (barWidth - textWidth) / 2, y + (barHeight / 2) + Game.scale(6));
    }
}