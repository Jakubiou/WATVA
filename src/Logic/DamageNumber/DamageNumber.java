package Logic.DamageNumber;

import Core.Game;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.Random;

public class DamageNumber {
    private double x, y;
    private int damage;
    private boolean isCrit;
    private long creationTime;
    private static final long DURATION_MS = 1600;
    private static final Random random = new Random();

    private double velocityX;
    private double velocityY;
    private double rotation;
    private double rotationSpeed;
    private int style;

    public DamageNumber(int centerX, int centerY, int damage) {
        this(centerX, centerY, damage, false);
    }

    public DamageNumber(int centerX, int centerY, int damage, boolean isCrit) {
        int off = Game.scale(30);
        this.x = centerX + random.nextInt(off * 2) - off;
        this.y = centerY + random.nextInt(off) - off / 2;
        this.damage = damage;
        this.isCrit = isCrit;
        this.creationTime = System.currentTimeMillis();

        double angle = (random.nextDouble() - 0.5) * Math.PI / 5;
        double spd = Game.scale(isCrit ? 3.5 : 2.2);
        this.velocityX = Math.sin(angle) * spd;
        this.velocityY = -Math.cos(angle) * spd;

        this.rotation = (random.nextDouble() - 0.5) * 0.3;
        this.rotationSpeed = (random.nextDouble() - 0.5) * 0.12;

        if (isCrit) style = 1;
        else if (damage >= 50) style = 2;
        else style = 0;
    }

    public void update() {
        velocityY += Game.scale(0.07);
        x += velocityX;
        y += velocityY;
        rotation += rotationSpeed;
        rotationSpeed *= 0.93;
    }

    public boolean isActive() {
        return System.currentTimeMillis() - creationTime < DURATION_MS;
    }

    public void draw(Graphics g) {
        if (!isActive()) return;
        Graphics2D g2d = (Graphics2D) g;

        long elapsed = System.currentTimeMillis() - creationTime;
        float t = elapsed / (float) DURATION_MS;

        float alpha;
        if (t < 0.1f) alpha = t / 0.1f;
        else alpha = 1f - ((t - 0.1f) / 0.9f);
        alpha = Math.max(0f, Math.min(1f, alpha));

        float scale;
        if (t < 0.15f) {
            scale = (float)(1.4 * Math.sin((t / 0.15f) * Math.PI / 2));
        } else if (t < 0.28f) {
            scale = (float)(1.4 - 0.45 * ((t - 0.15f) / 0.13f));
        } else {
            scale = 1.0f - (t - 0.28f) * 0.35f;
        }
        scale = Math.max(0.05f, scale);
        if (isCrit) scale *= 1.5f;

        int fontSize = getFontSize();
        Font font = new Font("Arial", Font.BOLD, Math.max(8, (int)(fontSize * scale)));
        g2d.setFont(font);
        String text = buildText();

        AffineTransform savedTx = g2d.getTransform();
        Composite savedComposite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2d.rotate(rotation, x, y);

        FontMetrics fm = g2d.getFontMetrics();
        int tw = fm.stringWidth(text);
        int drawX = (int)x - tw / 2;
        int drawY = (int)y;

        g2d.setColor(new Color(0, 0, 0, (int)(160 * alpha)));
        g2d.drawString(text, drawX + Game.scale(2), drawY + Game.scale(2));

        Color outline = getOutlineColor();
        g2d.setColor(new Color(outline.getRed(), outline.getGreen(), outline.getBlue(), (int)(255 * alpha)));
        int out = Math.max(1, Game.scale(2));
        g2d.drawString(text, drawX - out, drawY);
        g2d.drawString(text, drawX + out, drawY);
        g2d.drawString(text, drawX, drawY - out);
        g2d.drawString(text, drawX, drawY + out);

        if (isCrit) {
            float hue = (elapsed % 500) / 500f;
            Color shimmer = Color.getHSBColor(hue, 1f, 1f);
            g2d.setColor(new Color(shimmer.getRed(), shimmer.getGreen(), shimmer.getBlue(), (int)(255 * alpha)));
        } else {
            Color base = getDamageColor();
            g2d.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), (int)(255 * alpha)));
        }
        g2d.drawString(text, drawX, drawY);

        g2d.setTransform(savedTx);
        g2d.setComposite(savedComposite);
    }

    private String buildText() {
        if (isCrit) {
            return damage + "!";
        }
        if (damage >= 50) return damage + "!";
        return String.valueOf(damage);
    }

    private Color getDamageColor() {
        if (damage <= 5)   return new Color(190, 190, 190);
        if (damage <= 15)  return Color.WHITE;
        if (damage <= 30)  return Color.YELLOW;
        if (damage <= 60)  return new Color(255, 140, 0);
        if (damage <= 100) return Color.RED;
        return new Color(220, 0, 255);
    }

    private Color getOutlineColor() {
        if (isCrit)        return new Color(80, 0, 120);
        if (damage >= 100) return new Color(100, 0, 0);
        if (damage >= 50)  return new Color(100, 40, 0);
        return Color.BLACK;
    }

    private int getFontSize() {
        if (isCrit)        return Game.scale(16);
        if (damage >= 100) return Game.scale(26);
        if (damage >= 50)  return Game.scale(22);
        if (damage >= 25)  return Game.scale(19);
        if (damage >= 10)  return Game.scale(16);
        return Game.scale(14);
    }

    public int getX() { return (int)x; }
    public int getY() { return (int)y; }
    public int getDamage() { return damage; }
}