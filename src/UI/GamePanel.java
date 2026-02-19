package UI;

import Core.Game;
import Logic.DamageNumber.DamageNumberManager;
import Logic.FPSCounter;
import Logic.GameLogic;
import MainMenu.MainMenuPanel;
import Player.Player;
import Tutorial.TutorialManager;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import javax.swing.*;

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
    private boolean menuVisible = false;
    private FPSCounter fpsCounter = new FPSCounter();

    private TutorialManager tutorialManager;
    private boolean isTutorialMode;

    public GamePanel(Game game, Player player, boolean tutorialMode) {
        this.game = game;
        this.isTutorialMode = tutorialMode;

        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setFocusable(true);
        setLayout(null);

        setCustomCursor();

        gameLogic = new GameLogic(this, player, damageManager, tutorialMode);

        if (tutorialMode) {
            tutorialManager = new TutorialManager(player);
            tutorialManager.startTutorial();
        }

        initializeMenu();
        initializeAbilityPanel();
        initializeFont();
        initializeInput();

        if (!tutorialMode) {
            initializeLevelMap();
        }

        renderer = new GameRenderer(this, pixelPurlFont);

        if (!tutorialMode) {
            showLevelMap();
        } else {
            menuButton.setVisible(true);
            gameLogic.startTutorial();
        }
    }

    private void setCustomCursor() {
        try {
            InputStream cursorStream = getClass().getResourceAsStream("/WATVA/Other/Cursor.png");
            if (cursorStream != null) {
                Image cursorImage = ImageIO.read(cursorStream);

                int targetSize = 32;

                Image scaledCursor = cursorImage.getScaledInstance(targetSize, targetSize, Image.SCALE_SMOOTH);

                ImageIcon tempIcon = new ImageIcon(scaledCursor);
                int actualWidth = tempIcon.getIconWidth();
                int actualHeight = tempIcon.getIconHeight();

                if (actualWidth <= 0 || actualHeight <= 0) {
                    setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
                    return;
                }

                int hotspotX = actualWidth / 2;
                int hotspotY = actualHeight / 2;

                if (hotspotX >= actualWidth) hotspotX = actualWidth - 1;
                if (hotspotY >= actualHeight) hotspotY = actualHeight - 1;

                if (hotspotX < 0) hotspotX = 0;
                if (hotspotY < 0) hotspotY = 0;

                Point hotspot = new Point(hotspotX, hotspotY);

                Cursor customCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                        scaledCursor, hotspot, "GameCursor"
                );
                setCursor(customCursor);
            } else {
                setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
            }
        } catch (Exception e) {
            System.err.println("Error loading cursor: " + e.getMessage());
            e.printStackTrace();
            setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        }
    }

    private void initializeFont() {
        try {
            pixelPurlFont = Font.createFont(Font.TRUETYPE_FONT,
                    getClass().getResourceAsStream("/fonts/PixelPurl.ttf")).deriveFont((float) Game.scale(20f));
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(pixelPurlFont);
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
            pixelPurlFont = new Font("Courier New", Font.BOLD, Game.scale(20));
        }
    }

    private void initializeMenu() {
        int buttonWidth = Game.scale(100);
        int buttonHeight = Game.scale(40);

        URL normalUrl = getClass().getResource("/Buttons/Menu_button1.png");
        URL hoverUrl = getClass().getResource("/Buttons/Menu_button2.png");

        if (normalUrl == null || hoverUrl == null) {
            System.err.println("Error: Menu button icons not found!");
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

        menuButton.addActionListener(e -> toggleMenu());
        add(menuButton);

        menuPanel = new MenuPanel(game, this, gameLogic, isTutorialMode);
        add(menuPanel);
    }

    private void initializeAbilityPanel() {
        abilityPanel = new AbilityPanel(this, gameLogic.getPlayer());
        add(abilityPanel);
    }

    private void initializeLevelMap() {
        levelMapPanel = new LevelMapPanel(game, gameLogic.getLevelManager());
        levelMapPanel.setGamePanel(this);
        add(levelMapPanel);
    }

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

    private void initializeInput() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (isTutorialMode && tutorialManager != null) {
                    if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                        tutorialManager.handleSpacePress();
                        return;
                    }
                    if (e.getKeyCode() == KeyEvent.VK_Q) {
                        tutorialManager.onPlayerExplosion();
                    }
                    if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
                        tutorialManager.onPlayerDash();
                    }
                    tutorialManager.onPlayerMove(e.getKeyCode());
                }
                if (!isTutorialMode || !tutorialManager.isGameFrozen()) {
                    gameLogic.getPlayer().keyPressed(e);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (!isTutorialMode || !tutorialManager.isGameFrozen()) {
                    gameLogic.getPlayer().keyReleased(e);
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (isTutorialMode && tutorialManager != null) {
                    tutorialManager.handleReminderBoxClick(e.getX(), e.getY());
                }
                if ((!isTutorialMode || !tutorialManager.isGameFrozen()) && !gameLogic.isPaused()) {
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

    public void toggleMenu() {
        if (menuPanel.isVisible()) {
            menuPanel.setVisible(false);
            menuButton.setVisible(true);
            menuVisible = false;
            if(!gameLogic.getEnemies().isEmpty()) {
                gameLogic.resumeGame();
            }
        } else {
            menuPanel.setVisible(true);
            menuButton.setVisible(false);
            menuVisible = true;
            gameLogic.pauseGame();
        }
    }

    public void restartGame() {
        gameLogic.stopGame();
        removeAll();
        gameOverPanel = null;
        upgradePanel = null;

        Player newPlayer = new Player(0, 0, 100);
        gameLogic = new GameLogic(this, newPlayer, damageManager, isTutorialMode);

        if (isTutorialMode) {
            tutorialManager = new TutorialManager(newPlayer);
            tutorialManager.startTutorial();
        }

        initializeMenu();
        initializeAbilityPanel();

        if (!isTutorialMode) {
            initializeLevelMap();
        }

        renderer = new GameRenderer(this, pixelPurlFont);

        menuVisible = false;
        upgradePanelVisible = false;
        abilityPanelVisible = false;

        if (!isTutorialMode) {
            showLevelMap();
        } else {
            menuButton.setVisible(true);
            gameLogic.startTutorial();
        }
        revalidate();
        repaint();
    }

    public void returnToMainMenu() {
        if (!isTutorialMode) {
            gameLogic.savePlayerCoins();
        }
        gameLogic.stopGame();

        game.getContentPane().removeAll();
        MainMenuPanel mainMenuPanel = new MainMenuPanel();
        game.setContentPane(mainMenuPanel.getContentPane());
        game.revalidate();
        game.repaint();
        mainMenuPanel.setVisible(false);
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

    public void showLevelMap() {
        if (isTutorialMode) return;
        menuButton.setVisible(false);
        gameLogic.pauseGame();
        levelMapPanel.showMap();
    }

    public void startLevel(int levelNumber) {
        if (!isTutorialMode && levelMapPanel != null) {
            levelMapPanel.hideMap();
        }
        menuButton.setVisible(true);
        gameLogic.startLevel(levelNumber);
    }

    public void onLevelComplete() {
        if (isTutorialMode) return;
        menuButton.setVisible(false);
        JOptionPane.showMessageDialog(
                this,
                "LEVEL COMPLETE!\n\nNext level unlocked!",
                "Victory!",
                JOptionPane.INFORMATION_MESSAGE
        );
        showLevelMap();
    }

    public void onTutorialEnemyKilled() {
        if (tutorialManager != null) {
            tutorialManager.onEnemyKilled();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        fpsCounter.update();

        if (isTutorialMode && tutorialManager != null) {
            tutorialManager.update();
            if (tutorialManager.isTutorialComplete()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Tutorial Complete!\nReturning to main menu...",
                        "Well Done!",
                        JOptionPane.INFORMATION_MESSAGE
                );
                returnToMainMenu();
                return;
            }
        }

        if (abilityPanel.isVisible()) {
            abilityPanel.updateAbilityPanel();
        }

        boolean shouldUpdate = !isTutorialMode || !tutorialManager.isGameFrozen();

        if (shouldUpdate && mousePressed && !gameLogic.isPaused()) {
            Point mousePoint = getMousePosition();
            if (mousePoint != null) {
                currentMouseX = mousePoint.x + getCameraX();
                currentMouseY = mousePoint.y + getCameraY();
            }
            gameLogic.tryToShoot(currentMouseX, currentMouseY);
        }

        if (shouldUpdate) {
            gameLogic.update(damageManager);
        }

        repaint();
    }

    public void onTutorialProjectileFired() {
        if (tutorialManager != null) {
            tutorialManager.onProjectileFired();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        renderer.render(g, gameLogic.getPlayer(), gameLogic.getEnemies(),
                gameLogic.getPlayerProjectiles(), gameLogic.isGameOver(),
                gameLogic.isPaused(), abilityPanelVisible, upgradePanelVisible,
                gameLogic.getKillCount(), damageManager, gameLogic.getCrystalExplosion(),
                menuVisible);

        if (isTutorialMode && tutorialManager != null && tutorialManager.isTutorialActive()) {
            tutorialManager.draw(g, 0, 0);
        }

        if (gameLogic.isGameOver() && !upgradePanelVisible) {
            onGameOver();
        }
    }

    public int getCameraX() { return gameLogic.getCameraX(); }
    public int getCameraY() { return gameLogic.getCameraY(); }
    public Player getPlayer() { return gameLogic.getPlayer(); }
    public static int getWaveNumber() { return GameLogic.getWaveNumber(); }
    public GameLogic getGameLogic() { return gameLogic; }
    public boolean isTutorialMode() { return isTutorialMode; }
}