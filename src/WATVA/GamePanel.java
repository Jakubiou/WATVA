package WATVA;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.imageio.ImageIO;
import javax.swing.*;
public class GamePanel extends JPanel implements ActionListener {
    public static final int PANEL_WIDTH = (int)Game.getRealScreenWidth();
    public static final int PANEL_HEIGHT = (int)Game.getRealScreenHeight();
    public static final int BLOCK_SIZE = (int)(64 * Game.getScaleFactor());
    private JButton nextWaveButton;
    private AbilityPanel abilityPanel;
    private boolean abilityPanelVisible;
    private boolean upgradePanelVisible;
    public static int[][] map;
    public static int mapWidth, mapHeight;
    private Player player;
    private CopyOnWriteArrayList<Enemy> enemies;
    private CopyOnWriteArrayList<Arrow> arrows;
    private Timer timer;
    private boolean gameOver = false;
    private boolean isPaused = false;
    private Image[] blockImages;
    private int waveNumber = 0;
    private Game game;
    private JButton menuButton;
    private MenuPanel menuPanel;
    private SpawningEnemies spawningEnemies;
    private Collisions collisions;
    private GameOverPanel gameOverPanel;
    public static final int SCALE_FACTOR = 4;
    private static final int CAMERA_WIDTH = PANEL_WIDTH / SCALE_FACTOR;
    private static final int CAMERA_HEIGHT = PANEL_HEIGHT / SCALE_FACTOR;
    public static int cameraX, cameraY;
    private static int killCount;
    protected static Soundtrack backgroundMusic;
    private UpgradePanel upgradePanel;

    public GamePanel(Game game, Player player) {
        this.game = game;
        this.player = player;
        backgroundMusic = new Soundtrack("res/watva/Music/658572__josefpres__8-bit-game-loop-013-simple-mix-1-short-120-bpm.wav");
        backgroundMusic.playLoop();

        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setFocusable(true);
        setLayout(null);
        initializeMenu();
        loadBlockImages();
        loadMap("map1.txt");
        initGame();
        initializeAbilityPanel();
    }

    private void initializeMenu() {
        menuButton = new JButton();
        menuButton.setBounds(750, 10, 100, 40);

        ImageIcon normalMenuIcon = new ImageIcon(new ImageIcon("res/buttons/Menu_button1.png").getImage().getScaledInstance(100, 40, Image.SCALE_SMOOTH));
        ImageIcon rolloverMenuIcon = new ImageIcon(new ImageIcon("res/buttons/Menu_button2.png").getImage().getScaledInstance(100, 40, Image.SCALE_SMOOTH));

        menuButton.setIcon(normalMenuIcon);
        menuButton.setRolloverIcon(rolloverMenuIcon);
        menuButton.setBorderPainted(false);
        menuButton.setContentAreaFilled(false);
        menuButton.setFocusPainted(false);
        menuButton.setOpaque(false);

        menuButton.addActionListener(e -> toggleMenu());
        add(menuButton);

        menuPanel = new MenuPanel(game, this);
        add(menuPanel);
    }

    public void toggleMenu() {
        if (menuPanel.isVisible()) {
            menuPanel.setVisible(false);
            menuButton.setVisible(true);
            if(!enemies.isEmpty()) {
                startGame();
            }
        } else {
            menuPanel.setVisible(true);
            menuButton.setVisible(false);
            stopGame();
        }
    }

    public void restartGame() {
        closeGame();
        new Game();
    }

    public void closeGame(){
        stopGame();
        backgroundMusic.stop();
        enemies.clear();
        arrows.clear();
        collisions = null;
        spawningEnemies = null;
        game.dispose();
    }

