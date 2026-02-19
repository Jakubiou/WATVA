package Enemies;

import Logic.DamageNumber.DamageNumber;
import Logic.DamageNumber.DamageNumberManager;
import Core.Game;
import Logic.PathFinding;
import Logic.WallManager;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Enemy {
    private static final int SHOOT_RANGE = Game.scale(450);
    private static final long SHOOT_INTERVAL_MS = 2000;
    private static final long ATTACK_COOLDOWN_MS = 1000;
    private static final long FRAME_DURATION_MS = 100;
    private static final long UNSTUCK_CHECK_INTERVAL = 500; // Check every 500ms

    public enum Type {
        NORMAL, GIANT, SMALL, SHOOTING, SLIME, DARK_MAGE_BOSS, BUNNY_BOSS, ZOMBIE
    }

    public static final int NORMAL_SIZE = Game.scale(37);
    public static final int GIANT_SIZE = Game.scale(128);
    public static final int SMALL_SIZE = Game.scale(32);
    public static final int SHOOTING_SIZE = Game.scale(60);
    public static final int ZOMBIE_SIZE = Game.scale(50);

    private static final List<EnemyProjectile> globalProjectiles = new ArrayList<>();

    private final List<DamageNumber> damageNumbers;
    private final Type type;
    protected int x;
    protected int y;
    protected double hp;
    protected double baseSpeed;
    protected double currentSpeed;
    private boolean isAlive = true;

    private Image[] rightTextures;
    private Image[] leftTextures;
    private Image staticTexture;
    private int currentFrame = 0;
    private long lastFrameChangeTime = 0;
    private boolean movingRight = true;

    private boolean isOnFire = false;
    private long fireEndTime = 0;
    private int fireDamage = 10;
    private boolean isSlowed = false;
    private long slowEndTime = 0;

    private long lastShootTime = 0;
    private long lastAttackTime = 0;
    private long lastUnstuckCheck = 0;

    private Point nextPathStep = null;
    private long lastPathCalcTime = 0;
    private static final long PATH_RECALC_INTERVAL = 300;

    public Enemy(int x, int y, double hp, Type type) {
        this.x = x;
        this.y = y;
        this.hp = hp;
        this.type = type;
        this.damageNumbers = new ArrayList<>();

        initializeSpeed();
        loadTextures();
    }

    private void initializeSpeed() {
        switch (type) {
            case GIANT -> baseSpeed = Game.scale(1.8);
            case ZOMBIE -> baseSpeed = Game.scale(3.5);
            case SMALL -> baseSpeed = Game.scale(3.0);
            case SHOOTING -> baseSpeed = Game.scale(2.0);
            default -> baseSpeed = Game.scale(2.2);
        }
        currentSpeed = baseSpeed;
    }

    private void loadTextures() {
        try {
            switch (type) {
                case NORMAL -> loadAnimationTextures("Knight", 4, 1, 5);
                case ZOMBIE -> loadAnimationTextures("Zombie", 4, 1, 5);
                case GIANT -> loadAnimationTextures("Golem", 6, 7, 1);
                case SMALL -> staticTexture = loadTexture("/WATVA/Enemy/Small/Small.png");
                case SHOOTING -> staticTexture = loadTexture("/WATVA/Enemy/Mage/Mage1.png");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadAnimationTextures(String enemyName, int frameCount, int rightStartIndex, int leftStartIndex) throws IOException {
        rightTextures = new Image[frameCount];
        leftTextures = new Image[frameCount];
        String basePath = "/WATVA/Enemy/" + enemyName + "/" + enemyName;
        for (int i = 0; i < frameCount; i++) {
            rightTextures[i] = loadTexture(basePath + (rightStartIndex + i) + ".png");
            leftTextures[i] = loadTexture(basePath + (leftStartIndex + i) + ".png");
        }
    }

    private Image loadTexture(String path) throws IOException {
        return ImageIO.read(getClass().getResourceAsStream(path));
    }


    public void moveTowards(int targetPlayerX, int targetPlayerY, WallManager wallManager) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUnstuckCheck >= UNSTUCK_CHECK_INTERVAL) {
            checkAndUnstuck(wallManager);
            lastUnstuckCheck = currentTime;
        }

        if (type == Type.SLIME || type == Type.BUNNY_BOSS || type == Type.DARK_MAGE_BOSS) {
            moveTowards(targetPlayerX, targetPlayerY);
            return;
        }

        int centerX = x + getWidth() / 2;
        int centerY = y + getHeight() / 2;
        int targetCenterX = targetPlayerX + 25;
        int targetCenterY = targetPlayerY + 25;

        if (type == Type.SHOOTING) {
            boolean inRange = isInRange(targetPlayerX, targetPlayerY);
            boolean hasLineOfSight = wallManager.hasLineOfSight(centerX, centerY, targetCenterX, targetCenterY);

            if (inRange && hasLineOfSight) {
                movingRight = targetPlayerX > x;

                if (currentTime - lastShootTime >= SHOOT_INTERVAL_MS) {
                    shootAtPlayer(targetPlayerX, targetPlayerY);
                    lastShootTime = currentTime;
                }
                return;
            }
        }

        boolean hasLineOfSight = PathFinding.hasClearPath(centerX, centerY, targetCenterX, targetCenterY, wallManager);

        int moveTargetX, moveTargetY;

        if (hasLineOfSight) {
            moveTargetX = targetPlayerX;
            moveTargetY = targetPlayerY;
        } else {
            if (currentTime - lastPathCalcTime > PATH_RECALC_INTERVAL || nextPathStep == null) {
                nextPathStep = PathFinding.findNextStep(centerX, centerY, targetCenterX, targetCenterY, wallManager);
                lastPathCalcTime = currentTime;
            }

            if (nextPathStep != null) {
                moveTargetX = nextPathStep.x + (x % 20 - 10);
                moveTargetY = nextPathStep.y + (y % 20 - 10);
            } else {
                moveTargetX = targetPlayerX;
                moveTargetY = targetPlayerY;
            }
        }

        double deltaX = moveTargetX - x;
        double deltaY = moveTargetY - y;
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

        if (distance > 1) {
            movingRight = deltaX > 0;

            double normalizedX = deltaX / distance;
            double normalizedY = deltaY / distance;

            int nextX = x + (int)(normalizedX * currentSpeed);
            int nextY = y + (int)(normalizedY * currentSpeed);

            if (!checkWallCollision(nextX, nextY, wallManager)) {
                x = nextX;
                y = nextY;
            } else if (!checkWallCollision(nextX, y, wallManager)) {
                x = nextX;
            } else if (!checkWallCollision(x, nextY, wallManager)) {
                y = nextY;
            }

            updateAnimation();
        }
    }

    private boolean checkWallCollision(int nextX, int nextY, WallManager wallManager) {
        int w = getWidth();
        int h = getHeight();

        return wallManager.isWall(nextX + 2, nextY + 2) ||
                wallManager.isWall(nextX + w - 2, nextY + 2) ||
                wallManager.isWall(nextX + 2, nextY + h - 2) ||
                wallManager.isWall(nextX + w - 2, nextY + h - 2) ||
                wallManager.isWall(nextX + w/2, nextY + h/2);
    }

    private void checkAndUnstuck(WallManager wallManager) {
        int centerX = x + getWidth() / 2;
        int centerY = y + getHeight() / 2;

        if (wallManager.isWall(centerX, centerY)) {
            Point newPos = wallManager.unstuckFromWall(x, y, getWidth(), getHeight());
            x = newPos.x;
            y = newPos.y;
        }
    }

    public void moveTowards(int targetPlayerX, int targetPlayerY) {
        int deltaX = targetPlayerX - x;
        int deltaY = targetPlayerY - y;
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

        if (distance > 1) {
            movingRight = deltaX > 0;
            double normalizedX = deltaX / distance;
            double normalizedY = deltaY / distance;
            x += (int)(normalizedX * currentSpeed);
            y += (int)(normalizedY * currentSpeed);
            updateAnimation();
        }
    }

    private boolean isInRange(int targetX, int targetY) {
        return Math.hypot(targetX - x, targetY - y) <= SHOOT_RANGE;
    }

    public void shootAtPlayer(int playerX, int playerY) {
        int centerX = x + getWidth() / 2;
        int centerY = y + getHeight() / 2;
        globalProjectiles.add(new EnemyProjectile(centerX, centerY, playerX + 25, playerY + 25));
    }

    public static void updateAllProjectiles() {
        globalProjectiles.removeIf(projectile -> {
            projectile.move();
            return !projectile.isActive();
        });
    }

    public static void drawAllProjectiles(Graphics g) {
        for (EnemyProjectile projectile : globalProjectiles) {
            projectile.draw(g);
        }
    }

    public static List<EnemyProjectile> getAllProjectiles() {
        return new ArrayList<>(globalProjectiles);
    }

    public static void clearAllProjectiles() {
        globalProjectiles.clear();
    }

    private void updateAnimation() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameChangeTime >= FRAME_DURATION_MS) {
            currentFrame = (currentFrame + 1) % getFrameCount();
            lastFrameChangeTime = currentTime;
        }
    }

    private int getFrameCount() {
        return type == Type.NORMAL ? 4 : type == Type.ZOMBIE ? 4 : type == Type.GIANT ? 6 : 1;
    }

    public boolean canAttack() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAttackTime >= ATTACK_COOLDOWN_MS) {
            lastAttackTime = currentTime;
            return true;
        }
        return false;
    }

    public void moveAwayFrom(int otherX, int otherY) {
        int dx = this.x - otherX;
        int dy = this.y - otherY;
        if (dx == 0 && dy == 0) dx = 1;

        double dist = Math.sqrt(dx*dx + dy*dy);
        if (dist > 0) {
            this.x += (int)(dx/dist * 2);
            this.y += (int)(dy/dist * 2);
        }
    }

    public void update(DamageNumberManager damageManager) {
        updateStatusEffects();
        long currentTime = System.currentTimeMillis();
        if (isOnFire && currentTime % 1000 < 20) {
            hit(fireDamage, damageManager);
        }
    }

    public void setFire(int damage, int durationMs, DamageNumberManager damageManager) {
        isOnFire = true;
        fireEndTime = System.currentTimeMillis() + durationMs;
        fireDamage = damage;
        hit(damage * 2, damageManager);
    }

    public void applySlow(int durationMs) {
        if (!isSlowed) {
            currentSpeed = baseSpeed * 0.5;
        }
        isSlowed = true;
        slowEndTime = System.currentTimeMillis() + durationMs;
    }

    private void updateStatusEffects() {
        long currentTime = System.currentTimeMillis();
        if (isSlowed && currentTime >= slowEndTime) {
            isSlowed = false;
            currentSpeed = baseSpeed;
        }
        if (isOnFire && currentTime >= fireEndTime) {
            isOnFire = false;
            fireDamage = 0;
        }
    }

    public void draw(Graphics g) {
        drawEnemyTexture(g);
        drawStatusEffects(g);
    }

    private void drawEnemyTexture(Graphics g) {
        switch (type) {
            case NORMAL, GIANT, ZOMBIE -> drawAnimatedEnemy(g);
            case SMALL, SHOOTING -> drawStaticEnemy(g);
        }
    }

    private void drawAnimatedEnemy(Graphics g) {
        Image[] textures = movingRight ? rightTextures : leftTextures;
        int size = (type == Type.GIANT) ? GIANT_SIZE : (type == Type.ZOMBIE ? ZOMBIE_SIZE : NORMAL_SIZE);
        if (textures != null && currentFrame < textures.length && textures[currentFrame] != null) {
            g.drawImage(textures[currentFrame], x, y, size, size, null);
        }
    }

    private void drawStaticEnemy(Graphics g) {
        int width = type == Type.SMALL ? SMALL_SIZE : SHOOTING_SIZE - Game.scale(20);
        int height = type == Type.SMALL ? SMALL_SIZE : SHOOTING_SIZE;
        if (staticTexture != null) {
            g.drawImage(staticTexture, x, y, width, height, null);
        }
    }

    private void drawStatusEffects(Graphics g) {
        if (isSlowed) drawEffectOverlay(g, new Color(0, 0, 255, 60));
        if (isOnFire) drawEffectOverlay(g, new Color(255, 0, 0, 60));
    }

    private void drawEffectOverlay(Graphics g, Color color) {
        g.setColor(color);
        g.fillOval(x, y, getWidth(), getHeight());
    }

    public Rectangle getCollider() {
        return new Rectangle(x, y, getWidth(), getHeight());
    }

    public void hit(int damage, DamageNumberManager damageManager) {
        hp -= damage;
        damageManager.addDamageNumber(x + getWidth() / 2, y, damage);
        if (hp <= 0) isAlive = false;
    }

    public double getHp() { return hp; }
    public int getWidth() {
        return switch (type) {
            case GIANT -> GIANT_SIZE;
            case SMALL -> SMALL_SIZE;
            case SHOOTING -> SHOOTING_SIZE - Game.scale(20);
            case ZOMBIE -> ZOMBIE_SIZE;
            default -> NORMAL_SIZE;
        };
    }
    public int getHeight() {
        return switch (type) {
            case SHOOTING -> SHOOTING_SIZE;
            default -> getWidth();
        };
    }
    public int getDamage() {
        return switch (type) {
            case SHOOTING -> 20;
            case SLIME -> 5;
            case DARK_MAGE_BOSS -> 50;
            default -> 10;
        };
    }
    public int getX() { return x; }
    public int getY() { return y; }
    public boolean isAlive() { return isAlive; }
    public Type getType() { return type; }
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }

    @Deprecated
    public List<EnemyProjectile> getProjectiles() { return getAllProjectiles(); }
    @Deprecated
    public void updateProjectiles() { updateAllProjectiles(); }
    @Deprecated
    public void drawProjectiles(Graphics g) { drawAllProjectiles(g); }
}