package Player;

import Core.Game;
import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;

/**
 * Represents a projectile fired by the player.
 * Handles projectile movement, animation, and special effects.
 */
public class PlayerProjectile {
    public static final int SIZE = Game.scale(20);
    private int x, y;
    private double velocityX, velocityY;
    private int speed = Game.scale(10);
    private double distanceTravelled = 0;
    private static final double MAX_DISTANCE = 30000;
    private Image[] bulletTextures;
    private int currentFrame = 0;
    private long lastFrameChange = 0;
    private long frameDuration = 100;
    private int pierceCount;
    private int fireDamageLevel;
    private boolean slowEffect;
    private boolean canRicochet = false;
    private int ricochetCount = 0;
    private static final int MAX_RICOCHET = 2;

    /**
     * Creates a new player projectile.
     *
     * @param x Starting X position
     * @param y Starting Y position
     * @param targetX Target X position (for direction)
     * @param targetY Target Y position (for direction)
     * @param piercingLevel Number of additional enemies projectile can pierce
     * @param fireLevel Level of additional fire damage
     * @param hasSlowEffect Whether projectile slows enemies
     */
    public PlayerProjectile(int x, int y, int targetX, int targetY, int piercingLevel, int fireLevel, boolean hasSlowEffect) {
        this(x, y, targetX, targetY, piercingLevel, fireLevel, hasSlowEffect, 0);
    }

    public PlayerProjectile(int x, int y, int targetX, int targetY, int piercingLevel, int fireLevel, boolean hasSlowEffect, int bulletSpeedLevel) {
        this.x = x;
        this.y = y;
        this.pierceCount = 1 + piercingLevel;
        this.fireDamageLevel = fireLevel;
        this.slowEffect = hasSlowEffect;
        this.speed = Game.scale(10 + bulletSpeedLevel * 4); // +4 per level

        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 0) {
            this.velocityX = (dx / distance) * speed;
            this.velocityY = (dy / distance) * speed;
        } else {
            this.velocityX = speed;
            this.velocityY = 0;
        }

        bulletTextures = new Image[4];
        try {
            bulletTextures[0] = ImageIO.read(getClass().getResourceAsStream("/WATVA/Projectiles/Basic_player_projectile1.png"));
            bulletTextures[1] = ImageIO.read(getClass().getResourceAsStream("/WATVA/Projectiles/Basic_player_projectile2.png"));
            bulletTextures[2] = ImageIO.read(getClass().getResourceAsStream("/WATVA/Projectiles/Basic_player_projectile3.png"));
            bulletTextures[3] = ImageIO.read(getClass().getResourceAsStream("/WATVA/Projectiles/Basic_player_projectile4.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates projectile position and animation.
     *
     * @return true if projectile should be removed (out of bounds, max distance, or no pierce left)
     */
    public boolean move() {
        x += velocityX;
        y += velocityY;
        distanceTravelled += speed;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameChange >= frameDuration) {
            currentFrame = (currentFrame + 1) % bulletTextures.length;
            lastFrameChange = currentTime;
        }

        return distanceTravelled >= MAX_DISTANCE || pierceCount <= 0;
    }

    public void setPierceCount(int count) {
        pierceCount = count;
    }

    public int getPierceCount() {
        return pierceCount;
    }

    public void draw(Graphics g) {
        g.drawImage(bulletTextures[currentFrame], x, y, SIZE, SIZE, null);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getFireDamageLevel() {
        return fireDamageLevel;
    }

    public boolean hasSlowEffect() {
        return slowEffect;
    }

    public void setCanRicochet(boolean r) { canRicochet = r; }
    public boolean canRicochet() { return canRicochet; }
    public int getRicochetCount() { return ricochetCount; }
    // Called when hitting a wall — bounces and returns true if bounced, false if should die
    public boolean tryRicochet(Logic.World.WallManager wallManager) {
        if (!canRicochet || ricochetCount >= MAX_RICOCHET) return false;
        ricochetCount++;
        // Test which axis is blocked, reverse that velocity
        int nx = (int)(x + velocityX);
        int ny = (int)(y + velocityY);
        boolean wallX = wallManager.isWall(nx + SIZE/2, y + SIZE/2);
        boolean wallY = wallManager.isWall(x + SIZE/2, ny + SIZE/2);
        if (wallX) velocityX = -velocityX;
        if (wallY) velocityY = -velocityY;
        if (!wallX && !wallY) velocityX = -velocityX; // corner fallback
        return true;
    }}