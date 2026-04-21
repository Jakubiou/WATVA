package Bosses;

import Core.Game;
import java.awt.*;

public class DarkMageProjectile {
    private int x, y;
    private double dx, dy;
    private boolean active = true;
    private static final int SIZE = Game.scale(15);
    private static final int SPEED = Game.scale(6);
    private int distanceTraveled = 0;
    private boolean isFading = false;
    private float fadeAlpha = 1.0f;
    private static final float FADE_SPEED = 0.1f;

    public DarkMageProjectile(int startX, int startY, double dirX, double dirY) {
        this.x = startX;
        this.y = startY;
        this.dx = dirX * SPEED;
        this.dy = dirY * SPEED;
    }

    public void update() {
        if (!active) return;

        if (isFading) {
            fadeAlpha -= FADE_SPEED;
            if (fadeAlpha <= 0) {
                active = false;
                return;
            }
        }

        x += dx;
        y += dy;
        distanceTraveled += SPEED;
    }

    private static final Color COLOR_OUTER = new Color(200, 0, 255);
    private static final Color COLOR_INNER = new Color(150, 0, 200);

    public void draw(Graphics g) {
        if (!active) return;
        Graphics2D g2d = (Graphics2D) g;

        if (!isFading) {
            g2d.setColor(COLOR_OUTER);
            g2d.fillOval(x - SIZE, y - SIZE, SIZE * 2, SIZE * 2);
            g2d.setColor(COLOR_INNER);
            g2d.fillOval(x - SIZE / 2, y - SIZE / 2, SIZE, SIZE);
        } else {
            float safeAlpha = Math.min(1f, Math.max(0f, fadeAlpha));
            Composite saved = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, safeAlpha));
            g2d.setColor(COLOR_INNER);
            g2d.fillOval(x - SIZE / 2, y - SIZE / 2, SIZE, SIZE);
            g2d.setComposite(saved);
        }
    }

    public Rectangle getCollider() {
        return new Rectangle(x - SIZE / 2, y - SIZE / 2, SIZE, SIZE);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}