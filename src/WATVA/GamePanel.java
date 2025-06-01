package WATVA;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

public class GamePanel extends JPanel implements ActionListener {
    public static final int PANEL_WIDTH = (int)Game.getRealScreenWidth();
    public static final int PANEL_HEIGHT = (int)Game.getRealScreenHeight();
    public static final int BLOCK_SIZE = (int)(64 * Game.getScaleFactor());
    public static final int SCALE_FACTOR = 4;
    public static final int CAMERA_WIDTH = PANEL_WIDTH / SCALE_FACTOR;
    public static final int CAMERA_HEIGHT = PANEL_HEIGHT / SCALE_FACTOR;

    private AbilityPanel abilityPanel;
    private boolean abilityPanelVisible;
    private boolean upgradePanelVisible;
    private Game game;
    private JButton menuButton;
    private MenuPanel menuPanel;
    private GameOverPanel gameOverPanel;
    private UpgradePanel upgradePanel;
    private Font pixelPurlFont;
    private GameRenderer renderer;
    private GameLogic gameLogic;
    private boolean mousePressed = false;
    private int currentMouseX, currentMouseY;

    public GamePanel(Game game, Player player) {
        this.game = game;

        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setFocusable(true);
        setLayout(null);

        gameLogic = new GameLogic(this, player);

        initializeMenu();
        initializeAbilityPanel();
        initializeFont();
        initializeInput();

        renderer = new GameRenderer(this, pixelPurlFont);
        gameLogic.startNewGame();
    }

    private void initializeFont() {
        try {
            InputStream is = new FileInputStream("res/fonts/PixelPurl.ttf");
            pixelPurlFont = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont((float) (20f * Game.getScaleFactor()));
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(pixelPurlFont);
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
            System.err.println("Using fallback font instead");
            pixelPurlFont = new Font("Courier New", Font.BOLD, (int)(20 * Game.getScaleFactor()));
        }
    }

    private void initializeMenu() {
        menuButton = new JButton();
        menuButton.setBounds(PANEL_WIDTH / 2 - 50, 10, 100, 40);

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

        menuPanel = new MenuPanel(game, this,gameLogic);
        add(menuPanel);
    }

    private void initializeAbilityPanel() {
        abilityPanel = new AbilityPanel(this, gameLogic.getPlayer());
        add(abilityPanel);
    }

    protected void initializeUpgradePanel() {
        upgradePanel = new UpgradePanel(this, gameLogic.getPlayer());
        add(upgradePanel);
        upgradePanel.showPanel();
        gameOverPanel.setVisible(false);
        upgradePanelVisible = true;
    }

    private void initializeInput() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                gameLogic.getPlayer().keyPressed(e);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                gameLogic.getPlayer().keyReleased(e);
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!gameLogic.isPaused()) {
                    mousePressed = true;
                    currentMouseX = e.getX() + gameLogic.getCameraX();
                    currentMouseY = e.getY() + gameLogic.getCameraY();
                    gameLogic.tryToShoot(currentMouseX, currentMouseY);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                mousePressed = false;
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (!gameLogic.isPaused()) {
                    currentMouseX = e.getX() + gameLogic.getCameraX();
                    currentMouseY = e.getY() + gameLogic.getCameraY();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!gameLogic.isPaused()) {
                    currentMouseX = e.getX() + gameLogic.getCameraX();
                    currentMouseY = e.getY() + gameLogic.getCameraY();
                }
            }
        });
    }

    public void toggleMenu() {
        if (menuPanel.isVisible()) {
            menuPanel.setVisible(false);
            menuButton.setVisible(true);
            if(!gameLogic.getEnemies().isEmpty()) {
                gameLogic.resumeGame();
            }
        } else {
            menuPanel.setVisible(true);
            menuButton.setVisible(false);
            gameLogic.pauseGame();
        }
    }

    public void restartGame() {
        closeGame();
        new Game();
    }

    public void closeGame(){
        gameLogic.stopGame();
        game.dispose();
    }

    public void startNextWaveAfterAbility() {
        abilityPanel.hidePanel();
        menuButton.setVisible(true);
        gameLogic.nextWave();
    }

    public void onGameOver() {
        if (gameOverPanel == null) {
            gameOverPanel = new GameOverPanel(game, this);
            add(gameOverPanel);
        }
        gameOverPanel.setVisible(true);
        menuButton.setVisible(false);
    }

    public void onWaveComplete() {
        menuButton.setVisible(false);
        abilityPanel.showPanel();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (abilityPanel.isVisible()) {
            abilityPanel.updateAbilityPanel();
        }

        if (mousePressed && !gameLogic.isPaused()) {
            gameLogic.tryToShoot(currentMouseX, currentMouseY);
        }

        gameLogic.update();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        renderer.render(g, gameLogic.getPlayer(), gameLogic.getEnemies(),
                gameLogic.getPlayerProjectiles(), gameLogic.isGameOver(),
                gameLogic.isPaused(), abilityPanelVisible, upgradePanelVisible,
                gameLogic.getKillCount());

        if (gameLogic.isGameOver() && !upgradePanelVisible) {
            onGameOver();
        }
    }

    public int getCameraX() {
        return gameLogic.getCameraX();
    }

    public int getCameraY() {
        return gameLogic.getCameraY();
    }

    public Player getPlayer() {
        return gameLogic.getPlayer();
    }

    public static int getWaveNumber() {
        return GameLogic.getWaveNumber();
    }

    public Timer getTimer() {
        return gameLogic.getTimer();
    }
}