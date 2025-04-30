package WATVA;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Enemy {
    private ArrayList<EnemyProjectile> projectiles;
    private static final int SHOOT_RANGE = 300;
    private static final long SHOOT_INTERVAL = 5000;
    private long lastShootTime = 0;
    public static final int NORMAL_SIZE = 37;
    public static final int GIANT_SIZE = 128;
    public static final int SMALL_SIZE = 32;
    public static final int SHOOTING_SIZE = 60;
    protected int x;
    protected int y;
    protected double hp;
    protected double speed;
    private boolean isAlive;
    private Type type;
    private long lastAttackTime = 0;

    private Image[] knightTexturesRight;
    private Image[] knightTexturesLeft;
    private int currentFrame = 0;
    private long lastFrameChange = 0;
    private long frameDuration = 100;
    private boolean movingRight = true;

    private Image[] giantTextures;
    private Image texture3;
    private Image texture4;


    private boolean isOnFire = false;
    private long fireEndTime = 0;
    private int fireDamage = 0;
    private boolean isSlowed = false;
    private long slowEndTime = 0;


    public enum Type {
        NORMAL, GIANT, SMALL, SHOOTING, SLIME, DARK_MAGE_BOSS,BUNNY_BOSS
    }

    public Enemy(int x, int y, double hp, Type type) {
        this.x = x;
        this.y = y;
        this.hp = hp;
        this.type = type;
        this.isAlive = true;
        projectiles = new ArrayList<>();

        switch (type) {
            case NORMAL -> speed = 2;
            case GIANT -> speed = 1.5;
            case SMALL -> speed = 2.5;
            case SHOOTING -> speed = 1.8;
        }

        try {
            switch (type) {
                case SMALL -> texture3 = ImageIO.read(new File("res/watva/enemy/small/small.png"));
                case SHOOTING -> texture4 = ImageIO.read(new File("res/watva/enemy/mage/mage1.png"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (type == Type.NORMAL) {
            knightTexturesRight = new Image[4];
            knightTexturesLeft = new Image[4];
            try {
                knightTexturesRight[0] = ImageIO.read(new File("res/watva/enemy/knight/knight1.png"));
                knightTexturesRight[1] = ImageIO.read(new File("res/watva/enemy/knight/knight2.png"));
                knightTexturesRight[2] = ImageIO.read(new File("res/watva/enemy/knight/knight3.png"));
                knightTexturesRight[3] = ImageIO.read(new File("res/watva/enemy/knight/knight4.png"));

                knightTexturesLeft[0] = ImageIO.read(new File("res/watva/enemy/knight/knight5.png"));
                knightTexturesLeft[1] = ImageIO.read(new File("res/watva/enemy/knight/knight6.png"));
                knightTexturesLeft[2] = ImageIO.read(new File("res/watva/enemy/knight/knight7.png"));
                knightTexturesLeft[3] = ImageIO.read(new File("res/watva/enemy/knight/knight8.png"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (type == Type.GIANT) {
            giantTextures = new  Image[6];
            try {
                giantTextures[0] = ImageIO.read(new File("res/watva/enemy/golem/golem1.png"));
                giantTextures[1] = ImageIO.read(new File("res/watva/enemy/golem/golem2.png"));
                giantTextures[2] = ImageIO.read(new File("res/watva/enemy/golem/golem3.png"));
                giantTextures[3] = ImageIO.read(new File("res/watva/enemy/golem/golem4.png"));
                giantTextures[4] = ImageIO.read(new File("res/watva/enemy/golem/golem5.png"));
                giantTextures[5] = ImageIO.read(new File("res/watva/enemy/golem/golem6.png"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public boolean isAlive() {
        return isAlive;
    }

    public void draw(Graphics g) {
        if (type == Type.NORMAL) {
            Image[] textures = movingRight ? knightTexturesRight : knightTexturesLeft;
            g.drawImage(textures[currentFrame], x, y, NORMAL_SIZE, NORMAL_SIZE, null);
        } else if (type == Type.GIANT) {
            g.drawImage(giantTextures[currentFrame], x, y, GIANT_SIZE, GIANT_SIZE, null);
        } else if (type == Type.SMALL) {
            g.drawImage(texture3, x, y, SMALL_SIZE, SMALL_SIZE, null);
        }else if (type == Type.SHOOTING) {
            g.drawImage(texture4, x, y, SHOOTING_SIZE - 20, SHOOTING_SIZE, null);
        }
        if (isSlowed) {
            g.setColor(new Color(0, 0, 255, 100)); // Blue tint
            g.fillRect(x, y, getWidth(), getHeight());
        }
        if (isOnFire) {
            g.setColor(new Color(255, 0, 0, 100));
            g.fillRect(x, y, getWidth(), getHeight());
        }
    }

    public boolean isOffScreen() {
        int screenWidth = 6120;
        int screenHeight = 3600;
        return (x + getWidth() < 0 || x > screenWidth || y + getHeight() < 0 || y > screenHeight);
    }


    protected int getWidth() {
        switch (type) {
            case GIANT: return GIANT_SIZE;
            case SMALL: return SMALL_SIZE;
            case SHOOTING: return SHOOTING_SIZE;
            default: return NORMAL_SIZE;
        }
    }

    private int getHeight() {
        return getWidth();
    }

    public void moveTowards(int targetX, int targetY) {
        double deltaX = targetX - x;
        double deltaY = targetY - y;
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

        if (type == Type.SHOOTING && isInRange(targetX, targetY)) {
            stopAndShoot(targetX, targetY);
            return;
        }

        if (distance > 0) {
            double moveX = (deltaX / distance) * speed;
            double moveY = (deltaY / distance) * speed;

            x += Math.round(moveX);
            y += Math.round(moveY);

            int edgeLimit = 61;
            x = Math.max(edgeLimit, Math.min(x, Player.PANEL_WIDTH - edgeLimit - getWidth()));
            y = Math.max(edgeLimit, Math.min(y, Player.PANEL_HEIGHT - edgeLimit - getHeight()));

            movingRight = moveX > 0;

            if (type == Type.NORMAL) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastFrameChange >= frameDuration) {
                    currentFrame = (currentFrame + 1) % knightTexturesRight.length;
                    lastFrameChange = currentTime;
                }
            }

            if (type == Type.GIANT) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastFrameChange >= frameDuration) {
                    currentFrame = (currentFrame + 1) % giantTextures.length;
                    lastFrameChange = currentTime;
                }
            }
        }
    }


    private boolean isInRange(int targetX, int targetY) {
        double distance = Math.sqrt(Math.pow(targetX - x, 2) + Math.pow(targetY - y, 2));
        return distance <= SHOOT_RANGE;
    }

    private void stopAndShoot(int playerX, int playerY) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShootTime >= SHOOT_INTERVAL) {
            shootAtPlayer(playerX, playerY);
            lastShootTime = currentTime;
        }
    }

    public void shootAtPlayer(int playerX, int playerY) {
        if (isInRange(playerX, playerY)) {
            projectiles.add(new EnemyProjectile(x + SHOOTING_SIZE / 2, y + SHOOTING_SIZE / 2, playerX, playerY));
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

    public ArrayList<EnemyProjectile> getProjectiles() {
        return projectiles;
    }
    public void drawProjectiles(Graphics g) {
        for (EnemyProjectile projectile : projectiles) {
            projectile.draw(g);
        }
    }


    public Rectangle getCollider() {
        if(type == Type.NORMAL) {
            return new Rectangle(x + 24, y + 3, NORMAL_SIZE - 32, NORMAL_SIZE - 9);
        } else if (type == Type.GIANT) {
            return new Rectangle(x, y, GIANT_SIZE, GIANT_SIZE);
        }else if (type == Type.SHOOTING) {
            return new Rectangle(x, y, SHOOTING_SIZE, SHOOTING_SIZE);
        }else{
            return new Rectangle(x, y, SMALL_SIZE, SMALL_SIZE);
        }
    }

    public void hit(int damage) {
        hp -= damage;
    }

    public double getHp() {
        return hp;
    }

    public int getDamage() {
        if(type == Type.SHOOTING){
            return 20;
        }else if(type == Type.SLIME){
            return 5;
        }else if(type == Type.DARK_MAGE_BOSS){
            return 50;
        }else {
            return 10;
        }
    }

    public boolean canAttack() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAttackTime >= 1000) {
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

    public void update() {
        if (isSlowed && System.currentTimeMillis() >= slowEndTime) {
            isSlowed = false;
            speed *= 2;
        }
        if (isOnFire && System.currentTimeMillis() < fireEndTime) {
            hp -= fireDamage;
        } else if (isOnFire) {
            isOnFire = false;
        }
    }
    public void applySlow(int duration) {
        isSlowed = true;
        slowEndTime = System.currentTimeMillis() + duration;
        speed *= 0.5;
    }

    public void setFire(int damage, int duration) {
        isOnFire = true;
        fireEndTime = System.currentTimeMillis() + duration;
        fireDamage = damage;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
    public Type getType() {
        return type;
    }

}
