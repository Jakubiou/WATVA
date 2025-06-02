package WATVA;

import Lib.Soundtrack;

import java.io.*;
import java.util.ArrayList;
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
    protected static Soundtrack backgroundMusic;
    private long lastShotTime = 0;
    private long attackSpeedInterval = 200;
    private GamePanel gamePanel;

    /**
     * Initializes the game logic with references to game panel and player.
     * Loads game map, initializes game state, and starts background music.
     *
     * @param gamePanel The game panel for rendering
     * @param player The player character
     */
    public GameLogic(GamePanel gamePanel, Player player) {
        this.gamePanel = gamePanel;
        this.player = player;

        backgroundMusic = new Soundtrack("/WATVA/Music/MainSong.wav");
        backgroundMusic.playLoop();

        loadMap("Map1.txt");
        initializeGame();
    }

    /**
     * Loads game map from resource file.
     * Parses text file into 2D integer array representing the game world.
     *
     * @param filename The map file to load from resources
     */
    private void loadMap(String filename) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {

            ArrayList<int[]> mapList = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] tokens = line.split(" ");
                int[] row = new int[tokens.length];
                for (int i = 0; i < tokens.length; i++) {
                    try {
                        row[i] = Integer.parseInt(tokens[i].trim());
                    } catch (NumberFormatException e) {
                        row[i] = 0;
                        System.err.println("Error parsing number: " + tokens[i]);
                    }
                }
                mapList.add(row);
            }

            mapHeight = mapList.size();
            mapWidth = mapList.isEmpty() ? 0 : mapList.get(0).length;
            map = new int[mapHeight][mapWidth];
            for (int i = 0; i < mapHeight; i++) {
                map[i] = mapList.get(i);
            }
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes core game systems and components.
     * Sets up player, enemies, projectiles, collisions, and spawner.
     */
    private void initializeGame() {
        if (player == null) {
            player = new Player(mapWidth * GamePanel.BLOCK_SIZE / 2, mapHeight * GamePanel.BLOCK_SIZE / 2, 100);
            //savePlayerStatus();
        } else {
            loadPlayerStatus();
            player.setX(mapWidth * GamePanel.BLOCK_SIZE / 2);
            player.setY(mapHeight * GamePanel.BLOCK_SIZE / 2);
        }

        updateAttackSpeed();

        enemies = new CopyOnWriteArrayList<>();
        playerProjectiles = new CopyOnWriteArrayList<>();
        collisions = new Collisions(player, enemies, playerProjectiles);
        spawningEnemies = new SpawningEnemies(gamePanel, enemies);

        timer = new Timer(15, gamePanel);
        waveNumber = 0;
    }

    /**
     * Starts a new game session.
     * Begins first wave after short delay.
     */
    public void startNewGame() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        nextWave();
        timer.start();
    }

    /**
     * Main game update loop called each frame.
     * Handles player movement, collisions, enemy updates, and game state checks.
     */
    public void update() {
        if (!gameOver && !isPaused) {
            updateAttackSpeed();
            player.move();

            collisions.checkCollisions();
            gameOver = collisions.isGameOver();

            updateEnemies();
            checkWaveCompletion();
            checkGameOver();
        }
    }

    /**
     * Updates all active enemies including bosses.
     * Handles death states and special boss behaviors.
     */
    private void updateEnemies() {
        boolean bossExists = false;
        boolean bossDeathAnimationComplete = false;

        for (Enemy enemy : enemies) {
            if (enemy instanceof DarkMageBoss) {
                bossExists = true;
                DarkMageBoss darkMageBoss = (DarkMageBoss) enemy;

                if (darkMageBoss.isDead()) {
                    bossDeathAnimationComplete = true;
                    enemies.remove(enemy);
                    break;
                }
                else if (darkMageBoss.isDying()) {
                }
                else {
                    darkMageBoss.updateBossBehavior(player, enemies);
                }
            } else if (enemy instanceof BunnyBoss) {
                bossExists = true;
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

    /**
     * Checks if current wave completion conditions are met.
     * For normal waves: enough enemies killed.
     * For boss waves: boss defeated.
     */
    private void checkWaveCompletion() {
        boolean bossExists = false;
        boolean bossDeathAnimationComplete = false;
        boolean waveComplete = false;

        for (Enemy enemy : enemies) {
            if (enemy instanceof DarkMageBoss || enemy instanceof BunnyBoss) {
                bossExists = true;
                if (enemy instanceof DarkMageBoss) {
                    DarkMageBoss darkMageBoss = (DarkMageBoss) enemy;
                    if (darkMageBoss.isDead()) {
                        bossDeathAnimationComplete = true;
                    }
                }
                break;
            }
        }

        if (bossExists) {
            waveComplete = bossDeathAnimationComplete;
        } else {
            waveComplete = killCount >= waveNumber * 20;
        }

        if (waveComplete && !gameOver) {
            pauseGame();
            gamePanel.onWaveComplete();
        }
    }

    /**
     * Checks game over conditions (player death).
     * Saves player state if game ends.
     */
    private void checkGameOver() {
        if (player.getHp() <= 0) {
            gameOver = true;
            spawningEnemies.stopCurrentSpawn();
            savePlayerCoins();
            player.saveLocation("player_save.dat");
            loadPlayerStatus();
        }
    }

    /**
     * Updates player's attack speed based on upgrades.
     */
    private void updateAttackSpeed() {
        int baseInterval = 200;
        int speedReduction = player.getAttackSpeed() * 10;
        attackSpeedInterval = Math.max(100, baseInterval - speedReduction);
    }

    /**
     * Attempts to fire player projectiles if attack cooldown allows.
     *
     * @param mouseX Target x-coordinate
     * @param mouseY Target y-coordinate
     */
    public void tryToShoot(int mouseX, int mouseY) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShotTime >= attackSpeedInterval) {
            shoot(mouseX, mouseY);
            lastShotTime = currentTime;
        }
    }

    /**
     * Creates and fires player projectiles based on current abilities.
     * Handles different firing patterns (double shot, backward shot, etc).
     *
     * @param mouseX Target x-coordinate
     * @param mouseY Target y-coordinate
     */
    private void shoot(int mouseX, int mouseY) {
        if (player.isExplosionActive() && player.canUseExplosion()) {
            player.triggerExplosion();
        } else {
            int centerX = player.getX() + Player.WIDTH / 2;
            int centerY = player.getY() + Player.HEIGHT / 2;

            int piercingLevel = player.getPiercingLevel();
            int fireLevel = player.getFireLevel();
            boolean hasSlow = player.hasSlowEnemies();

            if (player.isDoubleShotActive() && player.isForwardBackwardShotActive()) {
                playerProjectiles.add(new PlayerProjectile(centerX, centerY, mouseX + 20, mouseY, piercingLevel, fireLevel, hasSlow));
                playerProjectiles.add(new PlayerProjectile(centerX, centerY, mouseX - 20, mouseY, piercingLevel, fireLevel, hasSlow));
                playerProjectiles.add(new PlayerProjectile(centerX, centerY, centerX - (mouseX - centerX), centerY - (mouseY - centerY), piercingLevel, fireLevel, hasSlow));
            } else if (player.isDoubleShotActive()) {
                playerProjectiles.add(new PlayerProjectile(centerX, centerY, mouseX + 20, mouseY, piercingLevel, fireLevel, hasSlow));
                playerProjectiles.add(new PlayerProjectile(centerX, centerY, mouseX - 20, mouseY, piercingLevel, fireLevel, hasSlow));
            } else if (player.isForwardBackwardShotActive()) {
                playerProjectiles.add(new PlayerProjectile(centerX, centerY, mouseX, mouseY, piercingLevel, fireLevel, hasSlow));
                playerProjectiles.add(new PlayerProjectile(centerX, centerY, centerX - (mouseX - centerX), centerY - (mouseY - centerY), piercingLevel, fireLevel, hasSlow));
            } else {
                playerProjectiles.add(new PlayerProjectile(centerX, centerY, mouseX, mouseY, piercingLevel, fireLevel, hasSlow));
            }
        }
    }

    /**
     * Advances to next wave of enemies.
     * Spawns regular enemies or bosses based on wave number.
     * Clears current enemies and resets counters.
     */
    public void nextWave() {
        spawningEnemies.stopCurrentSpawn();
        enemies.clear();
        waveNumber++;
        playerProjectiles.clear();
        killCount = 0;

        boolean isBossWave = (waveNumber % 10 == 0);

        if (waveNumber % 5 == 0 && !(waveNumber % 10 == 0)) {
            spawningEnemies.spawnBunnyBoss();
        }

        if (waveNumber % 10 == 0) {
            spawningEnemies.spawnDarkMageBoss();
        }

        if (!isBossWave) {
            switch (waveNumber) {
                case 1:
                    spawningEnemies.spawnEnemies(1, 0, 1, 0, 1);
                    break;
                case 2:
                    spawningEnemies.spawnEnemies(5, 2, 1, 2, 1);
                    break;
                case 3:
                    spawningEnemies.spawnEnemies(7, 3, 1, 5, 2);
                    break;
                default:
                    spawningEnemies.spawnEnemies(15, 5, 10, 10, 10);
                    break;
            }
        }

        resumeGame();
    }

    /**
     * Pauses game including timer and music.
     */
    public void pauseGame() {
        timer.stop();
        isPaused = true;
        backgroundMusic.stop();
    }

    /**
     * Resumes paused game including timer and music.
     */
    public void resumeGame() {
        timer.start();
        isPaused = false;
        backgroundMusic.playLoop();
    }

    /**
     * Stops game completely and cleans up resources.
     */
    public void stopGame() {
        pauseGame();
        backgroundMusic.stop();
        enemies.clear();
        playerProjectiles.clear();
        collisions = null;
        spawningEnemies = null;
    }

    /**
     * Increments enemy kill counter.
     */
    public static void killCountPlus(){
        killCount++;
    }

    /**
     * Saves player's coin count to file.
     */
    public void savePlayerCoins(){
        player.saveCoins("player_save.dat");
    }

    /**
     * Loads player status from save file.
     * Creates new player if load fails.
     */
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

    public Player getPlayer() {
        return player;
    }

    public CopyOnWriteArrayList<Enemy> getEnemies() {
        return enemies;
    }

    public CopyOnWriteArrayList<PlayerProjectile> getPlayerProjectiles() {
        return playerProjectiles;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public static int getWaveNumber() {
        return waveNumber;
    }

    public int getKillCount() {
        return killCount;
    }

    public int getCameraX() {
        return cameraX;
    }

    public int getCameraY() {
        return cameraY;
    }
}