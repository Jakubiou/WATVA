package WATVA;

import java.awt.*;

/**
 * Represents a projectile fired by enemies in the game.
 * Handles movement, collision detection, and rendering of enemy projectiles.
 */
public class EnemyProjectile {
    private double x, y;
    private double directionX, directionY;
    private double speed = 10;
    private boolean active = true;
    private static final int MAP_WIDTH = GamePanel.PANEL_WIDTH * 4;
    private static final int MAP_HEIGHT = GamePanel.PANEL_HEIGHT * 4;

    /**
     * Creates a new enemy projectile that moves toward the player.
     *
     * @param startX The initial x-coordinate of the projectile
     * @param startY The initial y-coordinate of the projectile
     * @param playerX The x-coordinate of the player target
     * @param playerY The y-coordinate of the player target
     */
    public EnemyProjectile(int startX, int startY, int playerX, int playerY) {
        this.x = startX;
        this.y = startY;

        double deltaX = playerX - startX;
        double deltaY = playerY - startY;
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

        if (distance > 0) {
            directionX = deltaX / distance;
            directionY = deltaY / distance;
        }
    }

    /**
     * Updates the projectile's position each frame.
     * Deactivates the projectile if it goes out of bounds.
     */
    public void move() {
        x += directionX * speed;
        y += directionY * speed;

        if (x < 0 || x > MAP_WIDTH || y < 0 || y > MAP_HEIGHT) {
            active = false;
        }
    }

    /**
     * Checks if the projectile is still active (in bounds and not collided).
     *
     * @return true if the projectile is active, false otherwise
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Checks for collision with the player.
     *
     * @param player The player to check collision against
     * @return true if collision occurred, false otherwise
     */
    public boolean checkCollisionWithPlayer(Player player) {
        if (this.getCollider().intersects(player.getCollider())) {
            active = false;
            return true;
        }
        return false;
    }

    /**
     * Draws the projectile on screen.
     * Adjusts position based on camera view.
     *
     * @param g The Graphics context to render to
     */
    public void draw(Graphics g) {
        g.setColor(Color.RED);
        g.fillOval((int)x - GameLogic.cameraX, (int)y - GameLogic.cameraY, 10, 10);
    }

    /**
     * Gets the collision bounds of the projectile.
     *
     * @return Rectangle representing the projectile's collision area
     */
    public Rectangle getCollider() {
        return new Rectangle((int)x, (int)y, 10, 10);
    }
}