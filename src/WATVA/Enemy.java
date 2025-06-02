package WATVA;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all enemy types in the game with movement, attacks and status effects.
 */
public class Enemy {
    private static final int SHOOT_RANGE = 300;
    private static final long SHOOT_INTERVAL_MS = 5000;
    private static final long ATTACK_COOLDOWN_MS = 1000;
    private static final long FRAME_DURATION_MS = 100;
    private static final int EDGE_LIMIT = 61;
    public static final int SCREEN_WIDTH = GamePanel.PANEL_WIDTH * 4;
    public static final int SCREEN_HEIGHT = GamePanel.PANEL_HEIGHT * 4;

    /**
     * Types of enemies in the game.
     */
    public enum Type {
        NORMAL, GIANT, SMALL, SHOOTING, SLIME, DARK_MAGE_BOSS, BUNNY_BOSS,ZOMBIE
    }

    public static final int NORMAL_SIZE = scale(37);
    public static final int GIANT_SIZE = scale(128);
    public static final int SMALL_SIZE = scale(32);
    public static final int SHOOTING_SIZE = scale(60);
    public static final int ZOMBIE_SIZE = scale(50);

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
    private int fireDamage = 10;
    private boolean isSlowed = false;
    private long slowEndTime = 0;

    private long lastShootTime = 0;
    private long lastAttackTime = 0;

    /**
     * Creates a new enemy at specified position with given health and type.
     * @param x The x-coordinate
     * @param y The y-coordinate
     * @param hp The health points
     * @param type The enemy type
     */
    public Enemy(int x, int y, double hp, Type type) {
        this.x = x;
        this.y = y;
        this.hp = hp;
        this.type = type;
        this.projectiles = new ArrayList<>();

        initializeSpeed();
        loadTextures();
    }


    /**
     * Scales a value based on the game's scale factor.
     * Used for maintaining proper sizing across different resolutions.
     *
     * @param value The base value to scale
     * @return The scaled value
     */
    private static int scale(int value) {
        return (int)(value * Game.getScaleFactor());
    }

    /**
     * Initializes movement speed based on enemy type.
     * Different enemy types have different base movement speeds.
     */
    private void initializeSpeed() {
        switch (type) {
            case GIANT -> baseSpeed = 1.8 * Game.getScaleFactor();
            case ZOMBIE -> baseSpeed = 5 * Game.getScaleFactor();
            case SMALL -> baseSpeed = 3 * Game.getScaleFactor();
            case SHOOTING -> baseSpeed = 2 * Game.getScaleFactor();
            default -> baseSpeed = 2.2 * Game.getScaleFactor();
        }
        currentSpeed = baseSpeed;
    }

    /**
     * Loads appropriate textures for the enemy based on its type.
     * Handles both animated and static enemy textures.
     */
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

    /**
     * Loads animation textures for enemies with multiple frames.
     *
     * @param enemyName The base name of the enemy for file paths
     * @param frameCount Number of animation frames
     * @param rightStartIndex Starting index for right-facing textures
     * @param leftStartIndex Starting index for left-facing textures
     * @throws IOException If texture files cannot be loaded
     */
    private void loadAnimationTextures(String enemyName, int frameCount, int rightStartIndex, int leftStartIndex)
            throws IOException {
        rightTextures = new Image[frameCount];
        leftTextures = new Image[frameCount];

        String basePath = "/WATVA/Enemy/" + enemyName + "/" + enemyName;

        for (int i = 0; i < frameCount; i++) {
            rightTextures[i] = loadTexture(basePath + (rightStartIndex + i) + ".png");
            leftTextures[i] = loadTexture(basePath + (leftStartIndex + i) + ".png");
        }
    }

    /**
     * Loads a single texture from resources.
     *
     * @param path The resource path to the texture
     * @return The loaded Image
     * @throws IOException If the texture cannot be loaded
     */
    private Image loadTexture(String path) throws IOException {
        return ImageIO.read(getClass().getResourceAsStream(path));
    }

    /**
     * Checks if enemy is outside the visible game area.
     *
     * @return true if enemy is off-screen, false otherwise
     */
    public boolean isOffScreen() {
        return x + getWidth() < 0 || x > SCREEN_WIDTH ||
                y + getHeight() < 0 || y > SCREEN_HEIGHT;
    }

    /**
     * Moves enemy towards a target position.
     * Handles both movement and attacking behavior for shooting enemies.
     *
     * @param targetX The x-coordinate to move towards
     * @param targetY The y-coordinate to move towards
     */
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

