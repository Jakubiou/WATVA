package WATVA;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Enemy {
    private static final int SHOOT_RANGE = 300;
    private static final long SHOOT_INTERVAL_MS = 5000;
    private static final long ATTACK_COOLDOWN_MS = 1000;
    private static final long FRAME_DURATION_MS = 100;
    private static final int EDGE_LIMIT = 61;
    private static final int SCREEN_WIDTH = 6120;
    private static final int SCREEN_HEIGHT = 3600;

    public enum Type {
        NORMAL, GIANT, SMALL, SHOOTING, SLIME, DARK_MAGE_BOSS, BUNNY_BOSS
    }

    public static final int NORMAL_SIZE = scale(37);
    public static final int GIANT_SIZE = scale(128);
    public static final int SMALL_SIZE = scale(32);
    public static final int SHOOTING_SIZE = scale(60);

    private final List<EnemyProjectile> projectiles;
    private final Type type;
    protected int x;
    protected int y;
    protected double hp;
    protected double baseSpeed;
    private double currentSpeed;
    private boolean isAlive = true;

    private Image[] rightTextures;
    private Image[] leftTextures;
    private Image staticTexture;
    private int currentFrame = 0;
    private long lastFrameChangeTime = 0;
    private boolean movingRight = true;

    private boolean isOnFire = false;
    private long fireEndTime = 0;
    private int fireDamage = 0;
    private boolean isSlowed = false;
    private long slowEndTime = 0;

    private long lastShootTime = 0;
    private long lastAttackTime = 0;

    public Enemy(int x, int y, double hp, Type type) {
        this.x = x;
        this.y = y;
        this.hp = hp;
        this.type = type;
        this.projectiles = new ArrayList<>();

        initializeSpeed();
        loadTextures();
    }

    private static int scale(int value) {
        return (int)(value * Game.getScaleFactor());
    }

    private void initializeSpeed() {
        switch (type) {
            case NORMAL -> baseSpeed = 2 * Game.getScaleFactor();
            case GIANT -> baseSpeed = 1.5 * Game.getScaleFactor();
            case SMALL -> baseSpeed = 2.5 * Game.getScaleFactor();
            case SHOOTING -> baseSpeed = 1.8 * Game.getScaleFactor();
        }
        currentSpeed = baseSpeed;
    }

    private void loadTextures() {
        try {
            switch (type) {
                case NORMAL -> loadAnimationTextures("knight", 4, 1, 5);
                case GIANT -> loadAnimationTextures("golem", 6, 7, 1);
                case SMALL -> staticTexture = loadTexture("res/watva/enemy/small/small.png");
                case SHOOTING -> staticTexture = loadTexture("res/watva/enemy/mage/mage1.png");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadAnimationTextures(String enemyName, int frameCount, int rightStartIndex, int leftStartIndex)
            throws IOException {
        rightTextures = new Image[frameCount];
        leftTextures = new Image[frameCount];

        String basePath = "res/watva/enemy/" + enemyName + "/" + enemyName;

        for (int i = 0; i < frameCount; i++) {
            rightTextures[i] = loadTexture(basePath + (rightStartIndex + i) + ".png");
            leftTextures[i] = loadTexture(basePath + (leftStartIndex + i) + ".png");
        }
    }

    private Image loadTexture(String path) throws IOException {
        return ImageIO.read(new File(path));
    }

    public boolean isOffScreen() {
        return x + getWidth() < 0 || x > SCREEN_WIDTH ||
                y + getHeight() < 0 || y > SCREEN_HEIGHT;
    }

    public void moveTowards(int targetX, int targetY) {
        if (shouldStopAndShoot(targetX, targetY)) {
            stopAndShoot(targetX, targetY);
            return;
        }

        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.hypot(dx, dy);

        if (distance > 0) {
            move(dx / distance, dy / distance);
            updateDirection(dx > 0);
            updateAnimation();
        }
    }

    private boolean shouldStopAndShoot(int targetX, int targetY) {
        return type == Type.SHOOTING && isInRange(targetX, targetY);
    }

    private boolean isInRange(int targetX, int targetY) {
        return Math.hypot(targetX - x, targetY - y) <= SHOOT_RANGE;
    }

    private void move(double moveX, double moveY) {
        x += Math.round(moveX * currentSpeed);
        y += Math.round(moveY * currentSpeed);

        x = Math.max(EDGE_LIMIT, Math.min(x, Player.PANEL_WIDTH - EDGE_LIMIT - getWidth()));
        y = Math.max(EDGE_LIMIT, Math.min(y, Player.PANEL_HEIGHT - EDGE_LIMIT - getHeight()));
    }

    private void updateDirection(boolean movingRight) {
        this.movingRight = movingRight;
    }

    private void updateAnimation() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameChangeTime >= FRAME_DURATION_MS) {
            currentFrame = (currentFrame + 1) % getFrameCount();
            lastFrameChangeTime = currentTime;
        }
    }

    private int getFrameCount() {
        return type == Type.NORMAL ? 4 :
                type == Type.GIANT ? 6 : 1;
    }

    private void stopAndShoot(int playerX, int playerY) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShootTime >= SHOOT_INTERVAL_MS) {
            shootAtPlayer(playerX, playerY);
            lastShootTime = currentTime;
        }
    }

    public void shootAtPlayer(int playerX, int playerY) {
        if (isInRange(playerX, playerY)) {
            int centerX = x + SHOOTING_SIZE / 2;
            int centerY = y + SHOOTING_SIZE / 2;
            projectiles.add(new EnemyProjectile(centerX, centerY, playerX, playerY));
        }
    }

    public void updateProjectiles() {
        for (int i = 0; i < projectiles.size(); i++) {
            EnemyProjectile projectile = projectiles.get(i);
            projectile.move();
            if (!projectile.isActive()) {
                projectiles.remove(i);
                i--;
            }
        }
    }

    public List<EnemyProjectile> getProjectiles() {
        return new ArrayList<>(projectiles);
    }

    public void drawProjectiles(Graphics g) {
        for (EnemyProjectile projectile : projectiles) {
            projectile.draw(g);
        }
    }
    public boolean canAttack() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAttackTime >= ATTACK_COOLDOWN_MS) {
            lastAttackTime = currentTime;
            return true;
        }
        return false;
    }

    public void moveAwayFrom(int x, int y) {
        int dx = this.x - x;
        int dy = this.y - y;

        if (Math.abs(dx) > Math.abs(dy)) {
            this.x += (dx > 0) ? 1 : -1;
        } else {
            this.y += (dy > 0) ? 1 : -1;
        }
    }

    public void applySlow(int durationMs) {
        isSlowed = true;
        slowEndTime = System.currentTimeMillis() + durationMs;
        currentSpeed = baseSpeed * 0.5;
    }

    public void setFire(int damage, int durationMs) {
        isOnFire = true;
        fireEndTime = System.currentTimeMillis() + durationMs;
        fireDamage = damage;
    }

    public void update() {
        updateStatusEffects();
        if (isOnFire && System.currentTimeMillis() < fireEndTime) {
            hp -= fireDamage;
        }
    }

    private void updateStatusEffects() {
        if (isSlowed && System.currentTimeMillis() >= slowEndTime) {
            isSlowed = false;
            currentSpeed = baseSpeed;
        }
        if (isOnFire && System.currentTimeMillis() >= fireEndTime) {
            isOnFire = false;
        }
    }

    public void draw(Graphics g) {
        drawEnemyTexture(g);
        drawStatusEffects(g);
    }

    private void drawEnemyTexture(Graphics g) {
        switch (type) {
            case NORMAL, GIANT -> drawAnimatedEnemy(g);
            case SMALL, SHOOTING -> drawStaticEnemy(g);
        }
    }

    private void drawAnimatedEnemy(Graphics g) {
        Image[] textures = movingRight ? rightTextures : leftTextures;
        int size = type == Type.NORMAL ? NORMAL_SIZE : GIANT_SIZE;
        g.drawImage(textures[currentFrame], x, y, size, size, null);
    }

    private void drawStaticEnemy(Graphics g) {
        int width = type == Type.SMALL ? SMALL_SIZE : SHOOTING_SIZE - 20;
        int height = type == Type.SMALL ? SMALL_SIZE : SHOOTING_SIZE;
        g.drawImage(staticTexture, x, y, width, height, null);
    }

    private void drawStatusEffects(Graphics g) {
        if (isSlowed) {
            drawEffectOverlay(g, new Color(0, 0, 255, 100));
        }
        if (isOnFire) {
            drawEffectOverlay(g, new Color(255, 0, 0, 100));
        }
    }

    private void drawEffectOverlay(Graphics g, Color color) {
        g.setColor(color);
        g.fillRect(x, y, getWidth(), getHeight());
    }

    public Rectangle getCollider() {
        if (type == Type.NORMAL) {
            return new Rectangle(x + 24, y + 3, NORMAL_SIZE - 32, NORMAL_SIZE - 9);
        }
        return new Rectangle(x, y, getWidth(), getHeight());
    }

    public void hit(int damage) {
        hp -= damage;
        isAlive = hp > 0;
    }

    public double getHp() {
        return hp;
    }

    public int getWidth() {
        return switch (type) {
            case GIANT -> GIANT_SIZE;
            case SMALL -> SMALL_SIZE;
            case SHOOTING -> SHOOTING_SIZE;
            default -> NORMAL_SIZE;
        };
    }

    public int getHeight() {
        return getWidth();
    }

    public int getDamage() {
        return switch (type) {
            case SHOOTING -> 20;
            case SLIME -> 5;
            case DARK_MAGE_BOSS -> 50;
            default -> 10;
        };
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public Type getType() {
        return type;
    }
}