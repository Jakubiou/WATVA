package Logic;

import Bosses.BunnyBoss;
import Bosses.DarkMageBoss;
import Core.Game;
import Enemies.Enemy;
import Enemies.Slime;
import UI.GamePanel;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Handles enemy spawning logic in the game.
 */
public class SpawningEnemies {

    private static final int SPAWN_DISTANCE_FROM_CAMERA = Game.scale(500);
    private static final int MAX_ENEMY_DISTANCE = Game.scale(1800);

    private GamePanel gamePanel;
    private CopyOnWriteArrayList<Enemy> enemies;
    private ExecutorService spawnExecutor;
    private volatile boolean stopSpawning = false;
    private volatile boolean pauseSpawning = false;

    public SpawningEnemies(GamePanel gamePanel, CopyOnWriteArrayList<Enemy> enemies) {
        this.gamePanel = gamePanel;
        this.enemies = enemies;
        this.spawnExecutor = Executors.newCachedThreadPool();
    }

    /**
     * Starts spawning enemies of different types with specified rates.
     */
    public void spawnEnemies(int normalPerSecond, int giantPerSecond, int smallPerSecond,
                             int shootingPerSecond, int slimePerSecond) {
        stopSpawning = false;
        pauseSpawning = false;

        System.out.println("Spawning enemies - Normal: " + normalPerSecond + ", Giant: " + giantPerSecond +
                ", Small: " + smallPerSecond + ", Shooting: " + shootingPerSecond +
                ", Slime: " + slimePerSecond);

        if (normalPerSecond > 0) {
            spawnEnemyType(normalPerSecond, Enemy.Type.NORMAL, 5 * gamePanel.getWaveNumber());
        }
        if (giantPerSecond > 0) {
            spawnEnemyType(giantPerSecond, Enemy.Type.GIANT, 15 * gamePanel.getWaveNumber());
        }
        if (smallPerSecond > 0) {
            spawnEnemyType(smallPerSecond, Enemy.Type.SMALL, 3 * gamePanel.getWaveNumber());
        }
        if (shootingPerSecond > 0) {
            spawnEnemyType(shootingPerSecond, Enemy.Type.SHOOTING, 4 * gamePanel.getWaveNumber());
        }
        if (slimePerSecond > 0) {
            spawnEnemyType(slimePerSecond, Enemy.Type.SLIME, 4 * gamePanel.getWaveNumber());
        }
    }