    /**
     * Determines if shooting enemy should stop to attack.
     *
     * @param targetX Target x position
     * @param targetY Target y position
     * @return true if enemy should stop to shoot, false otherwise
     */
    private boolean shouldStopAndShoot(int targetX, int targetY) {
        return type == Type.SHOOTING && isInRange(targetX, targetY);
    }

    /**
     * Checks if target is within shooting range.
     *
     * @param targetX Target x position
     * @param targetY Target y position
     * @return true if target is in range, false otherwise
     */
    private boolean isInRange(int targetX, int targetY) {
        return Math.hypot(targetX - x, targetY - y) <= SHOOT_RANGE;
    }

    /**
     * Moves enemy in specified direction.
     *
     * @param moveX X direction component (normalized)
     * @param moveY Y direction component (normalized)
     */
    private void move(double moveX, double moveY) {
        x += Math.round(moveX * currentSpeed);
        y += Math.round(moveY * currentSpeed);

        x = Math.max(EDGE_LIMIT, Math.min(x, Player.PANEL_WIDTH - EDGE_LIMIT - getWidth()));
        y = Math.max(EDGE_LIMIT, Math.min(y, Player.PANEL_HEIGHT - EDGE_LIMIT - getHeight()));
    }

    /**
     * Updates enemy's facing direction.
     *
     * @param movingRight true if enemy should face right, false for left
     */
    private void updateDirection(boolean movingRight) {
        this.movingRight = movingRight;
    }
    /**
     * Updates animation frame based on elapsed time.
     */
    private void updateAnimation() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameChangeTime >= FRAME_DURATION_MS) {
            currentFrame = (currentFrame + 1) % getFrameCount();
            lastFrameChangeTime = currentTime;
        }
    }

    /**
     * Gets number of animation frames for current enemy type.
     *
     * @return Number of animation frames
     */
    private int getFrameCount() {
        return type == Type.NORMAL ? 4 :
                type == Type.ZOMBIE ? 4 :
                type == Type.GIANT ? 6 : 1;
    }

    /**
     * Handles shooting behavior for ranged enemies.
     *
     * @param playerX Player's x position
     * @param playerY Player's y position
     */
    private void stopAndShoot(int playerX, int playerY) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShootTime >= SHOOT_INTERVAL_MS) {
            shootAtPlayer(playerX, playerY);
            lastShootTime = currentTime;
        }
    }

    /**
     * Creates and fires a projectile towards the player.
     *
     * @param playerX Player's x position
     * @param playerY Player's y position
     */
    public void shootAtPlayer(int playerX, int playerY) {
        if (isInRange(playerX, playerY)) {
            int centerX = x + SHOOTING_SIZE / 2;
            int centerY = y + SHOOTING_SIZE / 2;
            projectiles.add(new EnemyProjectile(centerX, centerY, playerX, playerY));
        }
    }

    /**
     * Updates all active projectiles and removes inactive ones.
     */
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

    /**
     * Gets list of active enemy projectiles.
     *
     * @return List of active projectiles
     */
    public List<EnemyProjectile> getProjectiles() {
        return new ArrayList<>(projectiles);
    }

    /**
     * Draws all active enemy projectiles.
     *
     * @param g Graphics context to draw with
     */
    public void drawProjectiles(Graphics g) {
        for (EnemyProjectile projectile : projectiles) {
            projectile.draw(g);
        }
    }

    /**
     * Checks if enemy can perform an attack (cooldown expired).
     *
     * @return true if attack is ready, false otherwise
     */
    public boolean canAttack() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAttackTime >= ATTACK_COOLDOWN_MS) {
            lastAttackTime = currentTime;
            return true;
        }
        return false;
    }

    /**
     * Moves enemy away from specified position.
     * Used for collision resolution.
     *
     * @param x X position to move away from
     * @param y Y position to move away from
     */
    public void moveAwayFrom(int x, int y) {
        int dx = this.x - x;
        int dy = this.y - y;

        if (Math.abs(dx) > Math.abs(dy)) {
            this.x += (dx > 0) ? 1 : -1;
        } else {
            this.y += (dy > 0) ? 1 : -1;
        }
    }

    /**
     * Updates enemy state including status effects.
     */
    public void update() {
        updateStatusEffects();

        long currentTime = System.currentTimeMillis();
        if (isOnFire && currentTime % 1000 < 15) {
            hp -= fireDamage;
            if (hp <= 0) {
                isAlive = false;
            }
        }
    }

    /**
     * Applies fire damage over time effect to enemy.
     *
     * @param damage Damage per tick
     * @param durationMs Duration of effect in milliseconds
     */
    public void setFire(int damage, int durationMs) {
        isOnFire = true;
        fireEndTime = System.currentTimeMillis() + durationMs;
        fireDamage = damage;
        hp -= damage * 4;
        if (hp <= 0) {
            isAlive = false;
        }
    }

    /**
     * Applies movement speed reduction to enemy.
     *
     * @param durationMs Duration of slow effect in milliseconds
     */
    public void applySlow(int durationMs) {
        isSlowed = true;
        slowEndTime = System.currentTimeMillis() + durationMs;
        currentSpeed = baseSpeed * 0.5;
    }

    /**
     * Updates active status effects (fire, slow).
     */
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

    /**
     * Draws the enemy with current animation frame and status effects.
     *
     * @param g Graphics context to draw with
     */
    public void draw(Graphics g) {
        drawEnemyTexture(g);
        drawStatusEffects(g);
    }

    /**
     * Draws the appropriate enemy texture based on enemy type.
     * Delegates to either animated or static drawing methods.
     *
     * @param g The Graphics context to render to
     */
    private void drawEnemyTexture(Graphics g) {
        switch (type) {
            case NORMAL, GIANT, ZOMBIE -> drawAnimatedEnemy(g);
            case SMALL, SHOOTING -> drawStaticEnemy(g);
        }
    }

    /**
     * Draws an animated enemy with multiple frames.
     * Selects appropriate texture set based on facing direction and
     * draws current animation frame.
     *
     * @param g The Graphics context to render to
     */
    private void drawAnimatedEnemy(Graphics g) {
        Image[] textures = movingRight ? rightTextures : leftTextures;
        int size;
        switch (type){
            case ZOMBIE-> size = ZOMBIE_SIZE;
            case GIANT -> size = GIANT_SIZE;
            default -> size = NORMAL_SIZE;
        }
        g.drawImage(textures[currentFrame], x, y, size, size, null);
    }

    /**
     * Draws a static (non-animated) enemy.
     * Uses single texture and adjusts size based on enemy type.
     *
     * @param g The Graphics context to render to
     */
    private void drawStaticEnemy(Graphics g) {
        int width = type == Type.SMALL ? SMALL_SIZE : SHOOTING_SIZE - 20;
        int height = type == Type.SMALL ? SMALL_SIZE : SHOOTING_SIZE;
        g.drawImage(staticTexture, x, y, width, height, null);
    }

    /**
     * Draws visual indicators for active status effects.
     * Shows different overlays for slow and fire effects.
     *
     * @param g The Graphics context to render to
     */
    private void drawStatusEffects(Graphics g) {
        if (isSlowed) {
            drawEffectOverlay(g, new Color(0, 0, 255, 100));
        }
        if (isOnFire) {
            drawEffectOverlay(g, new Color(255, 0, 0, 100));
        }
    }

    /**
     * Draws a colored overlay effect centered on the enemy.
     * Used to visualize status effects like slow or burn.
     *
     * @param g The Graphics context to render to
     * @param color The color of the effect overlay (with alpha transparency)
     */
    private void drawEffectOverlay(Graphics g, Color color) {
        g.setColor(color);
        g.fillOval(x + getWidth() / 2, y + getHeight() / 2, getWidth(), getHeight());
    }

    /**
     * Gets enemy's collision bounds.
     *
     * @return Rectangle representing collision area
     */
    public Rectangle getCollider() {
        if (type == Type.NORMAL) {
            return new Rectangle(x + 24, y + 3, NORMAL_SIZE - 32, NORMAL_SIZE - 9);
        }
        return new Rectangle(x + getWidth() / 2, y + getHeight() / 2, getWidth(), getHeight());
    }

    /**
     * Applies damage to the enemy.
     *
     * @param damage Amount of damage to apply
     */
    public void hit(int damage) {
        hp -= damage;
        isAlive = hp > 0;
    }

    public double getHp() {
        return hp;
    }

    public int getWidth() {
        return switch (type) {
            case GIANT -> GIANT_SIZE / 2;
            case SMALL -> SMALL_SIZE / 2;
            case SHOOTING -> SHOOTING_SIZE / 2;
            default -> NORMAL_SIZE / 2;
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

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }
}