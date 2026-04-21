package Enemies;

import Logic.DamageNumber.DamageNumber;
import Logic.DamageNumber.DamageNumberManager;
import Core.Game;
import Logic.PathFinding;
import Logic.World.WallManager;

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
    private static final long UNSTUCK_CHECK_INTERVAL = 500;

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
    private static final long PATH_RECALC_INTERVAL = 700;  // bylo 300ms

    // Detekce zaseknutí + wall-steering
    private int lastRecordedX = Integer.MIN_VALUE;
    private int lastRecordedY = Integer.MIN_VALUE;
    private long lastStuckSampleTime = 0;
    private static final long STUCK_SAMPLE_INTERVAL = 400; // jak často měříme pohyb
    private static final int  STUCK_THRESHOLD = Game.scale(4); // méně než X px za interval = zaseklý
    private int stuckCounter = 0;                // kolik po sobě jsme zaseklí
    private int wallSteerDir = 0;               // +1 nebo -1: smysl obcházení rohu

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
            case GIANT -> baseSpeed = Game.scale(2.0);
            case ZOMBIE -> baseSpeed = Game.scale(4.5);
            case SMALL -> baseSpeed = Game.scale(5.5);
            case SHOOTING -> baseSpeed = Game.scale(3.0);
            default -> baseSpeed = Game.scale(3.5);
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


    // Cachovaný LOS výsledek – nepočítej každý frame
    private boolean cachedHasLOS = true;
    private long lastLOSCheck = 0;
    private static final long LOS_CHECK_INTERVAL = 600;  // bylo 250ms

    public void moveTowards(int targetPlayerX, int targetPlayerY, WallManager wallManager) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUnstuckCheck >= UNSTUCK_CHECK_INTERVAL) {
            checkAndUnstuck(wallManager);
            lastUnstuckCheck = currentTime;
        }

        if (type == Type.SLIME || type == Type.BUNNY_BOSS) {
            moveTowards(targetPlayerX, targetPlayerY);
            return;
        }
        if (type == Type.DARK_MAGE_BOSS) {
            return;
        }

        int centerX = x + getWidth() / 2;
        int centerY = y + getHeight() / 2;
        int targetCenterX = targetPlayerX + 25;
        int targetCenterY = targetPlayerY + 25;

        if (type == Type.SHOOTING) {
            boolean inRange = isInRange(targetPlayerX, targetPlayerY);
            if (inRange) {
                boolean hasLineOfSight = wallManager.hasLineOfSight(centerX, centerY, targetCenterX, targetCenterY);
                if (hasLineOfSight) {
                    movingRight = targetPlayerX > x;
                    if (currentTime - lastShootTime >= SHOOT_INTERVAL_MS) {
                        shootAtPlayer(targetPlayerX, targetPlayerY);
                        lastShootTime = currentTime;
                    }
                    return;
                }
            }
        }

        // Cache LOS – nekontroluj každý frame (drahé pro 100+ enemáků)
        if (currentTime - lastLOSCheck > LOS_CHECK_INTERVAL) {
            cachedHasLOS = PathFinding.hasClearPath(centerX, centerY, targetCenterX, targetCenterY, wallManager);
            lastLOSCheck = currentTime;
        }

        int moveTargetX, moveTargetY;

        if (cachedHasLOS) {
            moveTargetX = targetPlayerX;
            moveTargetY = targetPlayerY;
            nextPathStep = null;
        } else {
            if (currentTime - lastPathCalcTime > PATH_RECALC_INTERVAL || nextPathStep == null) {
                // Pro velké entity (GIANT) použij findNextStepLarge – A* pak vyhýbá i rohům
                Point newStep;
                Rectangle col = getCollider();
                int entitySize = Math.max(col.width, col.height);
                if (entitySize > Logic.PathFinding.GRID_SIZE) {
                    newStep = PathFinding.findNextStepLarge(centerX, centerY, targetCenterX, targetCenterY, wallManager, entitySize);
                } else {
                    newStep = PathFinding.findNextStep(centerX, centerY, targetCenterX, targetCenterY, wallManager);
                }
                if (newStep != null) {
                    nextPathStep = newStep;      // aktualizuj jen pokud A* nebyl limitován
                    lastPathCalcTime = currentTime;
                }
                // null = frame limit překročen → nextPathStep zůstane starý cached krok
            }

            if (nextPathStep != null) {
                moveTargetX = nextPathStep.x;
                moveTargetY = nextPathStep.y;
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

            // ── Detekce zaseknutí ────────────────────────────────────────────
            if (currentTime - lastStuckSampleTime >= STUCK_SAMPLE_INTERVAL) {
                int movedDist = (int) Math.hypot(x - lastRecordedX, y - lastRecordedY);
                if (lastRecordedX != Integer.MIN_VALUE && movedDist < STUCK_THRESHOLD) {
                    stuckCounter++;
                    if (stuckCounter == 1) {
                        // Zvol smysl obcházení: zkus kolmý směr, ten který vede blíže k cíli
                        double perpAX = -normalizedY, perpAY = normalizedX;
                        double perpBX =  normalizedY, perpBY = -normalizedX;
                        double dotA = perpAX * deltaX + perpAY * deltaY;
                        wallSteerDir = (dotA >= 0) ? 1 : -1;
                    }
                    // Vynuť přepočet cesty, zaseklí ignorují cache
                    lastPathCalcTime = 0;
                } else {
                    stuckCounter = 0;
                    wallSteerDir = 0;
                }
                lastRecordedX = x;
                lastRecordedY = y;
                lastStuckSampleTime = currentTime;
            }

            // ── Wall-hugging / corner steering ───────────────────────────────
            // Pokud jsme zaseklí, přimíchej kolmý vektor k pohybu (wall hugging)
            double moveX = normalizedX;
            double moveY = normalizedY;

            if (stuckCounter >= 1 && wallSteerDir != 0) {
                // Kolmý vektor (otočení o 90°) ve zvoleném smyslu
                double perpX = -normalizedY * wallSteerDir;
                double perpY =  normalizedX * wallSteerDir;

                // Přimíchej kolmou složku – čím déle zaseklý, tím více se otočíme (max 90°)
                double steerStrength = Math.min(stuckCounter * 0.4, 1.0);
                moveX = normalizedX * (1.0 - steerStrength) + perpX * steerStrength;
                moveY = normalizedY * (1.0 - steerStrength) + perpY * steerStrength;

                // Normalizuj výsledný vektor
                double len = Math.sqrt(moveX * moveX + moveY * moveY);
                if (len > 0.001) { moveX /= len; moveY /= len; }
            }

            // ── Sub-step sliding ─────────────────────────────────────────────
            // Pohyb po 1 px na každé ose zvlášť – kloužení po rovné zdi.
            int stepsX = (int) Math.abs(Math.round(moveX * currentSpeed));
            int stepsY = (int) Math.abs(Math.round(moveY * currentSpeed));
            int signX  = moveX >= 0 ? 1 : -1;
            int signY  = moveY >= 0 ? 1 : -1;

            boolean blockedX = false, blockedY = false;
            for (int s = 0; s < stepsX; s++) {
                if (!checkWallCollision(x + signX, y, wallManager)) x += signX;
                else { blockedX = true; cachedHasLOS = false; lastPathCalcTime = 0; break; }
            }
            for (int s = 0; s < stepsY; s++) {
                if (!checkWallCollision(x, y + signY, wallManager)) y += signY;
                else { blockedY = true; cachedHasLOS = false; lastPathCalcTime = 0; break; }
            }

            // Pokud obě osy zablokovány a steer nepomohl, vyber opačný steer
            if (blockedX && blockedY && stuckCounter > 3) {
                wallSteerDir = -wallSteerDir;
                stuckCounter = 1; // reset počítadla, ale nezapomeň že jsme zaseklí
            }

            updateAnimation();
        } else {
            // Dorazili jsme k cílovému kroku – resetuj stuck state
            stuckCounter = 0;
            wallSteerDir = 0;
        }
    }

    private boolean checkWallCollision(int nextX, int nextY, WallManager wallManager) {
        // Použij skutečný collider (s offsety jako u Slime/Giant) místo raw getWidth/Height.
        // getCollider() vrací Rectangle relativní vůči (x,y), takže offsety přepočítáme.
        Rectangle col = getCollider();
        int offX = col.x - x;   // offset od levého horního rohu sprite
        int offY = col.y - y;
        int cw   = col.width;
        int ch   = col.height;
        int pad  = 2;

        return wallManager.isWall(nextX + offX + pad,      nextY + offY + pad) ||
                wallManager.isWall(nextX + offX + cw - pad, nextY + offY + pad) ||
                wallManager.isWall(nextX + offX + pad,      nextY + offY + ch - pad) ||
                wallManager.isWall(nextX + offX + cw - pad, nextY + offY + ch - pad) ||
                wallManager.isWall(nextX + offX + cw / 2,   nextY + offY + ch / 2);
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

    /**
     * Jemný push pro separaci enemáků (Vampire Survivors styl).
     * Neruší pathfinding – jen posune o pár pixelů.
     */
    public void nudge(int dx, int dy) {
        x += dx;
        y += dy;
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