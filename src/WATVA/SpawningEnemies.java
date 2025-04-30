package WATVA;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SpawningEnemies {
    private static final int EDGE_OFFSET = 1;
    private GamePanel gamePanel;
    private CopyOnWriteArrayList<Enemy> enemies;
    private ExecutorService spawnExecutor;
    private volatile boolean stopSpawning = false;

    public SpawningEnemies(GamePanel gamePanel, CopyOnWriteArrayList<Enemy> enemies) {
        this.gamePanel = gamePanel;
        this.enemies = enemies;
        this.spawnExecutor = Executors.newSingleThreadExecutor();
    }

    public void spawnEnemies(int normalPerSecond, int giantPerSecond, int smallPerSecond, int shootingPerSecond, int slimePerSecond) {
        stopSpawning = false;

        spawnEnemyType(normalPerSecond, Enemy.Type.NORMAL, 10);
        spawnEnemyType(giantPerSecond, Enemy.Type.GIANT, 25);
        spawnEnemyType(smallPerSecond, Enemy.Type.SMALL, 5);
        spawnEnemyType(shootingPerSecond, Enemy.Type.SHOOTING, 20);
        spawnEnemyType(slimePerSecond, Enemy.Type.SLIME, 8);
    }

    private void spawnEnemyType(int rate, Enemy.Type type, int hp) {
        if (rate <= 0) return;

        spawnExecutor.execute(() -> {
            while (!stopSpawning) {
                Point spawnPoint = getSpawnPointBehindCamera();
                if (spawnPoint != null) {
                    if (type == Enemy.Type.SLIME) {
                        enemies.add(new Slime(spawnPoint.x, spawnPoint.y, hp));
                    } else {
                        enemies.add(new Enemy(spawnPoint.x, spawnPoint.y, hp, type));
                    }
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(1000 / rate);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
    }

    public void stopCurrentSpawn() {
        stopSpawning = true;
        spawnExecutor.shutdownNow();
        try {
            if (!spawnExecutor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                spawnExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            spawnExecutor.shutdownNow();
        }
        spawnExecutor = Executors.newCachedThreadPool();
    }

    public void spawnDarkMageBoss() {
        Point spawnPoint = getSpawnPointBehindCamera();
        if (spawnPoint != null) {
            DarkMageBoss darkMageBoss = new DarkMageBoss(spawnPoint.x, spawnPoint.y, 500);
            enemies.add(darkMageBoss);
        }
    }
    public void spawnBunnyBoss() {
        Point spawnPoint = getSpawnPointBehindCamera();
        if (spawnPoint != null) {
            BunnyBoss bunnyBoss = new BunnyBoss(spawnPoint.x, spawnPoint.y, 500);
            enemies.add(bunnyBoss);
        }
    }


    private ArrayList<Point> getAvailableEdgeBlocks() {
        ArrayList<Point> availableBlocks = new ArrayList<>();

        Player player = gamePanel.getPlayer();
        int cameraX = gamePanel.getCameraX();
        int cameraY = gamePanel.getCameraY();

        int leftBound = cameraX - GamePanel.BLOCK_SIZE;
        int rightBound = cameraX + GamePanel.PANEL_WIDTH + GamePanel.BLOCK_SIZE * 2;
        int topBound = cameraY - GamePanel.BLOCK_SIZE * 2;
        int bottomBound = cameraY + GamePanel.PANEL_HEIGHT + GamePanel.BLOCK_SIZE;

        for (int y = EDGE_OFFSET; y < GamePanel.mapHeight - EDGE_OFFSET; y++) {
            for (int x = EDGE_OFFSET; x < GamePanel.mapWidth - EDGE_OFFSET; x++) {
                int worldX = x * GamePanel.BLOCK_SIZE;
                int worldY = y * GamePanel.BLOCK_SIZE;

                boolean justOutsideCamera =
                        (worldX >= leftBound && worldX <= rightBound &&
                                worldY >= topBound && worldY <= bottomBound &&
                                (worldX < cameraX || worldX > cameraX + GamePanel.PANEL_WIDTH ||
                                        worldY < cameraY || worldY > cameraY + GamePanel.PANEL_HEIGHT));

                if (justOutsideCamera && isBlockAvailable(x, y)) {
                    availableBlocks.add(new Point(worldX, worldY));
                }
            }
        }
        return availableBlocks;
    }

    private Point getSpawnPointBehindCamera() {
        ArrayList<Point> availableBlocks = getAvailableEdgeBlocks();

        if (!availableBlocks.isEmpty()) {
            return availableBlocks.get((int) (Math.random() * availableBlocks.size()));
        }
        return null;
    }

    private boolean isBlockAvailable(int x, int y) {
        Rectangle blockRect = new Rectangle(x * GamePanel.BLOCK_SIZE, y * GamePanel.BLOCK_SIZE, GamePanel.BLOCK_SIZE, GamePanel.BLOCK_SIZE);
        for (Enemy enemy : enemies) {
            if (blockRect.intersects(enemy.getCollider())) {
                return false;
            }
        }
        return true;
    }
}
