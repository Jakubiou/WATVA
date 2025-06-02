package WATVA;

import java.awt.*;
import java.io.File;
import java.io.IOException;
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
        try {
            for (int i = 0; i < blockImages.length; i++) {
                Image original = ImageIO.read(getClass().getResourceAsStream("/WATVA/Background/Block" + i + ".png"));
                blockImages[i] = original.getScaledInstance(
                        GamePanel.BLOCK_SIZE,
                        GamePanel.BLOCK_SIZE,
                        Image.SCALE_SMOOTH
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Main rendering method that draws all game elements.
     *
     * @param g The Graphics context to render to
     * @param player The player character
     * @param enemies List of active enemies
     * @param playerProjectiles List of active player projectiles
     * @param gameOver True if game is in game over state
     * @param isPaused True if game is paused
     * @param abilityPanelVisible True if ability panel is visible
     * @param upgradePanelVisible True if upgrade panel is visible
     * @param killCount Current kill count for wave progress
     */
    public void render(Graphics g, Player player, CopyOnWriteArrayList<Enemy> enemies,
                       CopyOnWriteArrayList<PlayerProjectile> playerProjectiles,
                       boolean gameOver, boolean isPaused, boolean abilityPanelVisible,
                       boolean upgradePanelVisible, int killCount) {

        updateCamera(player);
        Graphics2D g2d = (Graphics2D) g;

        g2d.translate(-GameLogic.cameraX, -GameLogic.cameraY);

        drawBackground(g2d);
        drawEnemies(g2d, enemies);
        drawArrows(g2d, playerProjectiles);
        drawUI(g2d, player);
        drawBossEnemies(g2d,enemies);
        drawPlayer(g2d, player);
        drawWaveProgressBar(g2d, gameOver, isPaused, enemies, killCount);

        g2d.translate(GameLogic.cameraX, GameLogic.cameraY);

        for (Enemy enemy : enemies) {
            if (enemy.getType() == Enemy.Type.SHOOTING) {
                enemy.drawProjectiles(g);
            }
        }

        if (abilityPanelVisible) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, gamePanel.getWidth(), gamePanel.getHeight());
        }
    }

    /**
     * Updates camera position to follow player while staying within map bounds.
     *
     * @param player The player character to follow
     */
    private void updateCamera(Player player) {
        int targetCameraX = player.getX() - GamePanel.CAMERA_WIDTH * 2;
        int targetCameraY = player.getY() - GamePanel.CAMERA_HEIGHT * 2;

        GameLogic.cameraX = Math.max(0, Math.min(targetCameraX, GameLogic.mapWidth * GamePanel.BLOCK_SIZE - GamePanel.PANEL_WIDTH));
        GameLogic.cameraY = Math.max(0, Math.min(targetCameraY, GameLogic.mapHeight * GamePanel.BLOCK_SIZE - GamePanel.PANEL_HEIGHT));
    }

    /**
     * Draws boss enemies with special handling.
     *
     * @param g The Graphics context
     * @param enemies List of enemies including bosses
     */
    private void drawBossEnemies(Graphics g, CopyOnWriteArrayList<Enemy> enemies) {
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            if (enemy.getType() == Enemy.Type.BUNNY_BOSS || enemy.getType() == Enemy.Type.DARK_MAGE_BOSS) {
                if (enemy.isOffScreen()) {
                    enemies.remove(i);
                    i--;
                } else {
                    enemy.draw(g);
                }
            }
        }
    }

    /**
     * Draws the game background using tile images.
     *
     * @param g The Graphics context
     */
    private void drawBackground(Graphics g) {
        for (int y = 0; y < GameLogic.mapHeight; y++) {
            for (int x = 0; x < GameLogic.mapWidth; x++) {
                int blockType = GameLogic.map[y][x];
                Image blockImage = blockImages[blockType];
                g.drawImage(blockImage, x * GamePanel.BLOCK_SIZE, y * GamePanel.BLOCK_SIZE,
                        GamePanel.BLOCK_SIZE, GamePanel.BLOCK_SIZE, null);
            }
        }
    }

    /**
     * Draws the player character.
     *
     * @param g The Graphics context
     * @param player The player character to draw
     */
    private void drawPlayer(Graphics g, Player player) {
        player.getGraphics().draw(g);
    }

    /**
     * Draws all regular enemies.
     *
     * @param g The Graphics context
     * @param enemies List of enemies to draw
     */
    private void drawEnemies(Graphics g, CopyOnWriteArrayList<Enemy> enemies) {
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            if (enemy.isOffScreen()) {
                enemies.remove(i);
                i--;
            } else {
                enemy.draw(g);
            }
        }
    }

    /**
     * Draws all player projectiles (arrows).
     *
     * @param g The Graphics context
     * @param playerProjectiles List of projectiles to draw
     */
    private void drawArrows(Graphics g, CopyOnWriteArrayList<PlayerProjectile> playerProjectiles) {
        for (PlayerProjectile playerProjectile : playerProjectiles) {
            playerProjectile.draw(g);
        }
    }

    /**
     * Draws the game UI including wave number and coin count.
     *
     * @param g The Graphics context
     * @param player The player for coin count
     */
    private void drawUI(Graphics g, Player player) {
        Graphics2D g2d = (Graphics2D) g;
        int fontSize = (int)(48 * Game.getScaleFactor());
        pixelPurlFont = pixelPurlFont.deriveFont((float)fontSize);
        g2d.setFont(pixelPurlFont);
        drawOutlinedText(g2d, "Wave: " + GamePanel.getWaveNumber(), 20 + GameLogic.cameraX, 40 + GameLogic.cameraY);
        drawOutlinedText(g2d, "Coins: " + player.getCoins(), 10 + GameLogic.cameraX, 80 + GameLogic.cameraY);
    }

    /**
     * Draws text with an outline effect for better visibility.
     *
     * @param g2d The Graphics2D context
     * @param text The text to draw
     * @param x The x-coordinate
     * @param y The y-coordinate
     */
    private void drawOutlinedText(Graphics2D g2d, String text, int x, int y) {
        g2d.setColor(Color.BLACK);
        g2d.drawString(text, x - 3, y);
        g2d.drawString(text, x + 3, y);
        g2d.drawString(text, x, y - 3);
        g2d.drawString(text, x, y + 3);

        g2d.setColor(Color.WHITE);
        g2d.drawString(text, x, y);
    }

    /**
     * Draws the wave progress bar showing kill progress toward next wave.
     *
     * @param g2d The Graphics2D context
     * @param gameOver True if game is over (hides bar)
     * @param isPaused True if game is paused (hides bar)
     * @param enemies List of enemies (for empty check)
     * @param killCount Current kills toward next wave
     */
    private void drawWaveProgressBar(Graphics2D g2d, boolean gameOver, boolean isPaused,
                                     CopyOnWriteArrayList<Enemy> enemies, int killCount) {
        if (gameOver || isPaused || enemies.isEmpty()) return;

        int barWidth = (int)(390 * Game.getScaleFactor());
        int barHeight = (int)(30 * Game.getScaleFactor());

        int x = (GamePanel.PANEL_WIDTH / 2) - (barWidth / 2) + GameLogic.cameraX;
        int y = 60 + GameLogic.cameraY;

        int maxKills = GamePanel.getWaveNumber() * 20;
        float progress = Math.min((float) killCount / maxKills, 1.0f);
        int filledWidth = (int) (barWidth * progress);

        g2d.setColor(new Color(50, 50, 50, 180));
        g2d.fillRoundRect(x, y, barWidth, barHeight, 15, 15);

        if (filledWidth > 0) {
            GradientPaint gradient = new GradientPaint(
                    x, y, Color.BLUE,
                    x + filledWidth, y, Color.CYAN
            );
            g2d.setPaint(gradient);
            g2d.fillRoundRect(x, y, filledWidth, barHeight, 15, 15);
        }

        g2d.setColor(Color.BLACK);
        g2d.drawRoundRect(x, y, barWidth, barHeight, 15, 15);

        g2d.setFont(pixelPurlFont.deriveFont((float) (16f * Game.getScaleFactor())));
        g2d.setColor(Color.WHITE);
        String text = "Wave Progress: " + killCount + " / " + maxKills;
        int textWidth = g2d.getFontMetrics().stringWidth(text);
        g2d.drawString(text, x + (barWidth - textWidth) / 2, y + (barHeight / 2) + 6);
    }
}