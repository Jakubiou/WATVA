package WATVA;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The Bunny boss enemy with jumping and stomping attacks.
 */
public class BunnyBoss extends Enemy {
    private static final int BUNNY_SIZE = 128;
    private long lastJumpTime = 0;
    private static final long JUMP_INTERVAL = 1500;
    private static final int JUMP_DISTANCE = 100;
    private boolean isJumping = false;
    private boolean preJump = false;
    private boolean jumpingRight = true;

    private Image[] bunnyTexturesRight = new Image[6];
    private Image[] bunnyTexturesLeft = new Image[6];
    private int currentFrame = 0;
    private int animationSpeed = 5; // Faster animation speed
    private int animationCounter = 0;

    private int startX, startY;
    private int targetX, targetY;
    private double jumpProgress = 0;

    private long lastSpecialAttackTime = 0;
    private static final long SPECIAL_ATTACK_INTERVAL = 3000;
    private boolean isBurrowing = false;
    private static final long BURROW_DURATION = 1000;
    private boolean isStomping = false;
    private long stompStartTime = 0;
    private static final long STOMP_DURATION = 500;

    private boolean isJumpAttack = false;
    private int jumpAttackCount = 0;
    private static final int JUMP_ATTACK_MAX_COUNT = 5;
    private transient Image hpBarFrame1;

