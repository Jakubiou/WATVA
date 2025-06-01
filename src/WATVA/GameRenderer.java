package WATVA;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.imageio.ImageIO;

public class GameRenderer {
    private GamePanel gamePanel;
    private Image[] blockImages;
    private Font pixelPurlFont;

    public GameRenderer(GamePanel gamePanel, Font pixelPurlFont) {
        this.gamePanel = gamePanel;
        this.pixelPurlFont = pixelPurlFont;
        loadBlockImages();
    }

    private void loadBlockImages() {
        blockImages = new Image[26];
        try {
            for (int i = 0; i < blockImages.length; i++) {
                Image original = ImageIO.read(new File("res/watva/background/Block" + i + ".png"));
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

    private void updateCamera(Player player) {
        int targetCameraX = player.getX() - GamePanel.CAMERA_WIDTH * 2;
        int targetCameraY = player.getY() - GamePanel.CAMERA_HEIGHT * 2;

        GameLogic.cameraX = Math.max(0, Math.min(targetCameraX, GameLogic.mapWidth * GamePanel.BLOCK_SIZE - GamePanel.PANEL_WIDTH));
        GameLogic.cameraY = Math.max(0, Math.min(targetCameraY, GameLogic.mapHeight * GamePanel.BLOCK_SIZE - GamePanel.PANEL_HEIGHT));
    }

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

    private void drawPlayer(Graphics g, Player player) {
        player.getGraphics().draw(g);
    }

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

    private void drawArrows(Graphics g, CopyOnWriteArrayList<PlayerProjectile> playerProjectiles) {
        for (PlayerProjectile playerProjectile : playerProjectiles) {
            playerProjectile.draw(g);
        }
    }

    private void drawUI(Graphics g, Player player) {
        Graphics2D g2d = (Graphics2D) g;
        int fontSize = (int)(48 * Game.getScaleFactor());
        pixelPurlFont = pixelPurlFont.deriveFont((float)fontSize);
        g2d.setFont(pixelPurlFont);
        drawOutlinedText(g2d, "Wave: " + GamePanel.getWaveNumber(), 20 + GameLogic.cameraX, 40 + GameLogic.cameraY);
        drawOutlinedText(g2d, "Coins: " + player.getCoins(), 10 + GameLogic.cameraX, 80 + GameLogic.cameraY);
    }

    private void drawOutlinedText(Graphics2D g2d, String text, int x, int y) {
        g2d.setColor(Color.BLACK);
        g2d.drawString(text, x - 3, y);
        g2d.drawString(text, x + 3, y);
        g2d.drawString(text, x, y - 3);
        g2d.drawString(text, x, y + 3);

        g2d.setColor(Color.WHITE);
        g2d.drawString(text, x, y);
    }

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