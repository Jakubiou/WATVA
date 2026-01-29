package UI;

import Core.Game;
import Logic.Level.LevelManager;
import MainMenu.MainMenuPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LevelMapPanel extends JPanel {
    private LevelManager levelManager;
    private Game game;
    private GamePanel gamePanel;
    private Font pixelFont;
    private Image backgroundImage;

    private Point[] levelPositions;
    private static final int LEVEL_BUTTON_SIZE = Game.scale(60);
    private int hoveredLevel = -1;

    public LevelMapPanel(Game game, LevelManager levelManager) {
        this.game = game;
        this.levelManager = levelManager;

        setLayout(null);
        setBounds(0, 0, GamePanel.PANEL_WIDTH, GamePanel.PANEL_HEIGHT);

        loadResources();
        initializeLevelPositions();
        setupMouseListener();
        setVisible(false);
    }

    private void loadResources() {
        try {
            pixelFont = Font.createFont(Font.TRUETYPE_FONT,
                            getClass().getResourceAsStream("/fonts/PixelPurl.ttf"))
                    .deriveFont((float)Game.scale(20));
        } catch (Exception e) {
            pixelFont = new Font("Arial", Font.BOLD, Game.scale(20));
        }

        try {
            backgroundImage = new ImageIcon(getClass().getResource("/watva/other/WorldMap.png")).getImage();
        } catch (Exception e) {
            System.err.println("Could not load WorldMap.png from /other/");
        }
    }

    private void initializeLevelPositions() {
        levelPositions = new Point[10];
        int startX = Game.scale(100);
        int startY = Game.scale(1000);

        levelPositions[0] = new Point(startX + Game.scale(250), startY);
        levelPositions[1] = new Point(startX, startY - Game.scale(350));
        levelPositions[2] = new Point(startX + Game.scale(150), startY - Game.scale(600));
        levelPositions[3] = new Point(startX + Game.scale(450), startY - Game.scale(300));
        levelPositions[4] = new Point(startX + Game.scale(700), startY - Game.scale(500));
        levelPositions[5] = new Point(startX + Game.scale(850), startY - Game.scale(100));
        levelPositions[6] = new Point(startX + Game.scale(1150), startY - Game.scale(100));
        levelPositions[7] = new Point(startX + Game.scale(1350), startY - Game.scale(350));
        levelPositions[8] = new Point(startX + Game.scale(1700), startY - Game.scale(350));
        levelPositions[9] = new Point(startX + Game.scale(1650), startY - Game.scale(600));
    }

    private void setupMouseListener() {
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                hoveredLevel = -1;
                for (int i = 0; i < 10; i++) {
                    if (isPointInLevel(e.getPoint(), i)) {
                        hoveredLevel = i + 1;
                        break;
                    }
                }
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (isPointInBack(e.getPoint())) {
                    returnToMainMenu();
                    return;
                }

                for (int i = 0; i < 10; i++) {
                    if (isPointInLevel(e.getPoint(), i)) {
                        int levelNumber = i + 1;
                        if (levelManager.isLevelUnlocked(levelNumber)) {
                            selectLevel(levelNumber);
                        }
                        break;
                    }
                }
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    private boolean isPointInLevel(Point click, int levelIndex) {
        Point levelPos = levelPositions[levelIndex];
        int dx = click.x - (levelPos.x + LEVEL_BUTTON_SIZE / 2);
        int dy = click.y - (levelPos.y + LEVEL_BUTTON_SIZE / 2);
        return Math.sqrt(dx * dx + dy * dy) <= LEVEL_BUTTON_SIZE / 2;
    }

    private boolean isPointInBack(Point p) {
        int x = Game.scale(20);
        int y = Game.scale(20);
        int w = Game.scale(150);
        int h = Game.scale(50);
        return p.x >= x && p.x <= x + w && p.y >= y && p.y <= y + h;
    }

    private void returnToMainMenu() {
        new MainMenuPanel();
        Window topWindow = SwingUtilities.getWindowAncestor(this);
        if (topWindow != null) {
            topWindow.dispose();
        }
    }

    private void selectLevel(int levelNumber) {
        levelManager.setCurrentLevel(levelNumber);
        setVisible(false);
        if (gamePanel != null) {
            gamePanel.startLevel(levelNumber);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        if (backgroundImage != null) {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null);
        } else {
            g2d.setColor(new Color(20, 20, 30));
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setFont(pixelFont.deriveFont((float)Game.scale(48)));
        g2d.setColor(Color.WHITE);
        String title = "SELECT LEVEL";
        int titleWidth = g2d.getFontMetrics().stringWidth(title);
        g2d.drawString(title, (GamePanel.PANEL_WIDTH - titleWidth) / 2, Game.scale(50));

        drawPaths(g2d);

        for (int i = 0; i < 10; i++) {
            drawLevel(g2d, i + 1, levelPositions[i]);
        }

        drawBackButton(g2d);
    }

    private void drawPaths(Graphics2D g2d) {
        g2d.setStroke(new BasicStroke(Game.scale(5)));
        for (int i = 0; i < 9; i++) {
            Point from = levelPositions[i];
            Point to = levelPositions[i + 1];
            if (levelManager.isLevelUnlocked(i + 2)) {
                g2d.setColor(new Color(100, 200, 100, 150));
            } else {
                g2d.setColor(new Color(80, 80, 80, 150));
            }
            g2d.drawLine(
                    from.x + LEVEL_BUTTON_SIZE / 2,
                    from.y + LEVEL_BUTTON_SIZE / 2,
                    to.x + LEVEL_BUTTON_SIZE / 2,
                    to.y + LEVEL_BUTTON_SIZE / 2
            );
        }
    }

    private void drawLevel(Graphics2D g2d, int levelNumber, Point position) {
        boolean isUnlocked = levelManager.isLevelUnlocked(levelNumber);
        boolean isHovered = (hoveredLevel == levelNumber);
        boolean isCurrent = (levelManager.getCurrentLevel() == levelNumber);

        int size = LEVEL_BUTTON_SIZE;
        if (isHovered) {
            size = (int)(LEVEL_BUTTON_SIZE * 1.1);
        }

        int x = position.x + (LEVEL_BUTTON_SIZE - size) / 2;
        int y = position.y + (LEVEL_BUTTON_SIZE - size) / 2;

        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillOval(x + Game.scale(5), y + Game.scale(5), size, size);

        if (!isUnlocked) {
            g2d.setColor(new Color(60, 60, 60));
            g2d.fillOval(x, y, size, size);
            g2d.setColor(new Color(40, 40, 40));
            g2d.setStroke(new BasicStroke(Game.scale(3)));
            g2d.drawOval(x, y, size, size);
        } else if (levelNumber == 10) {
            GradientPaint gradient = new GradientPaint(x, y, new Color(255, 215, 0), x + size, y + size, new Color(218, 165, 32));
            g2d.setPaint(gradient);
            g2d.fillOval(x, y, size, size);
            g2d.setColor(new Color(184, 134, 11));
            g2d.setStroke(new BasicStroke(Game.scale(3)));
            g2d.drawOval(x, y, size, size);
        } else if (isCurrent) {
            g2d.setColor(new Color(50, 150, 255));
            g2d.fillOval(x, y, size, size);
            g2d.setColor(new Color(30, 100, 200));
            g2d.setStroke(new BasicStroke(Game.scale(3)));
            g2d.drawOval(x, y, size, size);
        } else {
            g2d.setColor(new Color(80, 200, 80));
            g2d.fillOval(x, y, size, size);
            g2d.setColor(new Color(50, 150, 50));
            g2d.setStroke(new BasicStroke(Game.scale(3)));
            g2d.drawOval(x, y, size, size);
        }

        g2d.setFont(pixelFont.deriveFont((float)Game.scale(24)));
        String text = isUnlocked ? String.valueOf(levelNumber) : "Locked";
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();

        g2d.setColor(Color.BLACK);
        g2d.drawString(text, x + (size - textWidth) / 2 + 2, y + (size + textHeight / 2) / 2 + 2);
        g2d.setColor(Color.WHITE);
        g2d.drawString(text, x + (size - textWidth) / 2, y + (size + textHeight / 2) / 2);

        if (isHovered && isUnlocked) {
            drawLevelInfo(g2d, levelNumber, x, y - Game.scale(60));
        }
    }

    private void drawLevelInfo(Graphics2D g2d, int levelNumber, int x, int y) {
        String info = (levelNumber == 10) ? "FINAL LEVEL" : "Level " + levelNumber;
        g2d.setFont(pixelFont.deriveFont((float)Game.scale(16)));
        FontMetrics fm = g2d.getFontMetrics();
        int width = fm.stringWidth(info) + Game.scale(20);
        int height = fm.getHeight() + Game.scale(10);
        int infoX = x + LEVEL_BUTTON_SIZE / 2 - width / 2;
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRoundRect(infoX, y, width, height, Game.scale(10), Game.scale(10));
        g2d.setColor(Color.YELLOW);
        g2d.drawString(info, infoX + Game.scale(10), y + fm.getAscent() + Game.scale(5));
    }

    private void drawBackButton(Graphics2D g2d) {
        int buttonWidth = Game.scale(150);
        int buttonHeight = Game.scale(50);
        int buttonX = Game.scale(20);
        int buttonY = Game.scale(20);

        g2d.setColor(new Color(200, 50, 50));
        g2d.fillRoundRect(buttonX, buttonY, buttonWidth, buttonHeight, Game.scale(15), Game.scale(15));
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(Game.scale(2)));
        g2d.drawRoundRect(buttonX, buttonY, buttonWidth, buttonHeight, Game.scale(15), Game.scale(15));

        g2d.setFont(pixelFont.deriveFont((float)Game.scale(20)));
        String text = "BACK";
        FontMetrics fm = g2d.getFontMetrics();
        int textX = buttonX + (buttonWidth - fm.stringWidth(text)) / 2;
        int textY = buttonY + (buttonHeight + fm.getHeight()) / 2 - Game.scale(3);
        g2d.drawString(text, textX, textY);
    }

    public void setGamePanel(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
    }

    public void showMap() {
        setVisible(true);
        repaint();
    }

    public void hideMap() {
        setVisible(false);
    }
}