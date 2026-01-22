package Logic.DamageNumber;

import java.awt.*;
import java.util.Random;

/**
 * Represents a floating damage number that appears when an enemy takes damage.
 * The number floats upward at a slight random angle and fades out over time with color based on damage amount.
 * Numbers appear at random positions around the damage source for visual variety.
 */
public class DamageNumber {
    private double x, y;
    private int damage;
    private long creationTime;
    private static final long DURATION_MS = 1500;
    private static final int FLOAT_SPEED = 2;
    private boolean isActive = true;
    private static final Random random = new Random();

    private static final int RANDOM_OFFSET_RANGE = 40;

    private double velocityX;
    private double velocityY;
    private static final double MAX_ANGLE_RADIANS = Math.PI / 12;

    /**
     * Creates a new damage number at a random position around the specified coordinates.
     *
     * @param centerX The center x-coordinate where damage occurred
     * @param centerY The center y-coordinate where damage occurred
     * @param damage The amount of damage dealt
     */
    public DamageNumber(int centerX, int centerY, int damage) {
        int offsetX = random.nextInt(RANDOM_OFFSET_RANGE * 2) - RANDOM_OFFSET_RANGE;
        int offsetY = random.nextInt(RANDOM_OFFSET_RANGE) - RANDOM_OFFSET_RANGE / 2;

        this.x = centerX + offsetX;
        this.y = centerY + offsetY;
        this.damage = damage;
        this.creationTime = System.currentTimeMillis();

        double angle = (random.nextDouble() - 0.5) * 2 * MAX_ANGLE_RADIANS;
        this.velocityX = Math.sin(angle) * FLOAT_SPEED;
        this.velocityY = -Math.cos(angle) * FLOAT_SPEED;
    }



    /**
     * Updates the damage number position and checks if it should still be active.
     */
    public void update() {
        x += velocityX;
        y += velocityY;

        long currentTime = System.currentTimeMillis();
        if (currentTime - creationTime >= DURATION_MS) {
            isActive = false;
        }
    }

    /**
     * Draws the damage number with appropriate color and transparency.
     *
     * @param g Graphics context to draw with
     */
    public void draw(Graphics g) {
        if (!isActive) return;

        Graphics2D g2d = (Graphics2D) g;
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - creationTime;
        float alpha = 1.0f - (float) elapsed / DURATION_MS;
        alpha = Math.max(0, Math.min(1, alpha));

        Color baseColor = getDamageColor(damage);
        Color colorWithAlpha = new Color(
                baseColor.getRed(),
                baseColor.getGreen(),
                baseColor.getBlue(),
                (int) (255 * alpha)
        );

        Color outlineColor = new Color(0, 0, 0, (int) (255 * alpha));

        Font font = new Font("Arial", Font.BOLD, getFontSize(damage));
        g2d.setFont(font);

        FontMetrics fm = g2d.getFontMetrics();
        String damageText = String.valueOf(damage);
        int textWidth = fm.stringWidth(damageText);
        int textHeight = fm.getHeight();

        int drawX = (int)(x - textWidth / 2);
        int drawY = (int)(y + textHeight / 4);

        g2d.setColor(outlineColor);
        g2d.drawString(damageText, drawX - 1, drawY);
        g2d.drawString(damageText, drawX + 1, drawY);
        g2d.drawString(damageText, drawX, drawY - 1);
        g2d.drawString(damageText, drawX, drawY + 1);

        g2d.setColor(colorWithAlpha);
        g2d.drawString(damageText, drawX, drawY);
    }

    /**
     * Determines the color based on damage amount.
     *
     * @param damage The damage amount
     * @return Color for the damage number
     */
    private Color getDamageColor(int damage) {
        if (damage <= 10) {
            return Color.WHITE;
        } else if (damage <= 25) {
            return Color.YELLOW;
        } else if (damage <= 50) {
            return Color.ORANGE;
        } else if (damage <= 100) {
            return Color.RED;
        } else {
            return Color.MAGENTA;
        }
    }

    /**
     * Determines the font size based on damage amount.
     *
     * @param damage The damage amount
     * @return Font size for the damage number
     */
    private int getFontSize(int damage) {
        if (damage <= 10) {
            return 16;
        } else if (damage <= 25) {
            return 18;
        } else if (damage <= 50) {
            return 20;
        } else if (damage <= 100) {
            return 22;
        } else {
            return 24;
        }
    }

    /**
     * Checks if the damage number is still active.
     *
     * @return true if still active, false if should be removed
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Gets the x-coordinate of the damage number.
     *
     * @return x-coordinate
     */
    public int getX() {
        return (int)x;
    }

    /**
     * Gets the y-coordinate of the damage number.
     *
     * @return y-coordinate
     */
    public int getY() {
        return (int)y;
    }

    /**
     * Gets the damage value.
     *
     * @return damage amount
     */
    public int getDamage() {
        return damage;
    }
}