    private void loadBlockImages() {
        blockImages = new Image[26];
        try {
            for (int i = 0; i < blockImages.length; i++) {
                Image original = ImageIO.read(new File("res/watva/background/Block" + i + ".png"));
                blockImages[i] = original.getScaledInstance(
                        BLOCK_SIZE,
                        BLOCK_SIZE,
                        Image.SCALE_SMOOTH
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeAbilityPanel() {
        abilityPanel = new AbilityPanel(this, player);
        add(abilityPanel);
    }
    protected void initializeUpgradePanel() {
        upgradePanel = new UpgradePanel(this, player);
        add(upgradePanel);
        upgradePanel.showPanel();
        gameOverPanel.setVisible(false);
        upgradePanelVisible = true;
    }

    private void stopGame() {
        timer.stop();
        isPaused = true;
        backgroundMusic.stop();
    }

    private void startGame() {
        timer.start();
        isPaused = false;
        backgroundMusic.playLoop();
    }
    private void loadMap(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initGame() {
        String saveFilePath = "player_save.dat";

        if (player == null) {
            player = new Player(mapWidth * BLOCK_SIZE / 2, mapHeight * BLOCK_SIZE / 2, 100);
            //savePlayerStatus();
        } else {
            player = Player.loadState(saveFilePath);
            player.setX(mapWidth * BLOCK_SIZE / 2);
            player.setY(mapHeight * BLOCK_SIZE / 2);
        }
        System.out.println("Player initialized: x=" + player.getX() + ", y=" + player.getY() +
                ", hp=" + player.getHp() + ", coins=" + player.getCoins());

        enemies = new CopyOnWriteArrayList<>();
        arrows = new CopyOnWriteArrayList<>();
        collisions = new Collisions(player, enemies, arrows);
        gameOverPanel = new GameOverPanel(game, this);
        gameOverPanel.setVisible(false);
        add(gameOverPanel);


        spawningEnemies = new SpawningEnemies(this, enemies);

        nextWaveButton = new JButton();
        nextWaveButton.setBounds(10, 60, 200, 50);

        ImageIcon normalIcon = new ImageIcon(new ImageIcon("res/buttons/NextWave_button1.png").getImage().getScaledInstance(200, 50, Image.SCALE_SMOOTH));
        ImageIcon rolloverIcon = new ImageIcon(new ImageIcon("res/buttons/NextWave_button2.png").getImage().getScaledInstance(200, 50, Image.SCALE_SMOOTH));
        nextWaveButton.setIcon(normalIcon);
        nextWaveButton.setRolloverIcon(rolloverIcon);

        nextWaveButton.setBorderPainted(false);
        nextWaveButton.setContentAreaFilled(false);
        nextWaveButton.setFocusPainted(false);
        nextWaveButton.setOpaque(false);

        nextWaveButton.setVisible(false);
        nextWaveButton.addActionListener(e -> startNextWave());
        add(nextWaveButton);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                player.keyPressed(e);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                player.keyReleased(e);
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                if (key == KeyEvent.VK_1) {
                    player.toggleMeleeMode();
                } else if (key == KeyEvent.VK_2) {
                    player.toggleMeleeMode();
                } else {
                    player.keyPressed(e);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                player.keyReleased(e);
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!isPaused) {
                    int mouseX = e.getX() + cameraX;
                    int mouseY = e.getY() + cameraY;

                    if (player.isMeleeMode()) {
                        player.performMeleeAttack(mouseX, mouseY);
                    } else if (player.isExplosionActive() && player.canUseExplosion()) {
                        player.triggerExplosion();
                    } else {
                        int centerX = player.getX() + Player.WIDTH / 2;
                        int centerY = player.getY() + Player.HEIGHT / 2;

                        if (player.isDoubleShotActive() && player.isForwardBackwardShotActive()) {
                            arrows.add(new Arrow(centerX, centerY, mouseX + 20, mouseY));
                            arrows.add(new Arrow(centerX, centerY, mouseX - 20, mouseY));
                            arrows.add(new Arrow(centerX, centerY, centerX - (mouseX - centerX), centerY - (mouseY - centerY)));
                        } else if (player.isDoubleShotActive()) {
                            arrows.add(new Arrow(centerX, centerY, mouseX + 20, mouseY));
                            arrows.add(new Arrow(centerX, centerY, mouseX - 20, mouseY));
                        } else if (player.isForwardBackwardShotActive()) {
                            arrows.add(new Arrow(centerX, centerY, mouseX, mouseY));
                            arrows.add(new Arrow(centerX, centerY, centerX - (mouseX - centerX), centerY - (mouseY - centerY)));
                        } else {
                            arrows.add(new Arrow(centerX, centerY, mouseX, mouseY));
                        }
                    }
                }
            }
        });

        timer = new Timer(15, this);
        waveNumber = 0;
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        nextWave();
        timer.start();
    }

    private void nextWave() {
        spawningEnemies.stopCurrentSpawn();
        enemies.clear();
        waveNumber++;
        arrows.clear();
        killCount = 0;
        spawningEnemies.spawnBunnyBoss();

            switch (waveNumber) {
                case 10:
                    spawningEnemies.spawnEnemies(0, 0, 10, 0, 0);
                    break;
                case 2:
                    spawningEnemies.spawnEnemies(5, 2, 1, 2, 1);
                    break;
                case 3:
                    spawningEnemies.spawnEnemies(7, 3, 1, 5, 2);
                    break;
                case 1:
                    //spawningEnemies.spawnDarkMageBoss();
                    spawningEnemies.spawnEnemies(0, 0, 0, 0, 1);
                    break;
                default:
                    spawningEnemies.spawnEnemies(waveNumber * 4, waveNumber * 2, waveNumber * 3, waveNumber * 3, 5);
                    break;
            }


        startGame();
    }


    private void startNextWave() {
        nextWaveButton.setVisible(false);
        abilityPanel.hidePanel();
        nextWave();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver && !isPaused) {
            if (abilityPanel.isVisible()) {
                abilityPanel.updateAbilityPanel();
            }
            player.move();

            collisions.checkCollisions();
            gameOver = collisions.isGameOver();
            boolean bossDead = false;

            for (Enemy enemy : enemies) {
                if (enemy instanceof DarkMageBoss) {
                    DarkMageBoss darkMageBoss = (DarkMageBoss) enemy;
                    if (darkMageBoss.isDead()) {
                        bossDead = true;
                    }else {
                        darkMageBoss.updateBossBehavior(player, enemies);
                    }
                } else if (enemy instanceof BunnyBoss) {
                    BunnyBoss bunnyBoss = (BunnyBoss) enemy;

                        bunnyBoss.updateBossBehavior(player, enemies);

                } else if (enemy.getType() == Enemy.Type.SHOOTING) {
                    enemy.updateProjectiles();
                }
            }

            if ((killCount > waveNumber * 1 || bossDead) && !gameOver) {
                stopGame();
                nextWaveButton.setVisible(true);
                abilityPanel.showPanel();
            }

            if (player.getHp() <= 0) {
                gameOver = true;
                gameOverPanel.setVisible(true);
                menuButton.setVisible(false);
                savePlayerCoins();
                player.saveLocation("player_save.dat");
                loadPlayerStatus();
            }
            repaint();
        }
    }
    public static void killCountPlus(){
        killCount++;
    }
    public void savePlayerStatus(){
        player.saveState("player_save.dat");
    }
    public void savePlayerCoins(){
        player.saveCoins("player_save.dat");
    }
    public void loadPlayerStatus(){
        player = Player.loadState("player_save.dat");
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        updateCamera();
        Graphics2D g2d = (Graphics2D) g;

        g2d.translate(-cameraX, -cameraY);

        drawBackground(g2d);
        drawEnemies(g2d);
        drawArrows(g2d);
        drawUI(g2d);
        drawPlayer(g2d);

        g2d.translate(cameraX, cameraY);

        for (Enemy enemy : enemies) {
            if (enemy.getType() == Enemy.Type.SHOOTING) {
                enemy.drawProjectiles(g);
            }
        }

        if (abilityPanelVisible) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        if (gameOver && !upgradePanelVisible) {
            gameOverPanel.setVisible(true);
            menuButton.setVisible(false);
        }
    }

    private void updateCamera() {
        int targetCameraX = player.getX() - CAMERA_WIDTH * 2;
        int targetCameraY = player.getY() - CAMERA_HEIGHT * 2;

        cameraX = Math.max(0, Math.min(targetCameraX, mapWidth * BLOCK_SIZE - PANEL_WIDTH));
        cameraY = Math.max(0, Math.min(targetCameraY, mapHeight * BLOCK_SIZE - PANEL_HEIGHT));
    }
    private void drawBackground(Graphics g) {
        for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
                int blockType = map[y][x];
                Image blockImage = blockImages[blockType];
                g.drawImage(blockImage, x * BLOCK_SIZE, y * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE, null);
            }
        }
    }
    private void drawPlayer(Graphics g) {
        player.draw(g);
    }
    public void drawEnemies(Graphics g) {
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
    private void drawArrows(Graphics g) {
        for (Arrow arrow : arrows) {
            arrow.draw(g);
        }
    }
    private void drawUI(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Wave: " + waveNumber, 10 + cameraX, 20 + cameraY);
        g.drawString("Coins: " + player.getCoins(), 10 + cameraX, 40 + cameraY);
    }

    public int getCameraX() {
        return cameraX;
    }

    public int getCameraY() {
        return cameraY;
    }

    public Player getPlayer() {
        return player;
    }
}
