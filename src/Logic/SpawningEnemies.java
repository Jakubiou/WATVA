package Logic;

import Bosses.BunnyBoss;
import Bosses.DarkMageBoss;
import Core.Game;
import Enemies.Enemy;
import Enemies.Slime;
import Player.Player;
import UI.Game.GamePanel;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SpawningEnemies {

    private static final int SPAWN_DISTANCE_FROM_CAMERA = Game.scale(500);
    private static final int MAX_ENEMY_DISTANCE = Game.scale(1800);
    private static final int MIN_DISTANCE_FROM_PLAYER = Game.scale(200);

    private GamePanel gamePanel;
    private CopyOnWriteArrayList<Enemy> enemies;
    private ExecutorService spawnExecutor;
    private volatile boolean stopSpawning = false;
    private volatile boolean pauseSpawning = false;
    private Player playerReference;

    public SpawningEnemies(GamePanel gamePanel, CopyOnWriteArrayList<Enemy> enemies) {
        this.gamePanel = gamePanel;
        this.enemies = enemies;
        this.spawnExecutor = Executors.newCachedThreadPool();
    }

    public void setPlayerReference(Player player) {
        this.playerReference = player;
    }

    public void spawnEnemies(int normalPerSecond, int giantPerSecond, int smallPerSecond,
                             int shootingPerSecond, int slimePerSecond) {
        stopSpawning = false;
        pauseSpawning = false;

        if (normalPerSecond > 0) {
            spawnEnemyType(normalPerSecond, Enemy.Type.NORMAL, 2 * gamePanel.getWaveNumber());
        }
        if (giantPerSecond > 0) {
            spawnEnemyType(giantPerSecond, Enemy.Type.GIANT, 5 * gamePanel.getWaveNumber());
        }
        if (smallPerSecond > 0) {
            spawnEnemyType(smallPerSecond, Enemy.Type.SMALL, gamePanel.getWaveNumber());
        }
        if (shootingPerSecond > 0) {
            spawnEnemyType(shootingPerSecond, Enemy.Type.SHOOTING, gamePanel.getWaveNumber());
        }
        if (slimePerSecond > 0) {
            spawnEnemyType(slimePerSecond, Enemy.Type.SLIME, 2 * gamePanel.getWaveNumber());
        }
    }

    private void spawnEnemyType(int rate, Enemy.Type type, int hp) {
        if (rate <= 0) return;

        long spawnInterval = 1000 / rate;

        spawnExecutor.execute(() -> {
            while (!stopSpawning) {
                if (!pauseSpawning) {
                    Point spawnPoint = getSpawnPointAwayFromPlayer();
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
        spawnExecutor = Executors.newCachedThreadPool();
    }

    public void spawnDarkMageBoss() {
        Point spawnPoint = getSpawnPointForBoss();

        if (spawnPoint != null) {
            int bossHp = 10000 * GameLogic.getWaveNumber();
            DarkMageBoss darkMageBoss = new DarkMageBoss(spawnPoint.x, spawnPoint.y, bossHp);

            Logic.World.WallManager wm = gamePanel.getGameLogic().getWallManager();
            if (wm != null && wm.getArenaCenter() != null) {
                darkMageBoss.setArenaCenter(
                        wm.getArenaCenter().x,
                        wm.getArenaCenter().y,
                        wm.getArenaRadius()
                );
            }

            enemies.add(darkMageBoss);
            System.out.println("Dark Mage Boss added at " + spawnPoint + "! Total enemies: " + enemies.size());
        } else {
            System.err.println("ERROR: Could not find spawn point for Dark Mage Boss!");
        }
    }

    private Point getSpawnPointForBoss() {
        if (playerReference == null) playerReference = gamePanel.getPlayer();
        if (playerReference == null) return getSpawnPointAwayFromPlayer();

        int playerX = playerReference.getX();
        int playerY = playerReference.getY();

        Logic.World.WallManager wm = gamePanel.getGameLogic().getWallManager();

        for (int attempt = 0; attempt < 30; attempt++) {
            double angle = Math.random() * Math.PI * 2;
            int dist = Game.scale(250) + (int)(Math.random() * Game.scale(200));
            int tx = playerX + (int)(Math.cos(angle) * dist);
            int ty = playerY + (int)(Math.sin(angle) * dist);

            if (wm != null) {
                boolean blocked = false;
                int bossSize = Game.scale(128);
                int pad = Game.scale(16);
                if (wm.isWall(tx + pad, ty + pad) ||
                        wm.isWall(tx + bossSize - pad, ty + pad) ||
                        wm.isWall(tx + pad, ty + bossSize - pad) ||
                        wm.isWall(tx + bossSize - pad, ty + bossSize - pad) ||
                        wm.isWall(tx + bossSize/2, ty + bossSize/2)) {
                    blocked = true;
                }
                if (blocked) continue;
            }
            return new Point(tx, ty);
        }
        return new Point(playerX + Game.scale(300), playerY);
    }

    public void spawnBunnyBoss() {
        Point spawnPoint = getSpawnPointForBoss();
        System.out.println("Bunny Boss spawn point: " + spawnPoint);

        if (spawnPoint != null) {
            int bossHp = 6700 * GameLogic.getWaveNumber();
            System.out.println("Creating Bunny Boss with HP: " + bossHp + " at position: (" +
                    spawnPoint.x + ", " + spawnPoint.y + ")");

            BunnyBoss bunnyBoss = new BunnyBoss(spawnPoint.x, spawnPoint.y, bossHp);
            enemies.add(bunnyBoss);

            System.out.println("Bunny Boss added! Total enemies: " + enemies.size());
        } else {
            System.err.println("ERROR: Could not find spawn point for Bunny Boss!");
        }
    }

    private Point getSpawnPointAwayFromPlayer() {
        if (playerReference == null) {
            playerReference = gamePanel.getPlayer();
        }

        if (playerReference == null) {
            return getSpawnPointOutsideCamera();
        }

        int maxAttempts = 20;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            Point candidate = getSpawnPointOutsideCamera();
            if (candidate == null) continue;

            int dx = candidate.x - playerReference.getX();
            int dy = candidate.y - playerReference.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance >= MIN_DISTANCE_FROM_PLAYER) {
                return candidate;
            }
        }

        return getSpawnPointOutsideCamera();
    }

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

    /**
     * Special spawning for tutorial - spawns limited number of enemies total
     * @param totalEnemies Maximum number of enemies to spawn (e.g., 10)
     */
    public void spawnTutorialEnemies(int totalEnemies) {
        stopSpawning = false;
        pauseSpawning = false;

        System.out.println("Tutorial spawning: Will spawn " + totalEnemies + " enemies total");

        spawnExecutor.execute(() -> {
            int enemiesSpawned = 0;

            while (!stopSpawning && enemiesSpawned < totalEnemies) {
                if (!pauseSpawning) {
                    Point spawnPoint = getSpawnPointAwayFromPlayer();
                    if (spawnPoint != null) {
                        enemies.add(new Enemy(spawnPoint.x, spawnPoint.y, 15, Enemy.Type.NORMAL));
                        enemiesSpawned++;
                        System.out.println("Tutorial: Spawned enemy " + enemiesSpawned + "/" + totalEnemies);
                    }
                }

                try {
                    TimeUnit.MILLISECONDS.sleep(3000);
                } catch (InterruptedException e) {
                    return;
                }
            }

            System.out.println("Tutorial spawning complete: " + enemiesSpawned + " enemies spawned");
        });
    }
}