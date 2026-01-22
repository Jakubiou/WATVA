package Player;

import java.awt.*;

/**
 * Represents an explosion effect in the game with visual and damage capabilities.
 * Handles explosion animation, damage range calculation, and visual rendering.
 */
public class Explosion {
    private int x, y;
    private int currentRadius = 0;
    private int maxRadius;
    private long startTime;
    private int duration = 500;
    private boolean hasDamaged = false;

    /**
     * Creates a new explosion at specified coordinates with maximum radius.
     *
     * @param x The x-coordinate of explosion center
     * @param y The y-coordinate of explosion center
     * @param maxRadius The maximum radius the explosion will reach
     */
    public Explosion(int x, int y, int maxRadius) {
        this.x = x;
        this.y = y;
        this.maxRadius = maxRadius;
        this.startTime = System.currentTimeMillis();
    }

    /**
     * Checks if the explosion animation is complete.
     *
     * @return true if explosion duration has elapsed, false otherwise
     */
    public boolean isComplete() {
        return System.currentTimeMillis() - startTime > duration;
    }

    /**
     * Updates the explosion's current radius based on elapsed time.
     * Should be called each frame to animate the explosion.
     */
    public void update() {
        long elapsedTime = System.currentTimeMillis() - startTime;
        currentRadius = (int) ((elapsedTime / (double) duration) * maxRadius);
    }

    /**
     * Draws the explosion effect with color changing over time.
     * Starts red and fades to yellow as the explosion progresses.
     *
     * @param g The Graphics context to render to
     */
    public void draw(Graphics g) {
        long elapsedTime = System.currentTimeMillis() - startTime;
        float progress = Math.min(elapsedTime / (float) duration, 1.0f);

        int r = 255;
        int gValue = Math.max(0, (int) (255 * (1 - progress)));
        int b = 0;

        g.setColor(new Color(r, gValue, b, 100));
        g.fillOval(x - currentRadius, y - currentRadius, currentRadius * 2, currentRadius * 2);
    }

    /**
     * Checks if a target position is within the explosion's current damage radius.
     *
     * @param targetX The x-coordinate to check
     * @param targetY The y-coordinate to check
     * @return true if position is within damage range, false otherwise
     */
    public boolean isInRange(int targetX, int targetY) {
        int dx = x - targetX;
        int dy = y - targetY;
        return dx * dx + dy * dy <= currentRadius * currentRadius;
    }

    public boolean hasDamaged() {
        return hasDamaged;
    }

    public void setDamaged(boolean damaged) {
        this.hasDamaged = damaged;
    }
}