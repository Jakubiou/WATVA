package Logic;

import Bosses.BunnyBoss;
import Bosses.DarkMageBoss;
import Core.Game;
import Enemies.Enemy;
import Logic.DamageNumber.DamageNumberManager;
import Logic.Level.LevelData;
import Logic.Level.LevelManager;
import Player.Player;
import Soundtrack.Soundtrack;
import UI.GamePanel;
import Player.PlayerProjectile;

import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.Timer;

/**
 * The core game logic controller that manages game state, wave progression,
 * collisions, and player/enemy interactions.
 */
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

    public GameLogic(GamePanel gamePanel, Player player, DamageNumberManager damageManager) {
        this.gamePanel = gamePanel;
        this.player = player;
        this.levelManager = new LevelManager();

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
            loadPlayerStatus();
            player.setX(mapWidth * GamePanel.BLOCK_SIZE / 2);
            player.setY(mapHeight * GamePanel.BLOCK_SIZE / 2);
        }

        updateAttackSpeed();

        enemies = new CopyOnWriteArrayList<>();
        playerProjectiles = new CopyOnWriteArrayList<>();
        collisions = new Collisions(player, enemies, playerProjectiles, damageManager);
        spawningEnemies = new SpawningEnemies(gamePanel, enemies);

        timer = new Timer(15, gamePanel);
        waveNumber = 0;
    }

    public void startLevel(int levelNumber) {
        levelManager.setCurrentLevel(levelNumber);
        waveNumber = 0;
        killCount = 0;
        gameOver = false;

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
            player.move();

            collisions.checkCollisions();
            gameOver = collisions.isGameOver();

            updateEnemies(damageManager);
            checkWaveCompletion();
            checkGameOver();
        }
    }

    private void updateEnemies(DamageNumberManager damageManager) {
        for (Enemy enemy : enemies) {
            enemy.update(damageManager);
            if (enemy instanceof DarkMageBoss) {
                DarkMageBoss darkMageBoss = (DarkMageBoss) enemy;

                if (darkMageBoss.isDead()) {
                    enemies.remove(enemy);
                    break;
                }
                else if (!darkMageBoss.isDying()) {
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
        boolean bossExists = false;

        for (Enemy enemy : enemies) {
            if (enemy instanceof DarkMageBoss || enemy instanceof BunnyBoss) {
                bossExists = true;
                break;
            }
        }

        boolean waveComplete = false;

        if (bossExists) {
            waveComplete = true;
            for (Enemy enemy : enemies) {
                if (enemy instanceof DarkMageBoss || enemy instanceof BunnyBoss) {
                    waveComplete = false;
                    break;
                }
            }
        } else {
            waveComplete = killCount >= 50 * waveNumber;
        }

        if (waveComplete && !gameOver) {
            if (waveNumber >= 1) {
                onLevelComplete();
            } else {
                pauseGame();
                gamePanel.onWaveComplete();
            }
        }
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
            savePlayerCoins();
            player.saveLocation("player_save.dat");
            loadPlayerStatus();
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

            if (player.isDoubleShotActive() && player.isForwardBackwardShotActive()) {
                playerProjectiles.add(new PlayerProjectile(centerX, centerY, mouseX + doubleOffset, mouseY + doubleOffset, piercingLevel, fireLevel, hasSlow));
                playerProjectiles.add(new PlayerProjectile(centerX, centerY, mouseX - doubleOffset, mouseY - doubleOffset, piercingLevel, fireLevel, hasSlow));
                playerProjectiles.add(new PlayerProjectile(centerX, centerY, centerX - (mouseX - centerX), centerY - (mouseY - centerY), piercingLevel, fireLevel, hasSlow));
            } else if (player.isDoubleShotActive()) {
                playerProjectiles.add(new PlayerProjectile(centerX, centerY, mouseX + doubleOffset, mouseY + doubleOffset, piercingLevel, fireLevel, hasSlow));
                playerProjectiles.add(new PlayerProjectile(centerX, centerY, mouseX - doubleOffset, mouseY - doubleOffset, piercingLevel, fireLevel, hasSlow));
            } else if (player.isForwardBackwardShotActive()) {
                playerProjectiles.add(new PlayerProjectile(centerX, centerY, mouseX, mouseY, piercingLevel, fireLevel, hasSlow));
                playerProjectiles.add(new PlayerProjectile(centerX, centerY, centerX - (mouseX - centerX), centerY - (mouseY - centerY), piercingLevel, fireLevel, hasSlow));
            } else {
                playerProjectiles.add(new PlayerProjectile(centerX, centerY, mouseX, mouseY, piercingLevel, fireLevel, hasSlow));
            }
        }
    }

    public void nextWave() {
        spawningEnemies.stopCurrentSpawn();
        enemies.clear();
        waveNumber++;
        playerProjectiles.clear();
        killCount = 0;

        LevelData currentLevelData = levelManager.getLevel(levelManager.getCurrentLevel());

        if (waveNumber <= 10 && currentLevelData != null) {
            LevelData.WaveData waveData = currentLevelData.getWave(waveNumber - 1);

            if (waveData != null) {
                System.out.println("Boss type: " + waveData.bossType);
                System.out.println("Enemies: N:" + waveData.normalPerSecond + " G:" + waveData.giantPerSecond +
                        " S:" + waveData.smallPerSecond + " Sh:" + waveData.shootingPerSecond +
                        " Sl:" + waveData.slimePerSecond);

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

                System.out.println("Total enemies after spawn: " + enemies.size());
            } else {
                System.err.println("ERROR: Wave data is NULL for wave " + waveNumber);
            }
        } else {
            System.err.println("ERROR: Invalid wave number or level data is NULL");
        }

        resumeGame();
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
        spawningEnemies.stopCurrentSpawn();
        collisions = null;
        spawningEnemies = null;
    }

    public static void killCountPlus(){
        killCount++;
    }

    public void savePlayerCoins(){
        player.saveCoins("player_save.dat");
    }

    public void loadPlayerStatus() {
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
}