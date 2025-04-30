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
    protected static int WIDTH = 48;
    private ArrayList<Explosion> explosions = new ArrayList<>();
    private long explosionCooldown = 5000;
    private long lastExplosionTime = 0;
    private int explosionRange = 100;
    public static final int HEIGHT = 48;
    private int x, y, hp;
    private int speed = 5;
    private int heal = 0;
    private int coins = 0;
    private int damage = 5;
    private int attackSpeed = 5;
    private int defense = 0;
    private boolean up, down, left, right;
    private int currentFrame = 0;
    private long lastFrameChange = 0;
    private long frameDuration = 200;
    private transient Image[] rightTextures, leftTextures, idleTextures, upTextures, downTextures;
    private transient Image hpBarFrame1, hpBarFrame2, hpBarFrame3;

    public static final int PANEL_WIDTH = 6120;
    public static final int PANEL_HEIGHT = 3600;
    private boolean dashing = false;
    private int dashDistance = 100;
    private int dashSpeed = 20;
    private int dashDirectionX = 0, dashDirectionY = 0;
    private int dashProgress = 0;
    private long dashCooldown = 5000;
    private long lastDashTime = 0;
    private boolean meleeMode = false;
    private int mouseX, mouseY;
    private long meleeAttackStartTime = 0;

    private int maxHp;
    private static final long MELEE_ATTACK_DURATION = 50;
    private static final long serialVersionUID = 1L;
    protected static Soundtrack punchSound;
    private boolean isExplosionActive = false;
    private boolean isDoubleShotActive = false;
    private boolean isForwardBackwardShotActive = false;
    private transient MeleeAttack meleeAttack;
    private boolean isMeleeMode = false;

    private int piercingArrowsLevel = 0;
    private boolean slowEnemiesUnlocked = false;
    private int fireDamageLevel = 0;
    private int speedBoostLevel = 0;
    private int explosionRangeLevel = 0;
    private int regenerationLevel = 0;
    private int shieldLevel = 0;
    private int shieldHP = 0;
    private static final int MAX_SHIELD_HP = 100;
    private static final long SHIELD_REGENERATION_INTERVAL = 1000;
    private long lastShieldRegenerationTime = 0;


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
        rightTextures = loadTextures("Player1", "Player2", "Player3", "Player4");
        leftTextures = loadTextures("Player5", "Player6", "Player7", "Player8");
        upTextures = loadTextures("Player9", "Player10", "Player11", "Player12");
        downTextures = loadTextures("Player13", "Player14", "Player15", "Player16");
        idleTextures = loadTextures("Player17", "Player18", "Player19", "Player20");
        try {
            hpBarFrame1 = ImageIO.read(new File("res/watva/background/HPBar1.png"));
            hpBarFrame2 = ImageIO.read(new File("res/watva/background/HPBar2.png"));
            hpBarFrame3 = ImageIO.read(new File("res/watva/background/HPBar3.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        meleeAttack = new MeleeAttack(0, 0, 0);
    }

    private Image[] loadTextures(String... filenames) {
        Image[] textures = new Image[filenames.length];
        try {
            for (int i = 0; i < filenames.length; i++) {
                textures[i] = ImageIO.read(new File("res/watva/player/" + filenames[i] + ".png"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return textures;
    }

    public void draw(Graphics g) {
        Image[] textures = idleTextures;

        if (right) {
            textures = rightTextures;
        } else if (left) {
            textures = leftTextures;
        } else if (up) {
            textures = upTextures;
        } else if (down) {
            textures = downTextures;
        }

        g.drawImage(textures[currentFrame], x, y, WIDTH, HEIGHT, null);

        drawDashCooldown(g);
        for (Explosion explosion : explosions) {
            explosion.draw(g);
        }
        drawExplosionCooldown(g);

        int hpBarWidth = 200;
        int hpBarHeight = 20;
        int hpBarX = GamePanel.PANEL_WIDTH - hpBarWidth - 10 + GamePanel.cameraX;
        int hpBarY = 29 + GamePanel.cameraY;

        if (hp > 0) {
            g.setColor(Color.BLACK);
            g.drawRect(hpBarX, hpBarY, hpBarWidth, hpBarHeight);
        }
        if (shieldLevel > 0) {
            int shieldWidth = shieldHP * hpBarWidth / MAX_SHIELD_HP;
            g.setColor(Color.BLUE);
            g.fillRect(hpBarX, hpBarY + hpBarHeight + 5, shieldWidth, hpBarHeight);
            g.setColor(Color.BLACK);
            g.drawRect(hpBarX, hpBarY + hpBarHeight + 5, hpBarWidth, hpBarHeight);
        }

        if (hp > 0) {
            int redWidth = Math.min(hp, 100) * hpBarWidth / 100;
            g.setColor(Color.RED);
            g.fillRect(hpBarX, hpBarY, redWidth, hpBarHeight);
            if (hpBarFrame1 != null) {
                g.drawImage(hpBarFrame1, hpBarX - 35, hpBarY - 30, hpBarWidth + 35, hpBarHeight + 45, null);
            }
        }

        if (hp > 100) {
            int purpleWidth = Math.min(hp - 100, 200) * hpBarWidth / 200;
            g.setColor(Color.MAGENTA);
            g.fillRect(hpBarX, hpBarY, purpleWidth, hpBarHeight);
            if (hpBarFrame2 != null) {
                g.drawImage(hpBarFrame2, hpBarX - 35, hpBarY - 30, hpBarWidth + 35, hpBarHeight + 45, null);
            }
        }

        if (hp > 300) {
            int goldWidth = Math.min(hp - 300, 200) * hpBarWidth / 200;
            g.setColor(Color.YELLOW);
            g.fillRect(hpBarX, hpBarY, goldWidth, hpBarHeight);
            if (hpBarFrame3 != null) {
                g.drawImage(hpBarFrame3, hpBarX - 35, hpBarY - 30, hpBarWidth + 35, hpBarHeight + 45, null);
            }
        }

        if (hp > 0) {
            g.setColor(Color.BLACK);
            for (int i = 1; i <= 9; i++) {
                int dividerX = hpBarX + (i * hpBarWidth / 10);
                g.drawLine(dividerX, hpBarY - 3, dividerX, hpBarY + hpBarHeight);
            }
        }
        if (isMeleeMode && System.currentTimeMillis() - meleeAttackStartTime < MELEE_ATTACK_DURATION) {
            meleeAttack.draw((Graphics2D) g);
        }
    }

    public void toggleMeleeMode() {
        isMeleeMode = !isMeleeMode;
    }

    public boolean isMeleeMode() {
        return isMeleeMode;
    }
    public boolean isMeleeAttackActive() {
        return isMeleeMode;
    }

    public MeleeAttack getMeleeAttack() {
        return meleeAttack;
    }

    public void performMeleeAttack(int mouseX, int mouseY) {
        if (!isMeleeMode) return;

        int centerX = x + WIDTH / 2;
        int centerY = y + HEIGHT / 2;
        int angle = (int) Math.toDegrees(Math.atan2(mouseY - centerY, mouseX - centerX));

        meleeAttack.setPosition(centerX, centerY, angle);
        meleeAttackStartTime = System.currentTimeMillis();
        if (punchSound != null) {
            punchSound.playOnce();
        }
    }

    public long getMeleeAttackStartTime() {
        return meleeAttackStartTime;
    }

    private void drawDashCooldown(Graphics g) {
        long timeSinceLastDash = System.currentTimeMillis() - lastDashTime;
        if (timeSinceLastDash < dashCooldown) {
            double percentage = 1 - (double) timeSinceLastDash / dashCooldown;

            int radius = 30;
            int centerX = 50 + GamePanel.cameraX;
            int centerY = GamePanel.PANEL_HEIGHT - 50 + GamePanel.cameraY;

            g.setColor(Color.RED);
            g.fillArc(centerX - radius, centerY - radius, radius * 2, radius * 2, 90, (int) (360 * percentage));
        }
    }

    private void drawExplosionCooldown(Graphics g) {
        long timeSinceLastExplosion = System.currentTimeMillis() - lastExplosionTime;
        if (timeSinceLastExplosion < explosionCooldown) {
            double percentage = 1 - (double) timeSinceLastExplosion / explosionCooldown;

            int radius = 30;
            int centerX = 100 + GamePanel.cameraX;
            int centerY = GamePanel.PANEL_HEIGHT - 50 + GamePanel.cameraY;

            g.setColor(Color.ORANGE);
            g.fillArc(centerX - radius, centerY - radius, radius * 2, radius * 2, 90, (int) (360 * percentage));
        }
    }
    public ArrayList<Explosion> getExplosions() {
        return explosions;
    }

    public void move() {
        boolean moving = false;
        int edgeLimit = 61;
        long currentTime = System.currentTimeMillis();
        if (shieldLevel > 0 && currentTime - lastShieldRegenerationTime >= SHIELD_REGENERATION_INTERVAL) {
            shieldHP = Math.min(shieldHP + 1, MAX_SHIELD_HP);
            lastShieldRegenerationTime = currentTime;
        }

        if (regenerationLevel > 0 && hp < 100) {
            hp = Math.min(hp + regenerationLevel, 100);
        }

        if (dashing) {
            x += dashDirectionX * dashSpeed;
            y += dashDirectionY * dashSpeed;
            dashProgress += dashSpeed;

            if (dashProgress >= dashDistance || x < edgeLimit || y < edgeLimit ||
                    x + WIDTH > PANEL_WIDTH - edgeLimit || y + HEIGHT > PANEL_HEIGHT - edgeLimit) {
                dashing = false;
            }
            return;
        }

        if (up && y - speed >= edgeLimit) {
            y -= speed;
            moving = true;
        }

        if (down && y + speed + HEIGHT <= PANEL_HEIGHT - edgeLimit + 25) {
            y += speed;
            moving = true;
        }

        if (left && x - speed >= edgeLimit) {
            x -= speed;
            moving = true;
        }

        if (right && x + speed + WIDTH <= PANEL_WIDTH - edgeLimit) {
            x += speed;
            moving = true;
        }

        if (x < edgeLimit) x = edgeLimit;
        if (y < edgeLimit) y = edgeLimit;
        if (x + WIDTH > PANEL_WIDTH - edgeLimit) x = PANEL_WIDTH - edgeLimit - WIDTH;
        if (y + HEIGHT > PANEL_HEIGHT - edgeLimit) y = PANEL_HEIGHT - edgeLimit - HEIGHT;

        if (moving) {
            if (currentTime - lastFrameChange >= frameDuration) {
                currentFrame = (currentFrame + 1) % 4;
                lastFrameChange = currentTime;
            }
        } else {
            currentFrame = 0;
        }
        updateExplosions();
    }


    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP) { up = true; }
        if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN) { down = true; }
        if (key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT) { left = true; }
        if (key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT) { right = true; }
        if (key == KeyEvent.VK_SHIFT && canDash()) {
            startDash();
        }
        if (key == KeyEvent.VK_Q && canUseExplosion()) {
            triggerExplosion();
        }
    }

    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP) { up = false; }
        if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN) { down = false; }
        if (key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT) { left = false; }
        if (key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT) { right = false; }
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
    private boolean canDash() {
        return System.currentTimeMillis() - lastDashTime >= dashCooldown;
    }

    private void startDash() {
        dashing = true;
        dashProgress = 0;
        lastDashTime = System.currentTimeMillis();

        dashDirectionX = 0;
        dashDirectionY = 0;

        if (up) dashDirectionY = -1;
        if (down) dashDirectionY = 1;
        if (left) dashDirectionX = -1;
        if (right) dashDirectionX = 1;

        if (up && left) {
            dashDirectionX = -1;
            dashDirectionY = -1;
        } else if (up && right) {
            dashDirectionX = 1;
            dashDirectionY = -1;
        } else if (down && left) {
            dashDirectionX = -1;
            dashDirectionY = 1;
        } else if (down && right) {
            dashDirectionX = 1;
            dashDirectionY = 1;
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

    public int getHp() {
        return hp;
    }
    public int getCoins() {
        return coins;
    }
    public void earnCoins(int amount) {
        coins += amount;
    }
    public void spendCoins(int amount) {
        coins -= amount;
    }
    public void increaseAttackSpeed() {
        attackSpeed -= 50;
    }
    public void increaseSpeed() {
        speed += 1;
    }
    public void increaseHeal() {
        hp += 1;
        if (hp > 500) hp = 500;
    }
    public void increaseDefense() {
        defense += 1;
    }
    public void increaseDamage() {
        damage += 1;
    }

    public void increaseHp() {
        hp += 10;
        if (hp > 500) hp = 500;
    }

    public int getX() {
        return x;
    }
    public int getY() {
        return y;
    }
    public int getAttackSpeed() { return attackSpeed; }
    public int getSpeed() { return speed; }
    public int getDefense() { return defense; }

    public int getDamage() {
        return damage;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public int getHeal() {
        return heal;
    }
    public void setDoubleShotActive(boolean active) {
        isDoubleShotActive = active;
    }

    public void setForwardBackwardShotActive(boolean active) {
        isForwardBackwardShotActive = active;
    }
    public boolean isDoubleShotActive() {
        return isDoubleShotActive;
    }
    public boolean isExplosionActive() {
        return isExplosionActive;
    }

    public boolean isForwardBackwardShotActive() {
        return isForwardBackwardShotActive;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }
    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }
    public void hitEnemy(Enemy enemy) {
        if (slowEnemiesUnlocked) {
            enemy.applySlow(3000);
        }
        if (fireDamageLevel > 0) {
            enemy.setFire(fireDamageLevel, 3000);
        }
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

    public int getPiercingLevel() { return piercingArrowsLevel; }
    public boolean hasSlowEnemies() { return slowEnemiesUnlocked; }
    public int getFireLevel() { return fireDamageLevel; }
    public int getShieldLevel() { return shieldLevel; }

    public void hit(int damage) {
        if (shieldLevel > 0) {
            int remainingDamage = damage - shieldHP;
            shieldHP = Math.max(0, shieldHP - damage);
            if (remainingDamage > 0) {
                hp -= remainingDamage;
            }
        } else {
            hp -= damage;
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
