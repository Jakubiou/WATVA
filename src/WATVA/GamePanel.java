package WATVA;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import javax.swing.*;

/**
 * The main game panel that handles rendering, input, and UI management.
 * Acts as the central hub coordinating game logic, rendering, and user interface components.
 */
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

    /**
     * Constructs the game panel with references to game and player.
     * Initializes all game systems, UI components, and input handlers.
     *
     * @param game The main Game instance
     * @param player The player character
     */
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

    /**
     * Loads and initializes the custom pixel font.
     * Falls back to system font if custom font cannot be loaded.
     */
    private void initializeFont() {
        try {
            pixelPurlFont = Font.createFont(Font.TRUETYPE_FONT,
                    getClass().getResourceAsStream("/fonts/PixelPurl.ttf")).deriveFont((float) (20f * Game.getScaleFactor()));
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(pixelPurlFont);
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
            System.err.println("Using fallback font instead");
            pixelPurlFont = new Font("Courier New", Font.BOLD, (int)(20 * Game.getScaleFactor()));
        }
    }

    /**
     * Initializes the in-game menu button and menu panel.
     * Sets up button appearance and click behavior.
     */
    private void initializeMenu() {
        int buttonWidth = 100;
        int buttonHeight = 40;

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
        menuButton.setBounds(PANEL_WIDTH / 2 - buttonWidth / 2, 10, buttonWidth, buttonHeight);
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
     * Initializes the upgrade panel for player progression.
     */
    protected void initializeUpgradePanel() {
        upgradePanel = new UpgradePanel(this, gameLogic.getPlayer());
        add(upgradePanel);
        upgradePanel.showPanel();
        gameOverPanel.setVisible(false);
        upgradePanelVisible = true;
    }

    /**
     * Sets up all input handlers for keyboard and mouse.
     * Handles player movement, shooting, and UI interactions.
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

    /**
     * Toggles the in-game menu visibility.
     * Pauses/resumes game when menu is shown/hidden.
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
     * Restarts the game from scratch.
     * Creates new game instance and disposes current one.
     */
    public void restartGame() {
        closeGame();
        new Game();
    }

    /**
     * Closes the current game session.
     * Stops all game systems and disposes the window.
     */
    public void closeGame(){
        gameLogic.stopGame();
        game.dispose();
    }

    /**
     * Starts the next wave after ability selection.
     * Hides ability panel and resumes gameplay.
     */
    public void startNextWaveAfterAbility() {
        abilityPanel.hidePanel();
        menuButton.setVisible(true);
        gameLogic.nextWave();
    }

    /**
     * Handles game over state.
     * Shows game over panel and hides menu button.
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
     * Shows ability panel and hides menu button.
     */
    public void onWaveComplete() {
        menuButton.setVisible(false);
        abilityPanel.showPanel();
    }

    /**
     * Main game loop called by timer.
     * Updates game state, handles input, and triggers repaint.
     *
     * @param e The action event from timer
     */
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

    /**
     * Renders all game components.
     * Delegates to GameRenderer for actual drawing.
     *
     * @param g The Graphics context for rendering
     */
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
}