package WATVA;

import java.awt.*;

public class EnemyProjectile {
    private double x, y;
    private double directionX, directionY;
    private double speed = 10;
    private boolean active = true;
    private static final int MAP_WIDTH = 6120;
    private static final int MAP_HEIGHT = 3200;

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

        if (x < 0 || x > MAP_WIDTH || y < 0 || y > MAP_HEIGHT) {
            active = false;
        }
    }

    public boolean isActive() {
        return active;
    }

    public boolean checkCollisionWithPlayer(Player player) {
        if (this.getCollider().intersects(player.getCollider())) {
            active = false;
            return true;
        }
        return false;
    }

    public void draw(Graphics g) {
        g.setColor(Color.RED);
        g.fillOval((int)x - GamePanel.cameraX, (int)y - GamePanel.cameraY, 10, 10);
    }

    public Rectangle getCollider() {
        return new Rectangle((int)x, (int)y, 10, 10);
    }
}