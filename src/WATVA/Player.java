package WATVA;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.*;
import java.util.ArrayList;

import static com.sun.java.accessibility.util.AWTEventMonitor.addMouseMotionListener;

/**
 * Represents the player character in the game with all attributes, abilities, and state.
 * Handles player movement, combat, upgrades, and persistence.
 */
public class Player implements Serializable {
    public static int WIDTH = (int)(50 * Game.getScaleFactor());
    public static final int HEIGHT = (int)(50 * Game.getScaleFactor());
    public static final int MAX_SHIELD_HP = 100;
    public static final long SHIELD_REGENERATION_INTERVAL = 1000;
    public static final long HP_REGENERATION_INTERVAL = 1000;
    public static final long IDLE_TRIGGER_DELAY = 500;

    private ArrayList<Explosion> explosions = new ArrayList<>();
    private int x, y, hp;
    private int maxHp;
    private int speed = (int)(5 * Game.getScaleFactor());
    private int dashSpeed = (int)(20 * Game.getScaleFactor());
    private int dashDistance = 100;
    private long dashCooldown = 5000;
    private long lastDashTime = 0;
    private int coins = 0;
    private int damage = 1;
    private int critChance = 0; // Začíná na 0%
    private static final int MAX_CRIT_CHANCE = 75; // Maximum 75%
    private static final double MIN_CRIT_MULTIPLIER = 1.5;
    private static final double MAX_CRIT_MULTIPLIER = 2.0;
    private int attackSpeed = 5;
    private int defense = 0;
    private static final int MAX_DEFENSE = 50;

    private int currentFrame = 0;
    private long lastFrameChange = 0;
    private long frameDuration = 200;
    private long lastMovementTime = System.currentTimeMillis();
    private long idleAnimationStartTime = 0;

    private long explosionCooldown = 5000;
    private long lastExplosionTime = 0;
    private int explosionRange = 100;

    private int mouseX, mouseY;
    private static final long serialVersionUID = 1L;

    private boolean isExplosionActive = false;
    private boolean isDoubleShotActive = false;
    private boolean isForwardBackwardShotActive = false;

    private int piercingArrowsLevel = 0;
    private boolean slowEnemiesUnlocked = false;
    private int fireDamageLevel = 0;
    private int speedBoostLevel = 0;
    private int explosionRangeLevel = 0;
    private int regenerationLevel = 0;
    private int shieldLevel = 0;
    private int shieldHP = 0;
    private long lastShieldRegenerationTime = 0;
    private long lastHpRegenerationTime = 0;
    private int slowEnemiesLevel = 0;

    private transient PlayerGraphics graphics;
    private transient PlayerMovement movement;

