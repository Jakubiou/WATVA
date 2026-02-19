package Bosses;

import java.awt.*;

public class MeteorZone {
    int x, y, radius;
    long createdTime;
    boolean hasDealtDamage = false;
    static final int WARN_TIME = 1000;
    static final int EXPLODE_TIME = 500;

    int currentExplosionFrame = 0;
    long lastExplosionFrameTime = 0;
    static final long EXPLOSION_FRAME_DURATION = 80;

    public MeteorZone(int x, int y, int radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.createdTime = System.currentTimeMillis();
    }

    public void update() {
        long age = System.currentTimeMillis() - createdTime;

        if (age >= WARN_TIME && age < (WARN_TIME + EXPLODE_TIME)) {
            long explosionAge = age - WARN_TIME;
            currentExplosionFrame = (int)(explosionAge / EXPLOSION_FRAME_DURATION);
            if (currentExplosionFrame >= 6) currentExplosionFrame = 5;
        }
    }

    public boolean shouldDealDamage() {
        long age = System.currentTimeMillis() - createdTime;
        return age >= WARN_TIME && age < (WARN_TIME + EXPLODE_TIME);
    }

    public boolean isFinished() {
        return System.currentTimeMillis() - createdTime > (WARN_TIME + EXPLODE_TIME);
    }

    public void draw(Graphics g, Image[] meteorExplosionFrames) {
        long age = System.currentTimeMillis() - createdTime;
        Graphics2D g2d = (Graphics2D) g;

        if (age < WARN_TIME) {
            float alpha = Math.min(1.0f, age / (float) WARN_TIME);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.5f));
            g2d.setColor(DarkMageBoss.COLOR_METEOR_WARN);
            g2d.fillOval(x, y, radius * 2, radius * 2);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        } else if (shouldDealDamage()) {
            if (meteorExplosionFrames[currentExplosionFrame] != null) {
                int size = radius * 2;
                g2d.drawImage(meteorExplosionFrames[currentExplosionFrame], x, y, size, size, null);
            } else {
                g2d.setColor(new Color(150, 0, 200, 200));
                g2d.fillOval(x, y, radius * 2, radius * 2);
            }
        }
    }
}
