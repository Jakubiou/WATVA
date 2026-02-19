package Logic;

import Bosses.BunnyBoss;
import Bosses.DarkMageBoss;
import Core.Game;
import Enemies.Enemy;
import Enemies.EnemyProjectile;
import Logic.DamageNumber.DamageNumberManager;
import Logic.Level.LevelData;
import Logic.Level.LevelManager;
import Player.Player;
import Soundtrack.Soundtrack;
import UI.GamePanel;
import Player.PlayerProjectile;

import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.Timer;

public class GameLogic {
    public static int[][] map;
    public static int mapWidth, mapHeight;
    private Player player;
    private CopyOnWriteArrayList<Enemy> enemies;
    private CopyOnWriteArrayList<PlayerProjectile> playerProjectiles;
    private Timer timer;
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
    private WallManager wallManager;
    private boolean isTutorialMode;

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

        timer = new Timer(15, gamePanel);
        waveNumber = 0;
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

        timer.start();
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
        timer.start();
    }

    public void update(DamageNumberManager damageManager) {
        if (!gameOver && !isPaused) {
            damageManager.update();
            updateAttackSpeed();

            boolean isBossWave = false;
            for (Enemy enemy : enemies) {
                if (enemy instanceof DarkMageBoss || enemy instanceof BunnyBoss) {
                    isBossWave = true;
                    break;
                }
            }

            wallManager.update(player, isBossWave);

            player.move(wallManager);

            Enemy.updateAllProjectiles();

            if (crystalExplosion != null) {
                crystalExplosion.update();
                updateProjectiles();

                if (crystalExplosion.isWaveActive()) {
                    destroyEnemiesInWave();
                }

                if (crystalExplosion.isComplete()) {
                    crystalExplosion = null;
                    waveCompletionInProgress = false;

                    if (isTutorialMode) {
                        pauseGame();
                        gamePanel.onWaveComplete();
                    } else if (waveNumber >= 10) {
                        onLevelComplete();
                    } else {
                        pauseGame();
                        gamePanel.onWaveComplete();
                    }
                }

                return;
            }

            if (!waveCompletionInProgress) {
                collisions.checkCollisions();
                gameOver = collisions.isGameOver();
            }

            updateEnemies(damageManager);
            spawningEnemies.removeDistantEnemies(player.getX(), player.getY());

            if (!waveCompletionInProgress) {
                checkWaveCompletion();
            }
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
                    } else if (waveData.bossType == LevelData.BossType.BUNNY_BOSS) {
                        spawningEnemies.spawnBunnyBoss();
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

            boolean isBossWave = (waveData != null && waveData.hasBoss());

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

            int doubleOffset = Game.scale(20);

            int projectilesCreated = 0;

            if (player.isDoubleShotActive() && player.isForwardBackwardShotActive()) {
                playerProjectiles.add(new PlayerProjectile(centerX, centerY, mouseX + doubleOffset, mouseY + doubleOffset, piercingLevel, fireLevel, hasSlow));
                playerProjectiles.add(new PlayerProjectile(centerX, centerY, mouseX - doubleOffset, mouseY - doubleOffset, piercingLevel, fireLevel, hasSlow));
                playerProjectiles.add(new PlayerProjectile(centerX, centerY, centerX - (mouseX - centerX), centerY - (mouseY - centerY), piercingLevel, fireLevel, hasSlow));
                projectilesCreated = 3;
            } else if (player.isDoubleShotActive()) {
                playerProjectiles.add(new PlayerProjectile(centerX, centerY, mouseX + doubleOffset, mouseY + doubleOffset, piercingLevel, fireLevel, hasSlow));
                playerProjectiles.add(new PlayerProjectile(centerX, centerY, mouseX - doubleOffset, mouseY - doubleOffset, piercingLevel, fireLevel, hasSlow));
                projectilesCreated = 2;
            } else if (player.isForwardBackwardShotActive()) {
                playerProjectiles.add(new PlayerProjectile(centerX, centerY, mouseX, mouseY, piercingLevel, fireLevel, hasSlow));
                playerProjectiles.add(new PlayerProjectile(centerX, centerY, centerX - (mouseX - centerX), centerY - (mouseY - centerY), piercingLevel, fireLevel, hasSlow));
                projectilesCreated = 2;
            } else {
                playerProjectiles.add(new PlayerProjectile(centerX, centerY, mouseX, mouseY, piercingLevel, fireLevel, hasSlow));
                projectilesCreated = 1;
            }

            if (isTutorialMode && gamePanel != null) {
                for (int i = 0; i < projectilesCreated; i++) {
                    gamePanel.onTutorialProjectileFired();
                }
            }
        }
    }

    public void pauseGame() {
        timer.stop();
        isPaused = true;
        backgroundMusic.stop();
        spawningEnemies.pauseSpawning();
    }

    public void resumeGame() {
        timer.start();
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
}