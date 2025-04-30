package WATVA;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class Arrow {
    public static final int SIZE = 25;
    private int x, y;
    private int targetX, targetY;
    private int speed = 10;
    private double distanceTravelled = 0;
    private static final double MAX_DISTANCE = 30000;
    private Image[] bulletTextures;
    private int currentFrame = 0;
    private long lastFrameChange = 0;
    private long frameDuration = 100;
    private int pierceCount = 1;
    private boolean giantArrow;

    public Arrow(int x, int y, int targetX, int targetY) {
        this.x = x;
        this.y = y;

        double angle = Math.atan2(targetY - y, targetX - x);
        this.targetX = (int) (x + MAX_DISTANCE * Math.cos(angle));
        this.targetY = (int) (y + MAX_DISTANCE * Math.sin(angle));

        bulletTextures = new Image[3];
        try {
            bulletTextures[0] = ImageIO.read(new File("res/watva/bullets/Player_bullet1.png"));
            bulletTextures[1] = ImageIO.read(new File("res/watva/bullets/Player_bullet2.png"));
            bulletTextures[2] = ImageIO.read(new File("res/watva/bullets/Player_bullet3.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean move() {
        double angle = Math.atan2(targetY - y, targetX - x);
        x += speed * Math.cos(angle);
        y += speed * Math.sin(angle);
        distanceTravelled += speed;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameChange >= frameDuration) {
            currentFrame = (currentFrame + 1) % bulletTextures.length;
            lastFrameChange = currentTime;
        }

        return x < 0 || x > GamePanel.PANEL_WIDTH * 4 || y < 0 || y > GamePanel.PANEL_HEIGHT * 4 || distanceTravelled >= MAX_DISTANCE || pierceCount <= 0;
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

}
