package WATVA;

import java.awt.*;

public class Explosion {
    private int x, y;
    private int currentRadius = 0;
    private int maxRadius;
    private long startTime;
    private int duration = 500;
    private boolean hasDamaged = false;

    public Explosion(int x, int y, int maxRadius) {
        this.x = x;
        this.y = y;
        this.maxRadius = maxRadius;
        this.startTime = System.currentTimeMillis();
    }

    public boolean isComplete() {
        return System.currentTimeMillis() - startTime > duration;
    }

    public void update() {
        long elapsedTime = System.currentTimeMillis() - startTime;
        currentRadius = (int) ((elapsedTime / (double) duration) * maxRadius);
    }

    public void draw(Graphics g) {
        long elapsedTime = System.currentTimeMillis() - startTime;
        float progress = Math.min(elapsedTime / (float) duration, 1.0f);

        int r = 255;
        int gValue = Math.max(0, (int) (255 * (1 - progress)));
        int b = 0;

        g.setColor(new Color(r, gValue, b, 100));
        g.fillOval(x - currentRadius, y - currentRadius, currentRadius * 2, currentRadius * 2);
    }

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