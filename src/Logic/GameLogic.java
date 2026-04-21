package Logic;

import Bosses.BunnyBoss;
import Bosses.DarkMageBoss;
import Core.Game;
import Enemies.Enemy;
import Enemies.EnemyProjectile;
import Logic.DamageNumber.DamageNumberManager;
import Logic.Level.LevelData;
import Logic.Level.LevelManager;
import Logic.World.MapManager;
import Logic.World.WallManager;
import Pets.PetInventory;
import Pets.PetManager;
import Player.Player;
import Soundtrack.Soundtrack;
import UI.Game.GamePanel;
import Player.PlayerProjectile;

import java.util.concurrent.CopyOnWriteArrayList;

public class GameLogic {
    public static int[][] map;
    public static int mapWidth, mapHeight;
    private Player player;
    private CopyOnWriteArrayList<Enemy> enemies;
    private CopyOnWriteArrayList<PlayerProjectile> playerProjectiles;
    private Thread gameLoopThread;
    private volatile boolean running = false;
    private boolean gameOver = false;
    private boolean isPaused = false;
    private static int waveNumber = 0;
    private SpawningEnemies spawningEnemies;
    private Collisions collisions;
    public static int cameraX, cameraY;
    private static int killCount;
    public static Soundtrack backgroundMusic;
    private long lastShotTime = 0;
    private long attackSpeedInterval = 200;
    private GamePanel gamePanel;
    private MapManager mapManager;
    private LevelManager levelManager;
    private CrystalExplosion crystalExplosion;
    private boolean waveCompletionInProgress = false;
    private boolean isBossWave = false;  // cached, updated in nextWave()
    private WallManager wallManager;
    private boolean isTutorialMode;
    private PetInventory petInventory;
    private PetManager petManager;

    public GameLogic(GamePanel gamePanel, Player player, DamageNumberManager damageManager, boolean tutorialMode) {
        this.gamePanel = gamePanel;
        this.player = player;
        this.isTutorialMode = tutorialMode;

        if (!tutorialMode) {
            this.levelManager = new LevelManager();
        }

        this.wallManager = new WallManager();

        backgroundMusic = new Soundtrack("/WATVA/Music/MainSong.wav");
        backgroundMusic.playLoop();

        mapManager = new MapManager("Map1.txt");

        mapWidth = mapManager.getBaseWidth();
        mapHeight = mapManager.getBaseHeight();
        initializeGame(damageManager);
    }

    private void initializeGame(DamageNumberManager damageManager) {
        if (player == null) {
            player = new Player(mapWidth * GamePanel.BLOCK_SIZE / 2, mapHeight * GamePanel.BLOCK_SIZE / 2, 100);
            //player.saveState("player_save.dat");
        } else {
            if (!isTutorialMode) {
                loadPlayerStatus();
            }
            player.setX(mapWidth * GamePanel.BLOCK_SIZE / 2);
            player.setY(mapHeight * GamePanel.BLOCK_SIZE / 2);
        }

        updateAttackSpeed();

        enemies = new CopyOnWriteArrayList<>();
        playerProjectiles = new CopyOnWriteArrayList<>();
        collisions = new Collisions(player, enemies, playerProjectiles, damageManager, wallManager, gamePanel);
        EnemyProjectile.setWallManager(wallManager);
        DarkMageBoss.setWallManager(wallManager);
        spawningEnemies = new SpawningEnemies(gamePanel, enemies);
        spawningEnemies.setPlayerReference(player);

        // Pet system
        petInventory = PetInventory.load();
        petManager = new PetManager(petInventory, player);

        gameLoopThread = null;
        waveNumber = 0;
    }

