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
        waveCompletionInProgress = false;
        crystalExplosion = null;

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

            if (crystalExplosion != null) {
                crystalExplosion.update();

                updateProjectiles();

                if (crystalExplosion.isWaveActive()) {
                    destroyEnemiesInWave();
                }

                if (crystalExplosion.isComplete()) {
                    crystalExplosion = null;
                    waveCompletionInProgress = false;

                    if (waveNumber >= 10) {
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

    /**
     * OPTIMIZED: Destroy enemies in wave without checking every frame
     */
    private void destroyEnemiesInWave() {
        if (crystalExplosion == null || crystalExplosion.getPlayer() == null) return;

        Player p = crystalExplosion.getPlayer();
        int playerCenterX = p.getX() + Player.WIDTH / 2;
        int playerCenterY = p.getY() + Player.HEIGHT / 2;
        int radius = crystalExplosion.getWaveRadius();

        enemies.removeIf(enemy -> {
            int dx = enemy.getX() - playerCenterX;
            int dy = enemy.getY() - playerCenterY;
            return (dx * dx + dy * dy) <= (radius * radius);
        });
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
        LevelData currentLevelData = levelManager.getLevel(levelManager.getCurrentLevel());
        LevelData.WaveData waveData = null;

        if (currentLevelData != null && waveNumber > 0 && waveNumber <= 10) {
            waveData = currentLevelData.getWave(waveNumber - 1);
        }

        boolean waveComplete = false;
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

        if (waveComplete && !gameOver && !waveCompletionInProgress) {
            startWaveCompletionEffect();
        }
    }

    private void startWaveCompletionEffect() {
        waveCompletionInProgress = true;
        spawningEnemies.stopCurrentSpawn();
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
        waveCompletionInProgress = false;
        crystalExplosion = null;

        LevelData currentLevelData = levelManager.getLevel(levelManager.getCurrentLevel());

        if (waveNumber <= 10 && currentLevelData != null) {
            LevelData.WaveData waveData = currentLevelData.getWave(waveNumber - 1);

            if (waveData != null) {
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
    public CrystalExplosion getCrystalExplosion() { return crystalExplosion; }
}