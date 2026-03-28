package Pets;

import Core.Game;
import Logic.GameLogic;
import java.awt.*;

/**
 * Projectile fired by attacking pets (Emberwolf, Shadowcat, etc.)
 */
public class PetAttackProjectile {
    private double x, y;
    private double vx, vy;
    private int damage;
    private boolean active = true;
    private final Pet.PetType petType;
    private double distanceTravelled = 0;
    private static final double MAX_DIST = Game.scale(600);
    private static final int SIZE = Game.scale(12);

    public PetAttackProjectile(double startX, double startY, double targetX, double targetY,
                               int damage, Pet.PetType petType) {
        this.x = startX;
        this.y = startY;
        this.damage = damage;
        this.petType = petType;

        double dx = targetX - startX;
        double dy = targetY - startY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        double speed = Game.scale(7);
        if (dist > 0) {
            vx = (dx / dist) * speed;
            vy = (dy / dist) * speed;
        }
    }

    public void update() {
        x += vx;
        y += vy;
        distanceTravelled += Math.sqrt(vx * vx + vy * vy);
        if (distanceTravelled >= MAX_DIST) active = false;
    }

    public void draw(Graphics g) {
        if (!active) return;
        int sx = (int) x - GameLogic.cameraX;
        int sy = (int) y - GameLogic.cameraY;

        Color inner, outer;
        switch (petType) {
            case EMBERWOLF -> { inner = new Color(255, 120, 0); outer = new Color(255, 60, 0, 100); }
            case SHADOWCAT -> { inner = new Color(120, 0, 200); outer = new Color(80, 0, 150, 100); }
            case STORMBIRD -> { inner = new Color(200, 230, 255); outer = new Color(100, 180, 255, 100); }
            case DRAGONET  -> { inner = new Color(255, 80, 0); outer = new Color(200, 30, 0, 120); }
            default        -> { inner = Color.WHITE; outer = new Color(200, 200, 200, 80); }
        }

        // Glow efekt
        g.setColor(outer);
        ((Graphics2D)g).fillOval(sx - SIZE, sy - SIZE, SIZE * 2, SIZE * 2);
        g.setColor(inner);
        ((Graphics2D)g).fillOval(sx - SIZE/2, sy - SIZE/2, SIZE, SIZE);
    }

    public boolean isActive() { return active; }
    public void setActive(boolean b) { active = b; }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getDamage() { return damage; }

    public Rectangle getCollider() {
        return new Rectangle((int)x - SIZE/2, (int)y - SIZE/2, SIZE, SIZE);
    }
}