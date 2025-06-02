package WATVA;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Handles enemy spawning logic in the game.
 * Manages concurrent spawning of different enemy types with configurable rates.
 * Implements spawn points selection outside player's view.
 */
public class SpawningEnemies {
    private static final int EDGE_OFFSET = 1;
    private GamePanel gamePanel;
    private CopyOnWriteArrayList<Enemy> enemies;
    private ExecutorService spawnExecutor;
    private volatile boolean stopSpawning = false;

    /**
     * Creates a new SpawningEnemies controller.
     *
     * @param gamePanel Reference to the game panel
     * @param enemies Thread-safe list to add spawned enemies to
     */
    public SpawningEnemies(GamePanel gamePanel, CopyOnWriteArrayList<Enemy> enemies) {
        this.gamePanel = gamePanel;
        this.enemies = enemies;
        this.spawnExecutor = Executors.newSingleThreadExecutor();
    }

    /**
     * Starts spawning enemies of different types with specified rates.
     *
     * @param normalPerSecond Spawn rate for normal enemies
     * @param giantPerSecond Spawn rate for giant enemies
     * @param smallPerSecond Spawn rate for small enemies
     * @param shootingPerSecond Spawn rate for shooting enemies
     * @param slimePerSecond Spawn rate for slime enemies
     */
    public void spawnEnemies(int normalPerSecond, int giantPerSecond, int smallPerSecond, int shootingPerSecond, int slimePerSecond) {
        stopSpawning = false;

        spawnEnemyType(normalPerSecond, Enemy.Type.NORMAL, 5 * gamePanel.getWaveNumber());
        spawnEnemyType(giantPerSecond, Enemy.Type.GIANT, 15 * gamePanel.getWaveNumber());
        spawnEnemyType(smallPerSecond, Enemy.Type.SMALL, 3 * gamePanel.getWaveNumber());
        spawnEnemyType(shootingPerSecond, Enemy.Type.SHOOTING, 4 * gamePanel.getWaveNumber());
        spawnEnemyType(slimePerSecond, Enemy.Type.SLIME, 4 * gamePanel.getWaveNumber());
    }

    /**
     * Spawns a specific enemy type at given rate.
     *
     * @param rate Enemies per second to spawn
     * @param type Type of enemy to spawn
     * @param hp Health points for spawned enemies
     */
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

    /**
     * Stops all current spawning activities.
     */
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

    /**
     * Spawns a Dark Mage boss enemy.
     */
    public void spawnDarkMageBoss() {
        Point spawnPoint = getSpawnPointBehindCamera();
        if (spawnPoint != null) {
            DarkMageBoss darkMageBoss = new DarkMageBoss(spawnPoint.x, spawnPoint.y, 100 * GameLogic.getWaveNumber());
            enemies.add(darkMageBoss);
        }
    }

    /**
     * Spawns a Bunny boss enemy.
     */
    public void spawnBunnyBoss() {
        Point spawnPoint = getSpawnPointBehindCamera();
        if (spawnPoint != null) {
            BunnyBoss bunnyBoss = new BunnyBoss(spawnPoint.x, spawnPoint.y, 100 * GameLogic.getWaveNumber());
            enemies.add(bunnyBoss);
        }
    }

    /**
     * Gets all available spawn points at map edges outside camera view.
     *
     * @return List of available spawn points
     */
    private ArrayList<Point> getAvailableEdgeBlocks() {
        ArrayList<Point> availableBlocks = new ArrayList<>();

        Player player = gamePanel.getPlayer();
        int cameraX = gamePanel.getCameraX();
        int cameraY = gamePanel.getCameraY();

        int leftBound = cameraX - GamePanel.BLOCK_SIZE * 6;
        int rightBound = cameraX + GamePanel.PANEL_WIDTH + GamePanel.BLOCK_SIZE * 4;
        int topBound = cameraY - GamePanel.BLOCK_SIZE * 6;
        int bottomBound = cameraY + GamePanel.PANEL_HEIGHT + GamePanel.BLOCK_SIZE *4;

        for (int y = EDGE_OFFSET; y < GameLogic.mapHeight - EDGE_OFFSET; y++) {
            for (int x = EDGE_OFFSET; x < GameLogic.mapWidth - EDGE_OFFSET; x++) {
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

    /**
     * Gets a random spawn point behind camera view.
     *
     * @return Random spawn point or null if none available
     */
    private Point getSpawnPointBehindCamera() {
        ArrayList<Point> availableBlocks = getAvailableEdgeBlocks();

        if (!availableBlocks.isEmpty()) {
            return availableBlocks.get((int) (Math.random() * availableBlocks.size()));
        }
        return null;
    }

    /**
     * Checks if a map block is available for spawning (not occupied by enemies).
     *
     * @param x Block x coordinate
     * @param y Block y coordinate
     * @return true if block is available for spawning
     */
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
