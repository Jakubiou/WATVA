package WATVA;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Represents a Slime enemy in the game.
 * Implements unique jumping movement behavior and animations.
 * Extends the base Enemy class with slime-specific characteristics.
 */
public class Slime extends Enemy {
    private static final int SLIME_SIZE = 64;
    private long lastJumpTime = 0;
    private static final long JUMP_INTERVAL = 1500;
    private static final int JUMP_DISTANCE = 50;
    private boolean isJumping = false;
    private boolean preJump = false;
    private boolean jumpingRight = true;

    private Image[] slimeTexturesRight = new Image[6];
    private Image[] slimeTexturesLeft = new Image[6];
    private int currentFrame = 0;
    private int animationSpeed = 5;
    private int animationCounter = 0;

    private int startX, startY;
    private int targetX, targetY;
    private double jumpProgress = 0;

    /**
     * Creates a new Slime enemy.
     *
     * @param x Initial x-coordinate
     * @param y Initial y-coordinate
     * @param hp Initial health points
     */
    public Slime(int x, int y, int hp) {
        super(x, y, hp, Type.SLIME);
        this.baseSpeed = 0;

        try {
            for (int i = 0; i < 6; i++) {
                slimeTexturesRight[i] = ImageIO.read(getClass().getResourceAsStream("/WATVA/Enemy/Slime/Slime" + (i + 1) + ".png"));
            }
            slimeTexturesLeft[0] = slimeTexturesRight[0];
            for (int i = 1; i < 6; i++) {
                slimeTexturesLeft[i] = ImageIO.read(getClass().getResourceAsStream("/WATVA/Enemy/Slime/Slime" + (i + 6) + ".png"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.startX = x;
        this.startY = y;
    }

    /**
     * Moves the slime towards the target player position using jumping behavior.
     *
     * @param targetPlayerX Player's x-coordinate
     * @param targetPlayerY Player's y-coordinate
     */
    @Override
    public void moveTowards(int targetPlayerX, int targetPlayerY) {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastJumpTime >= JUMP_INTERVAL && !isJumping && !preJump) {
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
     * Draws the slime at its current position with appropriate animation frame.
     *
     * @param g Graphics context to draw to
     */
    @Override
    public void draw(Graphics g) {
        Image currentTexture;
        Image[] currentTextures = jumpingRight ? slimeTexturesRight : slimeTexturesLeft;

        if (preJump || isJumping) {
            currentTexture = currentTextures[currentFrame];
        } else {
            currentTexture = currentTextures[0];
        }

        if (currentTexture != null) {
            g.drawImage(currentTexture, x, y, SLIME_SIZE, SLIME_SIZE, null);
        }
    }

    /**
     * Gets the collision bounds of the slime.
     *
     * @return Rectangle representing the collision area
     */
    @Override
    public Rectangle getCollider() {
        return new Rectangle(x + 30, y + 24, SLIME_SIZE - 48, SLIME_SIZE - 48);
    }
}
