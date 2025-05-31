package WATVA;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

import static com.sun.java.accessibility.util.AWTEventMonitor.addMouseMotionListener;

public class Player implements Serializable {
    protected static int WIDTH = (int)(50 * Game.getScaleFactor());
    public static final int HEIGHT = (int)(50 * Game.getScaleFactor());
    public static final int PANEL_WIDTH = GamePanel.PANEL_WIDTH * 4;
    public static final int PANEL_HEIGHT = GamePanel.PANEL_HEIGHT * 4;
    public static final int MAX_SHIELD_HP = 100;
    public static final long SHIELD_REGENERATION_INTERVAL = 1000;
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
    protected static Soundtrack punchSound;

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
    private int slowEnemiesLevel = 0;

    private transient PlayerGraphics graphics;
    private transient PlayerMovement movement;


    public Player(int x, int y, int hp) {
        this.x = x;
        this.y = y;
        this.hp = Math.min(hp, 500);
        this.maxHp = Math.min(hp, 500);
        punchSound = new Soundtrack("res/watva/Music/493915__damnsatinist__retro-punch.wav");
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

    protected boolean canUseExplosion() {
        return System.currentTimeMillis() - lastExplosionTime >= explosionCooldown;
    }

    public void triggerExplosion() {
        explosions.add(new Explosion(x + WIDTH / 2, y + HEIGHT / 2, explosionRange));
        lastExplosionTime = System.currentTimeMillis();
    }

    private void updateExplosions() {
        Iterator<Explosion> iterator = explosions.iterator();
        while (iterator.hasNext()) {
            Explosion explosion = iterator.next();
            explosion.update();
            if (explosion.isComplete()) {
                iterator.remove();
            }
        }
    }

    public void saveState(String filePath) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(this);
            System.out.println("Player state saved successfully: x=" + x + ", y=" + y +
                    ", hp=" + hp + ", coins=" + coins + ", damage=" + damage);
        } catch (IOException e) {
            System.err.println("Error saving player state: " + e.getMessage());
        }
    }



    public static Player loadState(String filePath) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            Player player = (Player) ois.readObject();
            player.initializeTransientFields();
            System.out.println("Player state loaded successfully: x=" + player.getX() +
                    ", y=" + player.getY() + ", hp=" + player.getHp() + ", coins=" + player.getCoins());
            return player;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading player state: " + e.getMessage());
            return null;
        }
    }

    public void saveCoins(String filePath) {
        try {
            Player existingPlayer = loadState(filePath);
            if (existingPlayer != null) {
                existingPlayer.setCoins(this.coins);
                try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
                    oos.writeObject(existingPlayer);
                    System.out.println("Coins saved successfully into player file: " + coins);
                }
            } else {
                System.err.println("No existing player found to save coins.");
            }
        } catch (Exception e) {
            System.err.println("Error saving coins to player file: " + e.getMessage());
        }
    }
    public void saveLocation(String filePath) {
        try {
            Player existingPlayer = loadState(filePath);
            if (existingPlayer != null) {
                existingPlayer.setX(this.x);
                existingPlayer.setY(this.y);
                try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
                    oos.writeObject(existingPlayer);
                }
            }
        } catch (Exception e) {
            System.err.println("Error saving coins to player file: " + e.getMessage());
        }
    }

    public Rectangle getCollider() {
        return new Rectangle(x, y, 45, 45);
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
    public void setLastShieldRegenerationTime(long time) { lastShieldRegenerationTime = time; }
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

    public void upgradeSlow() {
        if (slowEnemiesLevel < 3) {
            slowEnemiesLevel++;
        }
        slowEnemiesUnlocked = true;
    }

    public void upgradePiercing() {
        if (piercingArrowsLevel < 3) piercingArrowsLevel++;
    }

    public void setSlowEnemiesUnlocked(boolean unlocked) {
        slowEnemiesUnlocked = unlocked;
    }

    public void upgradeFire() {
        if (fireDamageLevel < 3) fireDamageLevel++;
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