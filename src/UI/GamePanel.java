package UI;

import Core.Game;
import Logic.DamageNumber.DamageNumberManager;
import Logic.GameLogic;
import Player.Player;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import javax.swing.*;

/**
 * The main game panel that handles rendering, input, and UI management.
 */
public class GamePanel extends JPanel implements ActionListener {
    public static final int PANEL_WIDTH = Game.getScaledGameWidth();
    public static final int PANEL_HEIGHT = Game.getScaledGameHeight();
    public static final int BLOCK_SIZE = Game.scale(64);
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
    private DamageNumberManager damageManager = new DamageNumberManager();
    private LevelMapPanel levelMapPanel;

    /**
     * Constructs the game panel with references to game and player.
     */
    public GamePanel(Game game, Player player) {
        this.game = game;

        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setFocusable(true);
        setLayout(null);

        gameLogic = new GameLogic(this, player, damageManager);

        initializeMenu();
        initializeAbilityPanel();
        initializeFont();
        initializeInput();
        initializeLevelMap();

        renderer = new GameRenderer(this, pixelPurlFont);

        showLevelMap();
    }

    /**
     * Loads and initializes the custom pixel font.
     */
    private void initializeFont() {
        try {
            pixelPurlFont = Font.createFont(Font.TRUETYPE_FONT,
                    getClass().getResourceAsStream("/fonts/PixelPurl.ttf")).deriveFont((float) Game.scale(20f));
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(pixelPurlFont);
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
            System.err.println("Using fallback font instead");
            pixelPurlFont = new Font("Courier New", Font.BOLD, Game.scale(20));
        }
    }

    /**
     * Initializes the in-game menu button and menu panel.
     */
    private void initializeMenu() {
        int buttonWidth = Game.scale(100);
        int buttonHeight = Game.scale(40);

        URL normalUrl = getClass().getResource("/Buttons/Menu_button1.png");
        URL hoverUrl = getClass().getResource("/Buttons/Menu_button2.png");

        if (normalUrl == null || hoverUrl == null) {
            System.err.println("Chyba: Ikony menu tlačítka nebyly nalezeny!");
            return;
        }

        ImageIcon normalMenuIcon = new ImageIcon(new ImageIcon(normalUrl).getImage().getScaledInstance(buttonWidth, buttonHeight, Image.SCALE_SMOOTH));
        ImageIcon rolloverMenuIcon = new ImageIcon(new ImageIcon(hoverUrl).getImage().getScaledInstance(buttonWidth, buttonHeight, Image.SCALE_SMOOTH));

        menuButton = new JButton();
        menuButton.setIcon(normalMenuIcon);
        menuButton.setRolloverIcon(rolloverMenuIcon);
        menuButton.setBounds(PANEL_WIDTH / 2 - buttonWidth / 2, Game.scale(10), buttonWidth, buttonHeight);
        menuButton.setBorderPainted(false);
        menuButton.setContentAreaFilled(false);
        menuButton.setFocusPainted(false);
        menuButton.setOpaque(false);
        menuButton.setPreferredSize(new Dimension(buttonWidth, buttonHeight));
        menuButton.setMaximumSize(new Dimension(buttonWidth, buttonHeight));
        menuButton.setMinimumSize(new Dimension(buttonWidth, buttonHeight));

        menuButton.addActionListener(e -> toggleMenu());
        add(menuButton);

        menuPanel = new MenuPanel(game, this, gameLogic);
        add(menuPanel);
    }

    /**
     * Initializes the ability selection panel shown between waves.
     */
    private void initializeAbilityPanel() {
        abilityPanel = new AbilityPanel(this, gameLogic.getPlayer());
        add(abilityPanel);
    }

    /**
     * Initializes the level map.
     */
    private void initializeLevelMap() {
        levelMapPanel = new LevelMapPanel(game, gameLogic.getLevelManager());
        levelMapPanel.setGamePanel(this);
        add(levelMapPanel);
    }

    /**
     * Initializes the upgrade panel.
     */
    public void initializeUpgradePanel() {
        if (upgradePanel == null) {
            upgradePanel = new UpgradePanel(this, gameLogic.getPlayer(), gameLogic.getLevelManager());
            add(upgradePanel);
        }
        upgradePanel.showPanel();
        if (gameOverPanel != null) {
            gameOverPanel.setVisible(false);
        }
        upgradePanelVisible = true;
    }

    /**
     * Sets up all input handlers.
     */
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
                    currentMouseX = e.getX() + getCameraX();
                    currentMouseY = e.getY() + getCameraY();
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
                    currentMouseX = e.getX() + getCameraX();
                    currentMouseY = e.getY() + getCameraY();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!gameLogic.isPaused()) {
                    currentMouseX = e.getX() + getCameraX();
                    currentMouseY = e.getY() + getCameraY();
                }
            }
        });
    }

    /**
     * Toggles the in-game menu.
     */
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

    /**
     * Restarts the game.
     */
    public void restartGame() {
        closeGame();
        new Game();
    }

    /**
     * Closes the current game.
     */
    public void closeGame(){
        gameLogic.stopGame();
        game.dispose();
    }

    /**
     * Starts next wave after ability selection.
     */
    public void startNextWaveAfterAbility() {
        abilityPanel.hidePanel();
        menuButton.setVisible(true);
        gameLogic.nextWave();
    }

    /**
     * Handles game over.
     */
    public void onGameOver() {
        if (gameOverPanel == null) {
            gameOverPanel = new GameOverPanel(game, this);
            add(gameOverPanel);
        }
        gameOverPanel.setVisible(true);
        menuButton.setVisible(false);
    }

    /**
     * Handles wave completion.
     */
    public void onWaveComplete() {
        menuButton.setVisible(false);
        abilityPanel.showPanel();
    }

    /**
     * Shows level map.
     */
    public void showLevelMap() {
        menuButton.setVisible(false);
        gameLogic.pauseGame();
        levelMapPanel.showMap();
    }

    /**
     * Starts selected level.
     */
    public void startLevel(int levelNumber) {
        levelMapPanel.hideMap();
        menuButton.setVisible(true);
        gameLogic.startLevel(levelNumber);
    }

    /**
     * Called when player completes a level (10 waves).
     */
    public void onLevelComplete() {
        menuButton.setVisible(false);

        JOptionPane.showMessageDialog(
                this,
                "LEVEL COMPLETE!\n\nNext level unlocked!",
                "Victory!",
                JOptionPane.INFORMATION_MESSAGE
        );

        showLevelMap();
    }

    /**
     * Main game loop.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (abilityPanel.isVisible()) {
            abilityPanel.updateAbilityPanel();
        }

        if (mousePressed && !gameLogic.isPaused()) {
            Point mousePoint = getMousePosition();
            if (mousePoint != null) {
                currentMouseX = mousePoint.x + getCameraX();
                currentMouseY = mousePoint.y + getCameraY();
            }
            gameLogic.tryToShoot(currentMouseX, currentMouseY);
        }

        gameLogic.update(damageManager);
        repaint();
    }

    /**
     * Renders all game components.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        renderer.render(g, gameLogic.getPlayer(), gameLogic.getEnemies(),
                gameLogic.getPlayerProjectiles(), gameLogic.isGameOver(),
                gameLogic.isPaused(), abilityPanelVisible, upgradePanelVisible,
                gameLogic.getKillCount(), damageManager);

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

    public GameLogic getGameLogic() {
        return gameLogic;
    }
}