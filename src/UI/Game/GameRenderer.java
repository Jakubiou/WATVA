package UI.Game;

import Core.Game;
import Enemies.Enemy;
import Logic.CrystalExplosion;
import Logic.DamageNumber.DamageNumberManager;
import Logic.GameLogic;
import Logic.World.MapManager;
import Logic.World.WallManager;
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
    private Font uiFontLarge;
    private Font uiFontSmall;
    private static final Color COLOR_OVERLAY = new Color(0, 0, 0, 150);
    private static final Color COLOR_BAR_BG  = new Color(50, 50, 50, 180);

    /**
     * Creates a new GameRenderer with references to game panel and font.
     *
     * @param gamePanel The game panel to render to
     * @param pixelPurlFont The custom font to use for UI elements
     */
    public GameRenderer(GamePanel gamePanel, Font pixelPurlFont) {
        this.gamePanel = gamePanel;
        this.pixelPurlFont = pixelPurlFont;
        this.uiFontLarge = pixelPurlFont.deriveFont((float) Game.scale(48));
        this.uiFontSmall  = pixelPurlFont.deriveFont((float) Game.scale(16));
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
                    java.awt.image.BufferedImage original = ImageIO.read(is);
                    if (original != null) {
                        java.awt.image.BufferedImage scaled = new java.awt.image.BufferedImage(
                                GamePanel.BLOCK_SIZE, GamePanel.BLOCK_SIZE,
                                java.awt.image.BufferedImage.TYPE_INT_RGB);
                        java.awt.Graphics2D sg = scaled.createGraphics();
                        sg.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                                java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                        sg.drawImage(original, 0, 0, GamePanel.BLOCK_SIZE, GamePanel.BLOCK_SIZE, null);
                        sg.dispose();
                        blockImages[i] = scaled;
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
     * Core.Main rendering method that draws all game elements.
     */
    public void render(Graphics g, Player player, CopyOnWriteArrayList<Enemy> enemies,
                       CopyOnWriteArrayList<PlayerProjectile> playerProjectiles,
                       boolean gameOver, boolean isPaused, boolean abilityPanelVisible,
                       boolean upgradePanelVisible, int killCount, DamageNumberManager damageManager,
                       CrystalExplosion crystalExplosion, boolean menuVisible) {

        updateCamera(player);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                java.awt.RenderingHints.VALUE_RENDER_SPEED);

        g2d.translate(-GameLogic.cameraX, -GameLogic.cameraY);

        Logic.PerformanceMonitor.begin("render_background");
        drawBackground(g2d, player);
        Logic.PerformanceMonitor.end("render_background");

        Logic.PerformanceMonitor.begin("render_walls");
        drawWalls(g2d);
        Logic.PerformanceMonitor.end("render_walls");

        Logic.PerformanceMonitor.begin("render_enemies");
        drawEnemies(g2d, enemies);
        Logic.PerformanceMonitor.end("render_enemies");

        Logic.PerformanceMonitor.begin("render_dmgNumbers");
        damageManager.draw(g);
        Logic.PerformanceMonitor.end("render_dmgNumbers");

        Logic.PerformanceMonitor.begin("render_arrows");
        drawArrows(g2d, playerProjectiles);
        Logic.PerformanceMonitor.end("render_arrows");

        Logic.PerformanceMonitor.begin("render_bosses");
        drawBossEnemies(g2d, enemies);
        Logic.PerformanceMonitor.end("render_bosses");

        Logic.PerformanceMonitor.begin("render_ui");
        drawUI(g2d, player);
        Logic.PerformanceMonitor.end("render_ui");

        Logic.PerformanceMonitor.begin("render_player");
        drawPlayer(g2d, player);
        Logic.PerformanceMonitor.end("render_player");

        Pets.PetManager pm = gamePanel.getGameLogic().getPetManager();
        if (pm != null) {
            Logic.PerformanceMonitor.begin("render_pets");
            pm.draw(g2d);
            Logic.PerformanceMonitor.end("render_pets");
        }

        if (crystalExplosion != null) {
            crystalExplosion.draw(g2d, GameLogic.cameraX, GameLogic.cameraY);
        }

        drawWaveProgressBar(g2d, gameOver, isPaused, enemies, killCount,
                crystalExplosion != null, menuVisible);

        g2d.translate(GameLogic.cameraX, GameLogic.cameraY);

        Logic.PerformanceMonitor.begin("render_enemyProjectiles");
        Enemy.drawAllProjectiles(g);
        Logic.PerformanceMonitor.end("render_enemyProjectiles");

        if (abilityPanelVisible) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, gamePanel.getWidth(), gamePanel.getHeight());
        }
    }


    /**
     * Updates camera position to follow player while staying within map bounds.
     */
    private void updateCamera(Player player) {
        GameLogic.cameraX = player.getX() - GamePanel.CAMERA_WIDTH * 2;
        GameLogic.cameraY = player.getY() - GamePanel.CAMERA_HEIGHT * 2;
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
     * Draws all regular enemies. Skips enemies outside the screen (frustum culling).
     */
    private void drawEnemies(Graphics g, CopyOnWriteArrayList<Enemy> enemies) {
        int camX = GameLogic.cameraX;
        int camY = GameLogic.cameraY;
        int margin = Game.scale(128);
        int screenRight  = camX + GamePanel.PANEL_WIDTH  + margin;
        int screenBottom = camY + GamePanel.PANEL_HEIGHT + margin;

        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            if (enemy.getType() == Enemy.Type.DARK_MAGE_BOSS || enemy.getType() == Enemy.Type.BUNNY_BOSS) continue;

            int ex = enemy.getX();
            int ey = enemy.getY();
            int ew = enemy.getWidth();
            int eh = enemy.getHeight();

            if (ex + ew < camX - margin || ex > screenRight ||
                    ey + eh < camY - margin || ey > screenBottom) continue;

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
        g2d.setFont(uiFontLarge);
        drawOutlinedText(g2d, "Wave: " + GamePanel.getWaveNumber(),
                Game.scale(20) + GameLogic.cameraX,
                Game.scale(40) + GameLogic.cameraY);
        drawOutlinedText(g2d, "Coins: " + player.getCoins(),
                Game.scale(10) + GameLogic.cameraX,
                Game.scale(80) + GameLogic.cameraY);

        if (UI.SettingsPanel.isShowFps()) {
            g2d.setFont(uiFontSmall);
            int fps = gamePanel.getCurrentFps();
            Color fpsColor = fps >= 55 ? new Color(0, 220, 0) : fps >= 30 ? Color.YELLOW : Color.RED;
            g2d.setColor(Color.BLACK);
            g2d.drawString("FPS: " + fps, Game.scale(22) + GameLogic.cameraX, Game.scale(115) + GameLogic.cameraY);
            g2d.setColor(fpsColor);
            g2d.drawString("FPS: " + fps, Game.scale(20) + GameLogic.cameraX, Game.scale(113) + GameLogic.cameraY);
        }
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
        if (gameOver || isPaused || enemies.isEmpty() || waveCompletionActive || menuVisible) return;

        boolean isBossWave = false;
        for (Enemy e : enemies) {
            if (e.getType() == Enemy.Type.DARK_MAGE_BOSS || e.getType() == Enemy.Type.BUNNY_BOSS) {
                isBossWave = true;
                break;
            }
        }
        if (isBossWave) return;

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

        g2d.setFont(uiFontSmall);
        g2d.setColor(Color.WHITE);
        String text = "Wave Progress: " + killCount + " / " + maxKills;
        int textWidth = g2d.getFontMetrics().stringWidth(text);
        g2d.drawString(text, x + (barWidth - textWidth) / 2, y + (barHeight / 2) + Game.scale(6));
    }
}