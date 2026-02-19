package Tutorial;

import Core.Game;
import Player.Player;
import UI.GamePanel;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TutorialManager {
    private List<TutorialStep> steps;
    private int currentStepIndex = 0;
    private boolean tutorialActive = false;
    private boolean tutorialComplete = false;
    private boolean gameFrozen = true;
    private boolean showMainBox = true;
    private Player player;
    private Font pixelFont;
    private Font pixelFontSmall;
    private Font pixelFontTiny;

    private int killCount = 0;
    private int projectilesFired = 0;
    private boolean hasMoved = false;
    private boolean hasMovedUp = false;
    private boolean hasMovedDown = false;
    private boolean hasMovedLeft = false;
    private boolean hasMovedRight = false;
    private boolean hasDashed = false;
    private boolean hasUsedExplosion = false;

    private long stepCompletionTime = 0;
    private static final long STEP_TRANSITION_DELAY = 200;

    public TutorialManager(Player player) {
        this.player = player;
        loadFont();
        initializeTutorialSteps();
    }

    private void loadFont() {
        try {
            pixelFont = Font.createFont(Font.TRUETYPE_FONT,
                    getClass().getResourceAsStream("/fonts/PixelPurl.ttf")).deriveFont((float) Game.scale(28f));
            pixelFontSmall = Font.createFont(Font.TRUETYPE_FONT,
                    getClass().getResourceAsStream("/fonts/PixelPurl.ttf")).deriveFont((float) Game.scale(22f));
            pixelFontTiny = Font.createFont(Font.TRUETYPE_FONT,
                    getClass().getResourceAsStream("/fonts/PixelPurl.ttf")).deriveFont((float) Game.scale(16f));
        } catch (Exception e) {
            pixelFont = new Font("Arial", Font.BOLD, Game.scale(28));
            pixelFontSmall = new Font("Arial", Font.PLAIN, Game.scale(22));
            pixelFontTiny = new Font("Arial", Font.PLAIN, Game.scale(16));
        }
    }

    private void initializeTutorialSteps() {
        steps = new ArrayList<>();

        steps.add(new TutorialStep(
                "Welcome to WATVA!",
                "You are a lone survivor in a world\noverrun by enemies.\nThis tutorial will teach you how to survive.\n\nPress SPACE to continue",
                TutorialStep.StepType.INFO
        ));

        steps.add(new TutorialStep(
                "Basic Movement",
                "Use WASD or Arrow Keys to move.\nPress ALL direction keys:\nW, A, S, D to continue!",
                TutorialStep.StepType.MOVE
        ));

        steps.add(new TutorialStep(
                "Combat - Shooting",
                "LEFT CLICK (or hold) to shoot arrows.\nYour arrows automatically aim\nwhere you click.\n\nShoot 5 arrows to continue!",
                TutorialStep.StepType.SHOOT
        ));

        steps.add(new TutorialStep(
                "Eliminate Enemies",
                "Enemies will spawn but WON'T hurt you.\nThis is just for practice!\n\nKill 3 enemies to continue.",
                TutorialStep.StepType.KILL_ENEMIES
        ));

        steps.add(new TutorialStep(
                "Dash Ability",
                "Press SHIFT while moving to DASH.\nDash has a 5 second cooldown.\nUse it to escape danger!\n\nTry dashing now!",
                TutorialStep.StepType.DASH
        ));

        steps.add(new TutorialStep(
                "Explosion Ability",
                "Press Q to trigger EXPLOSION!\nDestroys all nearby enemies.\nCooldown: 5 seconds\n\nUse explosion now!",
                TutorialStep.StepType.EXPLOSION
        ));

        steps.add(new TutorialStep(
                "Collecting Resources",
                "Dead enemies drop COINS\n(shown in top-left).\nCoins buy permanent upgrades.\n\nPress SPACE to continue",
                TutorialStep.StepType.INFO
        ));

        steps.add(new TutorialStep(
                "Wave System",
                "Game is divided into WAVES.\nKill enemies to progress.\nProgress bar shows advancement.\n\nPress SPACE to continue",
                TutorialStep.StepType.INFO
        ));

        steps.add(new TutorialStep(
                "Abilities & Upgrades",
                "Between waves, choose ABILITIES:\n• Double Shot\n• Fire/Piercing arrows\n• Speed boost\n\nPress SPACE to continue",
                TutorialStep.StepType.INFO
        ));

        steps.add(new TutorialStep(
                "Boss Battles",
                "Wave 5: MINI-BOSS\nWave 10: MAJOR BOSS\nBosses are very tough!\n\nPress SPACE to continue",
                TutorialStep.StepType.INFO
        ));

        steps.add(new TutorialStep(
                "Health & Survival",
                "HP shown in bottom-left.\nEnemies damage on contact.\nUse dash to avoid!\n\nPress SPACE to continue",
                TutorialStep.StepType.INFO
        ));

        steps.add(new TutorialStep(
                "Permanent Upgrades",
                "Use coins in SHOP:\n• More damage\n• More health\n• Better defense\n\nPress SPACE to continue",
                TutorialStep.StepType.INFO
        ));

        steps.add(new TutorialStep(
                "Final Tips",
                "• Keep moving\n• Use abilities wisely\n• Boss arenas trap you\n• Difficulty increases\n\nPress SPACE to continue",
                TutorialStep.StepType.INFO
        ));

        steps.add(new TutorialStep(
                "Tutorial Complete!",
                "You're ready!\nGood luck, warrior!\n\nPress SPACE to return to menu",
                TutorialStep.StepType.COMPLETE
        ));
    }

    public void startTutorial() {
        tutorialActive = true;
        currentStepIndex = 0;
        tutorialComplete = false;
        gameFrozen = true;
        showMainBox = true;
        stepCompletionTime = 0;
        resetProgress();
    }

    public void update() {
        if (!tutorialActive || tutorialComplete) return;

        if (stepCompletionTime > 0) {
            if (System.currentTimeMillis() - stepCompletionTime >= STEP_TRANSITION_DELAY) {
                actuallyAdvanceStep();
                stepCompletionTime = 0;
            }
            return;
        }

        if (gameFrozen) return;

        TutorialStep currentStep = steps.get(currentStepIndex);

        switch (currentStep.getType()) {
            case MOVE:
                if (hasMovedUp && hasMovedDown && hasMovedLeft && hasMovedRight) {
                    scheduleNextStep();
                }
                break;
            case SHOOT:
                if (projectilesFired >= 5) {
                    scheduleNextStep();
                }
                break;
            case DASH:
                if (hasDashed) {
                    scheduleNextStep();
                }
                break;
            case EXPLOSION:
                if (hasUsedExplosion) {
                    scheduleNextStep();
                }
                break;
            case KILL_ENEMIES:
                if (killCount >= 3) {
                    scheduleNextStep();
                }
                break;
        }
    }

    private void scheduleNextStep() {
        stepCompletionTime = System.currentTimeMillis();
    }

    private void actuallyAdvanceStep() {
        gameFrozen = true;
        showMainBox = true;
        currentStepIndex++;
        resetProgress();
    }

    public void draw(Graphics g, int cameraX, int cameraY) {
        if (!tutorialActive || tutorialComplete) return;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        TutorialStep currentStep = steps.get(currentStepIndex);

        if (showMainBox) {
            drawMainTutorialBox(g2d, currentStep);
        } else {
            drawReminderBox(g2d, currentStep);
        }

        if (!gameFrozen) {
            drawVisualHints(g2d, currentStep);
        }
    }

    private void drawMainTutorialBox(Graphics2D g2d, TutorialStep step) {
        int boxWidth = Game.scale(800);
        int boxHeight = Game.scale(300);
        int boxX = (GamePanel.PANEL_WIDTH - boxWidth) / 2;
        int boxY = (GamePanel.PANEL_HEIGHT - boxHeight) / 2 - Game.scale(50);

        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, GamePanel.PANEL_WIDTH, GamePanel.PANEL_HEIGHT);

        g2d.setColor(new Color(0, 0, 0, 240));
        g2d.fillRoundRect(boxX, boxY, boxWidth, boxHeight, Game.scale(25), Game.scale(25));

        g2d.setColor(new Color(255, 215, 0));
        g2d.setStroke(new BasicStroke(Game.scale(4)));
        g2d.drawRoundRect(boxX, boxY, boxWidth, boxHeight, Game.scale(25), Game.scale(25));

        g2d.setFont(pixelFont.deriveFont(Font.BOLD, Game.scale(36)));
        g2d.setColor(new Color(255, 215, 0));
        String title = step.getTitle();
        int titleWidth = g2d.getFontMetrics().stringWidth(title);
        g2d.drawString(title, boxX + (boxWidth - titleWidth) / 2, boxY + Game.scale(50));

        g2d.setFont(pixelFontSmall);
        g2d.setColor(Color.WHITE);
        String[] lines = step.getDescription().split("\n");
        int lineY = boxY + Game.scale(105);
        int lineSpacing = Game.scale(32);
        int maxWidth = boxWidth - Game.scale(60);

        for (String line : lines) {
            if (g2d.getFontMetrics().stringWidth(line) > maxWidth) {
                String[] words = line.split(" ");
                StringBuilder currentLine = new StringBuilder();

                for (String word : words) {
                    String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                    if (g2d.getFontMetrics().stringWidth(testLine) <= maxWidth) {
                        if (currentLine.length() > 0) currentLine.append(" ");
                        currentLine.append(word);
                    } else {
                        int lineWidth = g2d.getFontMetrics().stringWidth(currentLine.toString());
                        g2d.drawString(currentLine.toString(), boxX + (boxWidth - lineWidth) / 2, lineY);
                        lineY += lineSpacing;
                        currentLine = new StringBuilder(word);
                    }
                }
                if (currentLine.length() > 0) {
                    int lineWidth = g2d.getFontMetrics().stringWidth(currentLine.toString());
                    g2d.drawString(currentLine.toString(), boxX + (boxWidth - lineWidth) / 2, lineY);
                    lineY += lineSpacing;
                }
            } else {
                int lineWidth = g2d.getFontMetrics().stringWidth(line);
                g2d.drawString(line, boxX + (boxWidth - lineWidth) / 2, lineY);
                lineY += lineSpacing;
            }
        }

        if (step.getType() == TutorialStep.StepType.INFO ||
                step.getType() == TutorialStep.StepType.COMPLETE) {
            g2d.setFont(pixelFontSmall.deriveFont(Font.ITALIC, Game.scale(20)));
            g2d.setColor(new Color(200, 200, 200));
            String hint = "(Press SPACE)";
            int hintWidth = g2d.getFontMetrics().stringWidth(hint);
            g2d.drawString(hint, boxX + (boxWidth - hintWidth) / 2, boxY + boxHeight - Game.scale(20));
        }

        g2d.setFont(pixelFontSmall.deriveFont(Game.scale(18)));
        g2d.setColor(new Color(150, 150, 150));
        String stepInfo = "Step " + (currentStepIndex + 1) + " / " + steps.size();
        int stepWidth = g2d.getFontMetrics().stringWidth(stepInfo);
        g2d.drawString(stepInfo, boxX + boxWidth - stepWidth - Game.scale(20), boxY + boxHeight - Game.scale(15));
    }

    private void drawReminderBox(Graphics2D g2d, TutorialStep step) {
        int boxWidth = Game.scale(350);
        int boxHeight = Game.scale(80);
        int boxX = GamePanel.PANEL_WIDTH - boxWidth - Game.scale(20);
        int boxY = Game.scale(20);

        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRoundRect(boxX, boxY, boxWidth, boxHeight, Game.scale(15), Game.scale(15));

        long time = System.currentTimeMillis();
        int alpha = (int)(Math.sin(time / 300.0) * 30 + 225);
        g2d.setColor(new Color(255, 215, 0, alpha));
        g2d.setStroke(new BasicStroke(Game.scale(3)));
        g2d.drawRoundRect(boxX, boxY, boxWidth, boxHeight, Game.scale(15), Game.scale(15));

        g2d.setFont(pixelFontTiny.deriveFont(Font.BOLD, Game.scale(18)));
        g2d.setColor(new Color(255, 215, 0));
        String taskText = getTaskSummary(step);
        int textWidth = g2d.getFontMetrics().stringWidth(taskText);
        g2d.drawString(taskText, boxX + (boxWidth - textWidth) / 2, boxY + Game.scale(28));

        if (step.getType() == TutorialStep.StepType.KILL_ENEMIES) {
            g2d.setFont(pixelFontTiny.deriveFont(Game.scale(16)));
            g2d.setColor(Color.CYAN);
            String progress = "Killed: " + killCount + " / 3";
            int progressWidth = g2d.getFontMetrics().stringWidth(progress);
            g2d.drawString(progress, boxX + (boxWidth - progressWidth) / 2, boxY + Game.scale(50));
        } else if (step.getType() == TutorialStep.StepType.SHOOT) {
            g2d.setFont(pixelFontTiny.deriveFont(Game.scale(16)));
            g2d.setColor(Color.CYAN);
            String progress = "Arrows: " + projectilesFired + " / 5";
            int progressWidth = g2d.getFontMetrics().stringWidth(progress);
            g2d.drawString(progress, boxX + (boxWidth - progressWidth) / 2, boxY + Game.scale(50));
        } else {
            g2d.setFont(pixelFontTiny.deriveFont(Font.ITALIC, Game.scale(14)));
            g2d.setColor(new Color(200, 200, 200));
            String hint = "(Click to view)";
            int hintWidth = g2d.getFontMetrics().stringWidth(hint);
            g2d.drawString(hint, boxX + (boxWidth - hintWidth) / 2, boxY + Game.scale(55));
        }
    }

    private String getTaskSummary(TutorialStep step) {
        switch (step.getType()) {
            case MOVE: return "Move: WASD (all directions)";
            case SHOOT: return "Shoot 5 arrows";
            case DASH: return "Dash (SHIFT)";
            case EXPLOSION: return "Explosion (Q)";
            case KILL_ENEMIES: return "Kill 3 Enemies";
            default: return "Task";
        }
    }

    private void drawVisualHints(Graphics2D g2d, TutorialStep step) {
        switch (step.getType()) {
            case MOVE: drawMovementKeys(g2d); break;
            case SHOOT: drawMouseHint(g2d); break;
            case DASH: drawShiftKey(g2d); break;
            case EXPLOSION: drawQKey(g2d); break;
        }
    }

    private void drawMovementKeys(Graphics2D g2d) {
        int baseX = GamePanel.PANEL_WIDTH / 2 - Game.scale(80);
        int baseY = GamePanel.PANEL_HEIGHT - Game.scale(200);
        int keySize = Game.scale(50);
        int spacing = Game.scale(60);

        String[] keys = {"W", "A", "S", "D"};
        boolean[] pressed = {hasMovedUp, hasMovedLeft, hasMovedDown, hasMovedRight};
        int[][] positions = {
                {baseX + spacing, baseY},
                {baseX, baseY + spacing},
                {baseX + spacing, baseY + spacing},
                {baseX + spacing * 2, baseY + spacing}
        };

        for (int i = 0; i < keys.length; i++) {
            drawKey(g2d, keys[i], positions[i][0], positions[i][1], keySize, pressed[i]);
        }
    }

    private void drawMouseHint(Graphics2D g2d) {
        int x = GamePanel.PANEL_WIDTH / 2;
        int y = GamePanel.PANEL_HEIGHT - Game.scale(180);

        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.fillOval(x - Game.scale(30), y - Game.scale(30), Game.scale(60), Game.scale(60));
        g2d.setColor(new Color(255, 0, 0));
        g2d.setStroke(new BasicStroke(Game.scale(3)));
        g2d.drawOval(x - Game.scale(30), y - Game.scale(30), Game.scale(60), Game.scale(60));

        g2d.setFont(pixelFontSmall.deriveFont(Game.scale(16)));
        g2d.setColor(Color.BLACK);
        String text = "CLICK/HOLD";
        int textWidth = g2d.getFontMetrics().stringWidth(text);
        g2d.drawString(text, x - textWidth / 2, y + Game.scale(6));

        g2d.setFont(pixelFontTiny.deriveFont(Game.scale(16)));
        g2d.setColor(Color.CYAN);
        String count = projectilesFired + " / 5";
        int countWidth = g2d.getFontMetrics().stringWidth(count);
        g2d.drawString(count, x - countWidth / 2, y + Game.scale(60));
    }

    private void drawShiftKey(Graphics2D g2d) {
        int x = GamePanel.PANEL_WIDTH / 2 - Game.scale(60);
        int y = GamePanel.PANEL_HEIGHT - Game.scale(180);
        drawKey(g2d, "SHIFT", x, y, Game.scale(120), false);
    }

    private void drawQKey(Graphics2D g2d) {
        int x = GamePanel.PANEL_WIDTH / 2 - Game.scale(25);
        int y = GamePanel.PANEL_HEIGHT - Game.scale(180);
        drawKey(g2d, "Q", x, y, Game.scale(50), false);
    }

    private void drawKey(Graphics2D g2d, String key, int x, int y, int size, boolean highlighted) {
        Color bgColor = highlighted ? new Color(0, 200, 0, 230) : new Color(50, 50, 50, 230);
        g2d.setColor(bgColor);
        g2d.fillRoundRect(x, y, size, Game.scale(50), Game.scale(10), Game.scale(10));

        Color borderColor = highlighted ? new Color(0, 255, 0) : new Color(255, 215, 0);
        g2d.setColor(borderColor);
        g2d.setStroke(new BasicStroke(Game.scale(3)));
        g2d.drawRoundRect(x, y, size, Game.scale(50), Game.scale(10), Game.scale(10));

        g2d.setFont(pixelFont.deriveFont(Font.BOLD, Game.scale(24)));
        g2d.setColor(Color.WHITE);
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(key);
        int textHeight = fm.getHeight();
        g2d.drawString(key, x + (size - textWidth) / 2, y + (Game.scale(50) + textHeight) / 2 - Game.scale(5));
    }

    public void handleReminderBoxClick(int mouseX, int mouseY) {
        if (gameFrozen || showMainBox) return;

        int boxWidth = Game.scale(350);
        int boxHeight = Game.scale(80);
        int boxX = GamePanel.PANEL_WIDTH - boxWidth - Game.scale(20);
        int boxY = Game.scale(20);

        if (mouseX >= boxX && mouseX <= boxX + boxWidth &&
                mouseY >= boxY && mouseY <= boxY + boxHeight) {
            showMainBox = true;
            gameFrozen = true;
        }
    }

    public void nextStep() {
        if (currentStepIndex < steps.size() - 1) {
            currentStepIndex++;
            resetProgress();
            gameFrozen = true;
            showMainBox = true;
        } else {
            completeTutorial();
        }
    }

    private void resetProgress() {
        killCount = 0;
        projectilesFired = 0;
        hasMoved = false;
        hasMovedUp = false;
        hasMovedDown = false;
        hasMovedLeft = false;
        hasMovedRight = false;
        hasDashed = false;
        hasUsedExplosion = false;
    }

    public void completeTutorial() {
        tutorialComplete = true;
        tutorialActive = false;
        gameFrozen = false;
    }

    public void onPlayerMove(int keyCode) {
        if (keyCode == java.awt.event.KeyEvent.VK_W || keyCode == java.awt.event.KeyEvent.VK_UP) {
            hasMovedUp = true;
        }
        if (keyCode == java.awt.event.KeyEvent.VK_S || keyCode == java.awt.event.KeyEvent.VK_DOWN) {
            hasMovedDown = true;
        }
        if (keyCode == java.awt.event.KeyEvent.VK_A || keyCode == java.awt.event.KeyEvent.VK_LEFT) {
            hasMovedLeft = true;
        }
        if (keyCode == java.awt.event.KeyEvent.VK_D || keyCode == java.awt.event.KeyEvent.VK_RIGHT) {
            hasMovedRight = true;
        }
        hasMoved = true;
    }

    public void onProjectileFired() {
        projectilesFired++;
    }

    public void onPlayerDash() {
        hasDashed = true;
    }

    public void onPlayerExplosion() {
        hasUsedExplosion = true;
    }

    public void onEnemyKilled() {
        if (tutorialActive && currentStepIndex < steps.size()) {
            TutorialStep currentStep = steps.get(currentStepIndex);
            if (currentStep.getType() == TutorialStep.StepType.KILL_ENEMIES) {
                killCount++;
            }
        }
    }

    public void handleSpacePress() {
        if (!tutorialActive) return;

        TutorialStep currentStep = steps.get(currentStepIndex);

        if (currentStep.getType() == TutorialStep.StepType.INFO ||
                currentStep.getType() == TutorialStep.StepType.COMPLETE) {
            nextStep();
        } else {
            if (gameFrozen && showMainBox) {
                gameFrozen = false;
                showMainBox = false;
            }
        }
    }

    public boolean isTutorialActive() { return tutorialActive; }
    public boolean isTutorialComplete() { return tutorialComplete; }
    public boolean isGameFrozen() { return gameFrozen; }
    public TutorialStep getCurrentStep() {
        return tutorialActive && currentStepIndex < steps.size() ? steps.get(currentStepIndex) : null;
    }
}