    /**
     * Spawns a specific enemy type at given rate.
     */
    private void spawnEnemyType(int rate, Enemy.Type type, int hp) {
        if (rate <= 0) return;

        long spawnInterval = 1000 / rate;

        spawnExecutor.execute(() -> {
            while (!stopSpawning) {
                if (!pauseSpawning) {
                    Point spawnPoint = getSpawnPointOutsideCamera();
                    if (spawnPoint != null) {
                        if (type == Enemy.Type.SLIME) {
                            enemies.add(new Slime(spawnPoint.x, spawnPoint.y, hp));
                        } else {
                            enemies.add(new Enemy(spawnPoint.x, spawnPoint.y, hp, type));
                        }
                    }
                }

                try {
                    TimeUnit.MILLISECONDS.sleep(spawnInterval);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
    }

    public void pauseSpawning() {
        pauseSpawning = true;
    }

    public void resumeSpawning() {
        pauseSpawning = false;
    }

    public void stopCurrentSpawn() {
        stopSpawning = true;
        pauseSpawning = false;
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
     * Spawns a Dark Mage boss enemy outside camera view.
     */
    public void spawnDarkMageBoss() {
        Point spawnPoint = getSpawnPointOutsideCamera();
        System.out.println("Dark Mage Boss spawn point: " + spawnPoint);

        if (spawnPoint != null) {
            int bossHp = 1000 * GameLogic.getWaveNumber();
            System.out.println("Creating Dark Mage Boss with HP: " + bossHp + " at position: (" +
                    spawnPoint.x + ", " + spawnPoint.y + ")");

            DarkMageBoss darkMageBoss = new DarkMageBoss(spawnPoint.x, spawnPoint.y, bossHp);
            enemies.add(darkMageBoss);

            System.out.println("Dark Mage Boss added! Total enemies: " + enemies.size());
        } else {
            System.err.println("ERROR: Could not find spawn point for Dark Mage Boss!");
        }
    }

    /**
     * Spawns a Bunny boss enemy outside camera view.
     */
    public void spawnBunnyBoss() {
        Point spawnPoint = getSpawnPointOutsideCamera();
        System.out.println("Bunny Boss spawn point: " + spawnPoint);

        if (spawnPoint != null) {
            int bossHp = 200 * GameLogic.getWaveNumber();
            System.out.println("Creating Bunny Boss with HP: " + bossHp + " at position: (" +
                    spawnPoint.x + ", " + spawnPoint.y + ")");

            BunnyBoss bunnyBoss = new BunnyBoss(spawnPoint.x, spawnPoint.y, bossHp);
            enemies.add(bunnyBoss);

            System.out.println("Bunny Boss added! Total enemies: " + enemies.size());
        } else {
            System.err.println("ERROR: Could not find spawn point for Bunny Boss!");
        }
    }

    /**
     * Gets a random spawn point OUTSIDE the camera view.
     */
    private Point getSpawnPointOutsideCamera() {
        ArrayList<Point> spawnPoints = new ArrayList<>();

        int cameraX = gamePanel.getCameraX();
        int cameraY = gamePanel.getCameraY();

        int cameraLeft = cameraX;
        int cameraRight = cameraX + GamePanel.PANEL_WIDTH;
        int cameraTop = cameraY;
        int cameraBottom = cameraY + GamePanel.PANEL_HEIGHT;

        int spawnLeft = cameraLeft - SPAWN_DISTANCE_FROM_CAMERA;
        int spawnRight = cameraRight + SPAWN_DISTANCE_FROM_CAMERA;
        int spawnTop = cameraTop - SPAWN_DISTANCE_FROM_CAMERA;
        int spawnBottom = cameraBottom + SPAWN_DISTANCE_FROM_CAMERA;

        int blockSize = GamePanel.BLOCK_SIZE;

        for (int y = spawnTop; y <= spawnBottom; y += blockSize) {
            for (int x = spawnLeft; x < cameraLeft; x += blockSize) {
                spawnPoints.add(new Point(x, y));
            }
        }

        for (int y = spawnTop; y <= spawnBottom; y += blockSize) {
            for (int x = cameraRight; x <= spawnRight; x += blockSize) {
                spawnPoints.add(new Point(x, y));
            }
        }

        for (int x = spawnLeft; x <= spawnRight; x += blockSize) {
            for (int y = spawnTop; y < cameraTop; y += blockSize) {
                spawnPoints.add(new Point(x, y));
            }
        }

        for (int x = spawnLeft; x <= spawnRight; x += blockSize) {
            for (int y = cameraBottom; y <= spawnBottom; y += blockSize) {
                spawnPoints.add(new Point(x, y));
            }
        }

        if (!spawnPoints.isEmpty()) {
            Point selectedPoint = spawnPoints.get((int) (Math.random() * spawnPoints.size()));
            System.out.println("Selected spawn point from " + spawnPoints.size() + " available points");
            return selectedPoint;
        }

        System.err.println("WARNING: No spawn points available!");
        return null;
    }

    public void removeDistantEnemies(int playerX, int playerY) {
        enemies.removeIf(enemy -> {
            if (enemy.getType() == Enemy.Type.DARK_MAGE_BOSS ||
                    enemy.getType() == Enemy.Type.BUNNY_BOSS) {
                return false;
            }

            int dx = enemy.getX() - playerX;
            int dy = enemy.getY() - playerY;
            double distance = Math.sqrt(dx * dx + dy * dy);

            return distance > MAX_ENEMY_DISTANCE;
        });
    }
}