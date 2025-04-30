package WATVA;

import java.awt.*;
import java.awt.geom.Arc2D;

public class MeleeAttack {
    private int x, y;
    private int angle;
    private int radius = 100;

    public MeleeAttack(int x, int y, int angle) {
        this.x = x;
        this.y = y;
        this.angle = angle;
    }

    public void setPosition(int x, int y, int angle) {
        this.x = x;
        this.y = y;
        this.angle = angle;
    }

    public void draw(Graphics2D g) {
        g.setColor(Color.GRAY);
        int adjustedAngle = 360 - angle - 45;
        g.fill(new Arc2D.Double(x - radius, y - radius, radius * 2, radius * 2, adjustedAngle, 90, Arc2D.PIE));
    }


    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getAngle() {
        return angle;
    }

    public int getRadius() {
        return radius;
    }
}
