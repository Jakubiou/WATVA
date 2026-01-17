package WATVA;

import java.awt.*;

/**
 * Projectile fired by the Dark Mage boss with fading effect.
 */
public class DarkMageProjectile {
    private int x, y;
    private double dx, dy;
    private boolean active = true;
    private static final int SIZE = 20;
    private static final int SPEED = 10;
    private int distanceTraveled = 0;
    private Color color = new Color(150, 0, 200);
    private boolean isFading = false;
    private float fadeAlpha = 1.0f;
    private static final float FADE_SPEED = 0.1f;

    /**
     * Creates a new projectile at specified position with given direction.
     * @param startX The starting x-coordinate
     * @param startY The starting y-coordinate
     * @param dirX The x-direction component
     * @param dirY The y-direction component
     */
    public DarkMageProjectile(int startX, int startY, double dirX, double dirY) {
        this.x = startX;
        this.y = startY;
        this.dx = dirX * SPEED;
        this.dy = dirY * SPEED;
    }

    /**
     * Updates the projectile position and fade state.
     */
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

    /**
     * Draws the projectile with glow and fade effects.
     * @param g The Graphics object to draw with
     */
    public void draw(Graphics g) {
        if (!active) return;

        Graphics2D g2d = (Graphics2D)g.create();

        if (!isFading) {
            g2d.setColor(new Color(200, 0, 255));
            g2d.fillOval(x - SIZE, y - SIZE, SIZE * 2, SIZE* 2);
        }

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha));
        g2d.setColor(color);
        g2d.fillOval(x - SIZE / 2, y - SIZE / 2, SIZE, SIZE);

        g2d.dispose();
    }

    /**
     * @return The collision bounds of the projectile
     */
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