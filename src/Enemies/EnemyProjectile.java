package Enemies;

import Core.Game;
import Logic.GameLogic;
import Logic.WallManager;
import Player.Player;

import java.awt.*;

public class EnemyProjectile {
    private double x, y;
    private double directionX, directionY;
    private double speed = Game.scale(0.1);
    private boolean active = true;
    private static final int SIZE = Game.scale(12);

    private static WallManager wallManager;

    /**
     * Set global wall manager for all projectiles
     */
    public static void setWallManager(WallManager wm) {
        wallManager = wm;
    }

    /**
     * Creates a new enemy projectile that moves toward the player.
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

    public void move() {
        x += directionX * speed;
        y += directionY * speed;

        if (wallManager != null && wallManager.isWall((int)x, (int)y)) {
            active = false;
        }
    }

    public boolean isActive() {
        return active;
    }

    /**
     * Checks for collision with the player.
     */
    public boolean checkCollisionWithPlayer(Player player) {
        if (this.getCollider().intersects(player.getCollider())) {
            active = false;
            return true;
        }
        return false;
    }

    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        int screenX = (int)x - GameLogic.cameraX;
        int screenY = (int)y - GameLogic.cameraY;

        g2d.setColor(new Color(255, 100, 100, 100));
        g2d.fillOval(screenX - SIZE, screenY - SIZE, SIZE * 2, SIZE * 2);

        g2d.setColor(new Color(255, 0, 0));
        g2d.fillOval(screenX - SIZE/2, screenY - SIZE/2, SIZE, SIZE);

        g2d.setColor(new Color(255, 200, 200));
        g2d.fillOval(screenX - SIZE/4, screenY - SIZE/4, SIZE/2, SIZE/2);
    }

    public Rectangle getCollider() {
        return new Rectangle((int)x - SIZE/2, (int)y - SIZE/2, SIZE, SIZE);
    }
}