    /**
     * Constructs a new Player at specified coordinates with given health.
     * Initializes all player systems including graphics, movement, and input handling.
     *
     * @param x The initial x-coordinate
     * @param y The initial y-coordinate
     * @param hp The initial health points
     */
    public Player(int x, int y, int hp) {
        this.x = x;
        this.y = y;
        this.hp = Math.min(hp, 500);
        this.maxHp = Math.min(hp, 500);
        this.lastHpRegenerationTime = System.currentTimeMillis();
        initializeTransientFields();
        addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
        });
    }

    /**
     * Initializes transient fields after deserialization.
     * Required because graphics and movement systems shouldn't be serialized.
     */
    private void initializeTransientFields() {
        this.graphics = new PlayerGraphics(this);
        this.movement = new PlayerMovement(this);
    }


    public void move() {
        movement.move();
    }

    public void keyPressed(KeyEvent e) {
        movement.keyPressed(e);
    }

    public void keyReleased(KeyEvent e) {
        movement.keyReleased(e);
    }

    public boolean canUseExplosion() {
        return System.currentTimeMillis() - lastExplosionTime >= explosionCooldown;
    }

    /**
     * Triggers an explosion centered on player's current position.
     * Creates explosion effect and starts cooldown timer.
     */
    public void triggerExplosion() {
        explosions.add(new Explosion(x + WIDTH / 2, y + HEIGHT / 2, explosionRange));
        lastExplosionTime = System.currentTimeMillis();
    }

    /**
     * Saves complete player state to file.
     *
     * @param filePath Path to save file
     */
    public void saveState(String filePath) {
        try {
            File file = new File(filePath);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
                oos.writeObject(this);
                System.out.println("Player state saved successfully: x=" + x + ", y=" + y +
                        ", hp=" + hp + ", coins=" + coins + ", damage=" + damage);
            }
        } catch (IOException e) {
            System.err.println("Error saving player state: " + e.getMessage());
        }
    }

    /**
     * Loads player state from file.
     *
     * @param filePath Path to save file
     * @return Loaded Player instance or null if load fails
     */
    public static Player loadState(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("No saved player file found, creating new player");
            return null;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            Player player = (Player) ois.readObject();
            player.initializeTransientFields();
            if (player.lastHpRegenerationTime == 0) {
                player.lastHpRegenerationTime = System.currentTimeMillis();
            }
            System.out.println("Player state loaded successfully: x=" + player.getX() +
                    ", y=" + player.getY() + ", hp=" + player.getHp() + ", coins=" + player.getCoins());
            return player;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading player state: " + e.getMessage());
            return null;
        }
    }

    /**
     * Saves only player's coins to file while preserving other stats.
     *
     * @param filePath Path to save file
     */
    public void saveCoins(String filePath) {
        try {
            Player existingPlayer = loadState(filePath);
            if (existingPlayer == null) {
                existingPlayer = new Player(0, 0, 100);
            }

            existingPlayer.setCoins(this.coins);
            saveState(filePath, existingPlayer);
            System.out.println("Coins saved successfully: " + coins);
        } catch (Exception e) {
            System.err.println("Error saving coins: " + e.getMessage());
        }
    }

    /**
     * Saves only player's location to file while preserving other stats.
     *
     * @param filePath Path to save file
     */
    public void saveLocation(String filePath) {
        try {
            Player existingPlayer = loadState(filePath);
            if (existingPlayer == null) {
                existingPlayer = new Player(0, 0, 100);
            }

            existingPlayer.setX(this.x);
            existingPlayer.setY(this.y);
            saveState(filePath, existingPlayer);
            System.out.println("Location saved successfully: x=" + x + ", y=" + y);
        } catch (Exception e) {
            System.err.println("Error saving location: " + e.getMessage());
        }
    }

    private void saveState(String filePath, Player player) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(player);
        } catch (IOException e) {
            System.err.println("Error saving player state: " + e.getMessage());
        }
    }

    /**
     * Gets player's collision bounds.
     *
     * @return Rectangle representing collision area
     */
    public Rectangle getCollider() {
        return new Rectangle(x, y, 45, 45);
    }


    public int getHp() { return hp; }
    public int getCoins() { return coins; }
    public void earnCoins(int amount) { coins += amount; }
    public void increaseDefense() { defense += 1; if (defense > MAX_DEFENSE) defense = MAX_DEFENSE; }
    public void increaseDamage() { damage += 1; }
    public void increaseHp() { hp += 10; if (hp > 500) hp = 500; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getAttackSpeed() { return attackSpeed; }
    public int getSpeed() { return speed; }
    public int getDefense() { return defense; }
    public int getDamage() { return damage; }
    public void setCoins(int coins) { this.coins = coins; }
    public void setDoubleShotActive(boolean active) { isDoubleShotActive = active; }
    public void setForwardBackwardShotActive(boolean active) { isForwardBackwardShotActive = active; }
    public boolean isDoubleShotActive() { return isDoubleShotActive; }
    public boolean isExplosionActive() { return isExplosionActive; }
    public boolean isForwardBackwardShotActive() { return isForwardBackwardShotActive; }
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public int getCurrentFrame() { return currentFrame; }
    public int getShieldHP() { return shieldHP; }
    public int getShieldLevel() { return shieldLevel; }
    public int getPiercingLevel() { return piercingArrowsLevel; }
    public int getFireLevel() { return fireDamageLevel; }
    public int getSlowLevel() { return slowEnemiesLevel; }
    public boolean hasSlowEnemies() { return slowEnemiesUnlocked; }
    public int getExplosionRangeLevel() { return explosionRangeLevel; }
    public int getRegenerationLevel() { return regenerationLevel; }
    public ArrayList<Explosion> getExplosions() { return explosions; }
    public long getLastDashTime() { return lastDashTime; }
    public long getDashCooldown() { return dashCooldown; }
    public long getLastExplosionTime() { return lastExplosionTime; }
    public long getExplosionCooldown() { return explosionCooldown; }
    public long getLastShieldRegenerationTime() { return lastShieldRegenerationTime; }
    public long getLastHpRegenerationTime() { return lastHpRegenerationTime; }
    public void setLastShieldRegenerationTime(long time) { lastShieldRegenerationTime = time; }
    public void setLastHpRegenerationTime(long time) { lastHpRegenerationTime = time; }
    public void setIdleAnimationStartTime(long time) { idleAnimationStartTime = time; }
    public void setLastMovementTime(long time) { lastMovementTime = time; }
    public void setLastFrameChange(long time) { lastFrameChange = time; }
    public void setCurrentFrame(int frame) { currentFrame = frame; }
    public long getFrameDuration() { return frameDuration; }
    public long getLastFrameChange() { return lastFrameChange; }
    public int getDashSpeed() { return dashSpeed; }
    public int getDashDistance() { return dashDistance; }
    public PlayerGraphics getGraphics() { return graphics; }
    public void setHp(int hp) { this.hp = hp; }
    public void setShieldHP(int shieldHP) { this.shieldHP = shieldHP; }
    public void setLastDashTime(long lastDashTime) { this.lastDashTime = lastDashTime; }
    public long getLastMovementTime() { return lastMovementTime; }
    public boolean isUp() { return movement.isUp(); }
    public boolean isDown() { return movement.isDown(); }
    public boolean isLeft() { return movement.isLeft(); }
    public boolean isRight() { return movement.isRight(); }
    public boolean isIdle() { return movement.isIdle(); }
    public void setSlowEnemiesUnlocked(boolean unlocked) { slowEnemiesUnlocked = unlocked; }
    public int getCritChance() { return critChance; }
    public void setCritChance(int critChance) {
        this.critChance = Math.min(critChance, MAX_CRIT_CHANCE);
    }

    /**
     * Upgrades player's slow ability, increasing effectiveness.
     */
    public void upgradeSlow() {
        if (slowEnemiesLevel < 3) {
            slowEnemiesLevel++;
        }
        slowEnemiesUnlocked = true;
    }

    /**
     * Upgrades arrow piercing ability.
     */
    public void upgradePiercing() {
        if (piercingArrowsLevel < 3) piercingArrowsLevel += 2;
    }

    /**
     * Upgrades fire damage ability.
     */
    public void upgradeFire() {
        if (fireDamageLevel < 3) {
            fireDamageLevel++;
        }
    }

    public void upgradeSpeed() {
        if (speedBoostLevel < 3) speedBoostLevel++;
        speed = 5 + (speedBoostLevel * 2);
    }

    public void upgradeExplosion() {
        if (explosionRangeLevel < 3) explosionRangeLevel++;
        explosionRange = 100 + (explosionRangeLevel * 50);
    }

    public void upgradeRegeneration() {
        if (regenerationLevel < 3) {
            regenerationLevel++;
        }
    }

    /**
     * Applies damage to player, considering defense and shield.
     *
     * @param damage Raw damage amount before reductions
     */
    public void hit(int damage) {
        int reducedDamage = damage;
        if (defense > 0) {
            reducedDamage = (int) Math.ceil(damage * (100 - defense) / 100.0);
        }

        if (shieldLevel > 0) {
            int remainingDamage = reducedDamage - shieldHP;
            shieldHP = Math.max(0, shieldHP - reducedDamage);
            if (remainingDamage > 0) {
                hp -= remainingDamage;
            }
        } else {
            hp -= reducedDamage;
        }
        if (hp < 0) hp = 0;
    }

    public void upgradeShield() {
        if (shieldLevel < 3) {
            shieldLevel++;
            shieldHP = MAX_SHIELD_HP;
        }
    }
}