    /**
     * Creates a new Bunny boss at specified position with given health.
     * @param x The x-coordinate of the boss
     * @param y The y-coordinate of the boss
     * @param hp The initial health points of the boss
     */
    public BunnyBoss(int x, int y, int hp) {
        super(x, y, hp, Type.BUNNY_BOSS);
        this.baseSpeed = 0;

        try {
            for (int i = 0; i < 6; i++) {
                bunnyTexturesRight[i] = ImageIO.read(getClass().getResourceAsStream("watva/boss/bunny/bunny" + (i + 7) + ".png"));
            }
            bunnyTexturesLeft[0] = bunnyTexturesRight[0];
            for (int i = 0; i < 6; i++) {
                bunnyTexturesLeft[i] = ImageIO.read(getClass().getResourceAsStream("watva/boss/bunny/bunny" + (i + 1) + ".png"));
            }
            hpBarFrame1 = ImageIO.read(getClass().getResourceAsStream("watva/boss/bunny/BunnyHPBar1.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.startX = x;
        this.startY = y;
    }

    /**
     * Moves the boss towards the target position using jumping mechanics.
     * @param targetPlayerX The target x-coordinate
     * @param targetPlayerY The target y-coordinate
     */
    @Override
    public void moveTowards(int targetPlayerX, int targetPlayerY) {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastJumpTime >= JUMP_INTERVAL && !isJumping && !preJump && !isJumpAttack) {
            lastJumpTime = currentTime;
            preJump = true;
            currentFrame = 0;
            animationCounter = 0;

            int deltaX = targetPlayerX - x;
            int deltaY = targetPlayerY - y;
            double angle = Math.atan2(deltaY, deltaX);

            targetX = x + (int) (Math.cos(angle) * JUMP_DISTANCE);
            targetY = y + (int) (Math.sin(angle) * JUMP_DISTANCE);

            jumpingRight = targetX > x;
        }

        if (preJump) {
            animationCounter++;
            if (animationCounter >= animationSpeed) {
                animationCounter = 0;
                currentFrame++;

                if (currentFrame >= 2) {
                    preJump = false;
                    isJumping = true;
                    jumpProgress = 0;
                    currentFrame = 2;
                }
            }
        }

        if (isJumping) {
            jumpProgress += 0.05;

            x = (int) (startX + jumpProgress * (targetX - startX));
            y = (int) (startY + jumpProgress * (targetY - startY));

            if (jumpProgress >= 1) {
                isJumping = false;
                startX = x;
                startY = y;
                currentFrame = 0;

                if (isJumpAttack) {
                    jumpAttackCount++;
                    if (jumpAttackCount >= JUMP_ATTACK_MAX_COUNT) {
                        isJumpAttack = false;
                        jumpAttackCount = 0;
                    } else {
                        startJumpAttack(targetPlayerX, targetPlayerY);
                    }
                }
            }

            animationCounter++;
            if (animationCounter >= animationSpeed) {
                animationCounter = 0;
                currentFrame++;
                if (currentFrame > 5) {
                    currentFrame = 2;
                }
            }
        }
    }

    /**
     * Draws the boss with animations and health bar.
     * @param g The Graphics object to draw with
     */
    @Override
    public void draw(Graphics g) {
            Image currentTexture;
        Image[] currentTextures = jumpingRight ? bunnyTexturesRight : bunnyTexturesLeft;

        if (preJump || isJumping) {
            currentTexture = currentTextures[currentFrame];
        } else {
            currentTexture = currentTextures[0];
        }

        if (currentTexture != null) {
            g.drawImage(currentTexture, x, y, BUNNY_SIZE, BUNNY_SIZE, null);
        }


        if (isStomping) {
            g.setColor(new Color(255, 0, 0, 100));
            int shockwaveRadius = 200;
            g.fillOval(x - shockwaveRadius / 2 + BUNNY_SIZE / 2,
                    y - shockwaveRadius / 2 + BUNNY_SIZE / 2,
                    shockwaveRadius, shockwaveRadius);
        }
        if (this.hp > 0) {
            int hpBarWidth = (int)(300 * Game.getScaleFactor());
            int hpBarHeight = (int)(30 * Game.getScaleFactor());
            int hpBarX = (GamePanel.PANEL_WIDTH - hpBarWidth) / 2;
            int hpBarY = GamePanel.PANEL_HEIGHT - hpBarHeight - (int)(10 * Game.getScaleFactor());

            g.setColor(new Color(50, 50, 50));
            g.fillRect(hpBarX + GameLogic.cameraX, hpBarY + GameLogic.cameraY, hpBarWidth, hpBarHeight);

            g.setColor(Color.RED);
            int redWidth = (int)(Math.min(this.hp, 500) * hpBarWidth / 500);
            g.fillRect(hpBarX + GameLogic.cameraX, hpBarY + GameLogic.cameraY, redWidth, hpBarHeight);

            g.setColor(Color.BLACK);
            int numSections = 10;
            int sectionWidth = hpBarWidth / numSections;
            for (int i = 1; i < numSections; i++) {
                int sectionX = hpBarX + sectionWidth * i + GameLogic.cameraX;
                g.drawLine(sectionX, hpBarY + GameLogic.cameraY,
                        sectionX, hpBarY + hpBarHeight + GameLogic.cameraY);
            }

            g.setColor(Color.BLACK);
            g.drawRect(hpBarX + GameLogic.cameraX, hpBarY + GameLogic.cameraY, hpBarWidth, hpBarHeight);

            if (hpBarFrame1 != null) {
                int frameWidth = (int)((hpBarWidth + 96) * Game.getScaleFactor());
                int frameHeight = (int)((hpBarHeight * 4.4) * Game.getScaleFactor());
                int frameX = hpBarX + GameLogic.cameraX - (int)(54 * Game.getScaleFactor());
                int frameY = hpBarY + GameLogic.cameraY - (int)(58 * Game.getScaleFactor());

                g.drawImage(hpBarFrame1, frameX, frameY, frameWidth, frameHeight, null);
            }
        }
    }

    /**
     * @return The collision bounds of the boss
     */
    @Override
    public Rectangle getCollider() {
        return new Rectangle(x + 30, y + 24, BUNNY_SIZE - 48, BUNNY_SIZE - 48);
    }

    /**
     * Starts a jump attack sequence towards the target position.
     * @param targetPlayerX The target x-coordinate
     * @param targetPlayerY The target y-coordinate
     */
    private void startJumpAttack(int targetPlayerX, int targetPlayerY) {
        isJumpAttack = true;
        preJump = true;
        currentFrame = 0;
        animationCounter = 0;

        int deltaX = targetPlayerX - x;
        int deltaY = targetPlayerY - y;
        double angle = Math.atan2(deltaY, deltaX);

        targetX = x + (int) (Math.cos(angle) * JUMP_DISTANCE * 3);
        targetY = y + (int) (Math.sin(angle) * JUMP_DISTANCE * 3);

        jumpingRight = targetX > x;
    }

    /**
     * Updates boss behavior and attack patterns.
     * @param player The player target
     * @param enemies The enemy list (unused in this implementation)
     */
    public void updateBossBehavior(Player player, CopyOnWriteArrayList<Enemy> enemies) {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastSpecialAttackTime >= SPECIAL_ATTACK_INTERVAL) {
            int attackType = (int) (Math.random() * 2);
            switch (attackType) {
                case 0 -> startGroundStomp();
                case 1 -> startJumpAttack(player.getX(), player.getY());
            }
            lastSpecialAttackTime = currentTime;
        }

        if (isStomping) {
            groundStomp(player);
        }

        if (!isBurrowing && !isStomping && !isJumpAttack) {
            moveTowards(player.getX(), player.getY());
        }
    }

    /**
     * Initiates the ground stomp attack.
     */
    private void startGroundStomp() {
        isStomping = true;
        stompStartTime = System.currentTimeMillis();
    }

    /**
     * Performs the ground stomp attack that damages nearby players.
     * @param player The player to damage
     */
    private void groundStomp(Player player) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - stompStartTime < STOMP_DURATION) {
            double distance = Math.sqrt(Math.pow(player.getX() - x, 2) + Math.pow(player.getY() - y, 2));
            if (distance <= 200) {
                player.hit(2);
            }
        } else {
            isStomping = false;
        }
    }
}