    private void startGameLoop() {
        if (running) return;
        running = true;
        gameLoopThread = new Thread(() -> {
            final long TARGET_NS = 1_000_000_000L / 60; // 60 FPS
            long lastTime = System.nanoTime();
            // Windows timer fix – 1ms sleep granularity
            try { Thread.sleep(0, 1); } catch (InterruptedException ignored) {}
            while (running) {
                long now = System.nanoTime();
                long elapsed = now - lastTime;
                if (elapsed >= TARGET_NS) {
                    lastTime = now;
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        gamePanel.actionPerformed(null);
                    });
                } else {
                    long sleepNs = TARGET_NS - elapsed - 500_000; // -0.5ms margin
                    if (sleepNs > 0) {
                        try {
                            Thread.sleep(sleepNs / 1_000_000, (int)(sleepNs % 1_000_000));
                        } catch (InterruptedException e) {
                            break;
                        }
                    } else {
                        Thread.yield();
                    }
                }
            }
        }, "GameLoop");
        gameLoopThread.setDaemon(true);
        gameLoopThread.setPriority(Thread.MAX_PRIORITY);
        gameLoopThread.start();
    }

    private void stopGameLoop() {
        running = false;
        if (gameLoopThread != null) {
            gameLoopThread.interrupt();
            gameLoopThread = null;
        }
    }

    public void startTutorial() {
        waveNumber = 1;
        killCount = 0;
        gameOver = false;
        waveCompletionInProgress = false;
        crystalExplosion = null;
        wallManager.clearWalls();

        player.setX(mapWidth * GamePanel.BLOCK_SIZE / 2);
        player.setY(mapHeight * GamePanel.BLOCK_SIZE / 2);

        enemies.clear();
        playerProjectiles.clear();

        spawningEnemies.spawnTutorialEnemies(10);

        startGameLoop();
        resumeGame();
    }

    public void startLevel(int levelNumber) {
        if (isTutorialMode) return;

        levelManager.setCurrentLevel(levelNumber);
        waveNumber = 0;
        killCount = 0;
        gameOver = false;
        waveCompletionInProgress = false;
        crystalExplosion = null;
        wallManager.clearWalls();

        player.setX(mapWidth * GamePanel.BLOCK_SIZE / 2);
        player.setY(mapHeight * GamePanel.BLOCK_SIZE / 2);

        enemies.clear();
        playerProjectiles.clear();

        nextWave();
        startGameLoop();
    }

    public void update(DamageNumberManager damageManager) {
        if (!gameOver && !isPaused) {
            PerformanceMonitor.begin("damageNumbers");
            damageManager.update();
            PerformanceMonitor.end("damageNumbers");

            updateAttackSpeed();

            // isBossWave: nepočítej každý frame iterací – cached v nextWave()
            PerformanceMonitor.begin("wallUpdate");
            wallManager.update(player, isBossWave);
            PerformanceMonitor.end("wallUpdate");

            PerformanceMonitor.begin("playerMove");
            player.move(wallManager);
            PerformanceMonitor.end("playerMove");

            PerformanceMonitor.begin("enemyProjectiles");
            Enemy.updateAllProjectiles();
            PerformanceMonitor.end("enemyProjectiles");

            PerformanceMonitor.begin("updateEnemies");
            updateEnemies(damageManager);
            PerformanceMonitor.end("updateEnemies");

            PerformanceMonitor.begin("petManager");
            if (petManager != null) petManager.update(enemies, damageManager, wallManager);
            PerformanceMonitor.end("petManager");

            if (crystalExplosion != null) {
                crystalExplosion.update();
                updateProjectiles();
                if (crystalExplosion.isWaveActive()) destroyEnemiesInWave();
                if (crystalExplosion.isComplete()) {
                    crystalExplosion = null;
                    waveCompletionInProgress = false;
                    if (isTutorialMode) { pauseGame(); gamePanel.onWaveComplete(); }
                    else if (waveNumber >= 10) { onLevelComplete(); }
                    else { pauseGame(); gamePanel.onWaveComplete(); }
                }
                return;
            }

            if (!waveCompletionInProgress) {
                PerformanceMonitor.begin("collisions");
                collisions.checkCollisions();
                PerformanceMonitor.end("collisions");
                gameOver = collisions.isGameOver();
            }

            PerformanceMonitor.begin("removeDistant");
            spawningEnemies.removeDistantEnemies(player.getX(), player.getY());
            PerformanceMonitor.end("removeDistant");

            if (!waveCompletionInProgress) checkWaveCompletion();
            checkGameOver();
        }
    }

    public void nextWave() {
        spawningEnemies.stopCurrentSpawn();
        enemies.clear();
        waveNumber++;
        playerProjectiles.clear();
        Enemy.clearAllProjectiles();
        killCount = 0;
        waveCompletionInProgress = false;
        crystalExplosion = null;
        isBossWave = false;

        if (isTutorialMode) {
            if (waveNumber <= 3) {
                spawningEnemies.spawnEnemies(waveNumber, 0, 0, 0, 0);
            } else if (waveNumber <= 6) {
                spawningEnemies.spawnEnemies(2, 1, 1, 0, 0);
            } else {
                spawningEnemies.spawnEnemies(3, 1, 2, 1, 1);
            }
        } else {
            LevelData currentLevelData = levelManager.getLevel(levelManager.getCurrentLevel());

            if (waveNumber <= 10 && currentLevelData != null) {
                LevelData.WaveData waveData = currentLevelData.getWave(waveNumber - 1);

                if (waveData != null) {
                    if (waveData.hasBoss()) {
                        wallManager.clearWalls();
                        wallManager.createBossArena(player);
                    } else {
                        wallManager.clearBossArena();
                    }

                    if (waveData.bossType == LevelData.BossType.DARK_MAGE_BOSS) {
                        spawningEnemies.spawnDarkMageBoss();
                        isBossWave = true;
                    } else if (waveData.bossType == LevelData.BossType.BUNNY_BOSS) {
                        spawningEnemies.spawnBunnyBoss();
                        isBossWave = true;
                    } else {
                        spawningEnemies.spawnEnemies(
                                waveData.normalPerSecond,
                                waveData.giantPerSecond,
                                waveData.smallPerSecond,
                                waveData.shootingPerSecond,
                                waveData.slimePerSecond
                        );
                    }
                }
            }
        }

        resumeGame();
    }

    private void destroyEnemiesInWave() {
        if (crystalExplosion == null) return;

        enemies.removeIf(enemy -> {
            if (enemy instanceof DarkMageBoss || enemy instanceof Bosses.BunnyBoss) {
                return false;
            }
            return true;
        });

        Enemy.clearAllProjectiles();
    }

    private void updateProjectiles() {
        playerProjectiles.removeIf(projectile -> projectile.move());
    }

    private void updateEnemies(DamageNumberManager damageManager) {
        for (Enemy enemy : enemies) {
            enemy.update(damageManager);
            if (enemy instanceof DarkMageBoss) {
                DarkMageBoss darkMageBoss = (DarkMageBoss) enemy;
                if (darkMageBoss.isDead()) {
                    enemies.remove(enemy);
                    break;
                } else if (!darkMageBoss.isDying()) {
                    darkMageBoss.updateBossBehavior(player, enemies);
                }
            } else if (enemy instanceof BunnyBoss) {
                BunnyBoss bunnyBoss = (BunnyBoss) enemy;
                if (bunnyBoss.getHp() <= 0) {
                    enemies.remove(enemy);
                    break;
                } else {
                    bunnyBoss.updateBossBehavior(player, enemies);
                }
            } else if (enemy.getType() == Enemy.Type.SHOOTING) {
                enemy.updateProjectiles();
            }
        }
    }

    private void checkWaveCompletion() {
        boolean waveComplete = false;

        if (isTutorialMode) {
            return;
        } else {
            LevelData currentLevelData = levelManager.getLevel(levelManager.getCurrentLevel());
            LevelData.WaveData waveData = null;

            if (currentLevelData != null && waveNumber > 0 && waveNumber <= 10) {
                waveData = currentLevelData.getWave(waveNumber - 1);
            }

            isBossWave = (waveData != null && waveData.hasBoss());

            if (isBossWave) {
                boolean bossAlive = false;
                for (Enemy enemy : enemies) {
                    if (enemy instanceof DarkMageBoss || enemy instanceof BunnyBoss) {
                        bossAlive = true;
                        break;
                    }
                }
                waveComplete = !bossAlive;
            } else {
                int requiredKills = 50 * waveNumber;
                waveComplete = killCount >= requiredKills;
            }
        }

        if (waveComplete && !gameOver && !waveCompletionInProgress) {
            startWaveCompletionEffect();
        }
    }

    private void startWaveCompletionEffect() {
        waveCompletionInProgress = true;
        spawningEnemies.stopCurrentSpawn();
        wallManager.despawnTemporaryWalls();
        crystalExplosion = new CrystalExplosion();
        crystalExplosion.setPlayer(player);
    }

    private void onLevelComplete() {
        pauseGame();
        levelManager.unlockNextLevel();
        gamePanel.onLevelComplete();
    }

    private void checkGameOver() {
        if (player.getHp() <= 0) {
            gameOver = true;
            spawningEnemies.stopCurrentSpawn();
            if (!isTutorialMode) {
                savePlayerCoins();
                player.saveLocation("player_save.dat");
                loadPlayerStatus();
            }
        }
    }

    private void updateAttackSpeed() {
        int baseInterval = 200;
        int speedReduction = player.getAttackSpeed() * 10;
        attackSpeedInterval = Math.max(100, baseInterval - speedReduction);
    }

    public void tryToShoot(int mouseX, int mouseY) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShotTime >= attackSpeedInterval) {
            shoot(mouseX, mouseY);
            lastShotTime = currentTime;
        }
    }

    private void shoot(int mouseX, int mouseY) {
        if (player.isExplosionActive() && player.canUseExplosion()) {
            player.triggerExplosion();
        } else {
            int centerX = player.getX() + Player.WIDTH / 2 - Game.scale(25);
            int centerY = player.getY() + Player.HEIGHT / 2;

            int piercingLevel = player.getPiercingLevel();
            int fireLevel = player.getFireLevel();
            boolean hasSlow = player.hasSlowEnemies();
            int bulletSpeedLevel = player.getBulletSpeedLevel();
            boolean ricochet = player.hasRicochetAbility();

            int doubleOffset = Game.scale(20);

            int projectilesCreated = 0;

            if (player.isDoubleShotActive() && player.isForwardBackwardShotActive()) {
                addProjectile(centerX, centerY, mouseX + doubleOffset, mouseY + doubleOffset, piercingLevel, fireLevel, hasSlow, bulletSpeedLevel, ricochet);
                addProjectile(centerX, centerY, mouseX - doubleOffset, mouseY - doubleOffset, piercingLevel, fireLevel, hasSlow, bulletSpeedLevel, ricochet);
                addProjectile(centerX, centerY, centerX - (mouseX - centerX), centerY - (mouseY - centerY), piercingLevel, fireLevel, hasSlow, bulletSpeedLevel, ricochet);
                projectilesCreated = 3;
            } else if (player.isDoubleShotActive()) {
                addProjectile(centerX, centerY, mouseX + doubleOffset, mouseY + doubleOffset, piercingLevel, fireLevel, hasSlow, bulletSpeedLevel, ricochet);
                addProjectile(centerX, centerY, mouseX - doubleOffset, mouseY - doubleOffset, piercingLevel, fireLevel, hasSlow, bulletSpeedLevel, ricochet);
                projectilesCreated = 2;
            } else if (player.isForwardBackwardShotActive()) {
                addProjectile(centerX, centerY, mouseX, mouseY, piercingLevel, fireLevel, hasSlow, bulletSpeedLevel, ricochet);
                addProjectile(centerX, centerY, centerX - (mouseX - centerX), centerY - (mouseY - centerY), piercingLevel, fireLevel, hasSlow, bulletSpeedLevel, ricochet);
                projectilesCreated = 2;
            } else {
                addProjectile(centerX, centerY, mouseX, mouseY, piercingLevel, fireLevel, hasSlow, bulletSpeedLevel, ricochet);
                projectilesCreated = 1;
            }

            if (isTutorialMode && gamePanel != null) {
                for (int i = 0; i < projectilesCreated; i++) {
                    gamePanel.onTutorialProjectileFired();
                }
            }
        }
    }

    private void addProjectile(int cx, int cy, int tx, int ty, int pierce, int fire, boolean slow, int bulletSpd, boolean ricochet) {
        PlayerProjectile p = new PlayerProjectile(cx, cy, tx, ty, pierce, fire, slow, bulletSpd);
        if (ricochet) p.setCanRicochet(true);
        playerProjectiles.add(p);
    }

    public void pauseGame() {
        stopGameLoop();
        isPaused = true;
        backgroundMusic.stop();
        spawningEnemies.pauseSpawning();
    }

    public void resumeGame() {
        startGameLoop();
        isPaused = false;
        backgroundMusic.playLoop();
        spawningEnemies.resumeSpawning();
    }

    public void stopGame() {
        pauseGame();
        backgroundMusic.stop();
        enemies.clear();
        playerProjectiles.clear();
        Enemy.clearAllProjectiles();
        spawningEnemies.stopCurrentSpawn();
        wallManager.clearWalls();
        collisions = null;
        spawningEnemies = null;
    }

    public static void killCountPlus(){
        killCount++;
    }

    public void savePlayerCoins(){
        if (!isTutorialMode) {
            player.saveCoins("player_save.dat");
        }
    }

    public void loadPlayerStatus() {
        if (isTutorialMode) return;

        try {
            player = Player.loadState("player_save.dat");
            if (player == null) {
                player = new Player(mapWidth * GamePanel.BLOCK_SIZE / 2,
                        mapHeight * GamePanel.BLOCK_SIZE / 2, 100);
            }
        } catch (Exception e) {
            System.err.println("Error loading player, creating new one: " + e.getMessage());
            player = new Player(mapWidth * GamePanel.BLOCK_SIZE / 2,
                    mapHeight * GamePanel.BLOCK_SIZE / 2, 100);
        }
    }

    public Player getPlayer() { return player; }
    public CopyOnWriteArrayList<Enemy> getEnemies() { return enemies; }
    public CopyOnWriteArrayList<PlayerProjectile> getPlayerProjectiles() { return playerProjectiles; }
    public boolean isGameOver() { return gameOver; }
    public boolean isPaused() { return isPaused; }
    public static int getWaveNumber() { return waveNumber; }
    public int getKillCount() { return killCount; }
    public int getCameraX() { return cameraX; }
    public int getCameraY() { return cameraY; }
    public MapManager getMapManager() { return mapManager; }
    public LevelManager getLevelManager() { return levelManager; }
    public CrystalExplosion getCrystalExplosion() { return crystalExplosion; }
    public WallManager getWallManager() { return wallManager; }
    public PetManager getPetManager() { return petManager; }
    public PetInventory getPetInventory() { return petInventory; }
}