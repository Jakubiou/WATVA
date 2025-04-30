package WATVA;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

public class DarkMageBoss extends Enemy {
    private static final int BOSS_SIZE = 128;
    private long lastSpecialAttackTime = 0;
    private static final long SPECIAL_ATTACK_INTERVAL = 2000;
    private long dashStartTime = 0;
    private boolean isPreparingForDash = false;
    protected boolean isDashing = false;
    private int dashTargetX;
    private int dashTargetY;
    protected boolean isAreaAttacking = false;
    private long areaAttackStartTime = 0;
    private static final int DASH_DURATION = 1000;
    private static final long DASH_PREPARE_TIME = 500;
    private static final int AREA_ATTACK_DURATION = 500;
    private Image[] bossTexturesLeft;
    private Image[] bossTexturesRight;
    private int currentFrame = 0;
    private long lastFrameChange = 0;
    private long frameDuration = 100;
    private boolean movingRight = true;
    private static final long AREA_ATTACK_DELAY = 500;
    private boolean isPreparingAreaAttack = false;
    private long areaAttackPrepareTime = 0;
    private Image[] bossAreaAttackTextures;
    private Image[] deathTextures;
    protected boolean isDying = false;
    protected boolean isDead = false;
    private long deathStartTime = 0;
    private int deathFrame = 0;
    private static final long DEATH_FRAME_DURATION = 100;

    public DarkMageBoss(int x, int y, int hp) {
        super(x, y, hp, Type.DARK_MAGE_BOSS);
        this.speed = 1;

        bossTexturesLeft = new Image[5];
        bossTexturesRight = new Image[5];
        bossAreaAttackTextures = new Image[5];
        deathTextures = new Image[10];
        try {
            for (int i = 0; i < 5; i++) {
                bossTexturesLeft[i] = ImageIO.read(new File("res/watva/boss/darkMage/darkMage" + (i + 1) + ".png"));
                bossTexturesRight[i] = ImageIO.read(new File("res/watva/boss/darkMage/darkMage" + (i + 6) + ".png"));
                bossAreaAttackTextures[i] = ImageIO.read(new File("res/watva/boss/darkMage/darkMage" + (i + 6) + ".png"));
            }
            for (int i = 0; i < 10; i++) {
                deathTextures[i] = ImageIO.read(new File("res/watva/boss/darkMage/darkMage" + (i + 11) + ".png"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void draw(Graphics g) {
        if (isDying) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - deathStartTime >= DEATH_FRAME_DURATION) {
                deathFrame++;
                deathStartTime = currentTime;
            }
            if (deathFrame < deathTextures.length) {
                g.drawImage(deathTextures[deathFrame], x, y, BOSS_SIZE, BOSS_SIZE, null);
            } else {
                isDead = true;
            }
        } else if (isPreparingAreaAttack) {
            int animFrame = (int) ((System.currentTimeMillis() - areaAttackPrepareTime) / frameDuration) % bossAreaAttackTextures.length;
            g.drawImage(bossAreaAttackTextures[animFrame], x, y, BOSS_SIZE, BOSS_SIZE, null);
        } else {
            Image[] textures = movingRight ? bossTexturesRight : bossTexturesLeft;
            g.drawImage(textures[currentFrame], x, y, BOSS_SIZE, BOSS_SIZE, null);
        }
        if (isAreaAttacking) {
            g.setColor(new Color(86, 0, 110, 100));
            int attackRadius = 400;
            g.fillOval(x - attackRadius / 2 + BOSS_SIZE / 2,
                    y - attackRadius / 2 + BOSS_SIZE / 2,
                    attackRadius, attackRadius);
        } else if (isPreparingForDash) {
            g.setColor(new Color(255, 255, 0, 100));
            g.fillOval(x - BOSS_SIZE / 2, y - BOSS_SIZE / 2, BOSS_SIZE * 2, BOSS_SIZE * 2);
        }

        if (this.hp > 0) {
            int hpBarWidth = 300;
            int hpBarHeight = 30;
            int hpBarX = (GamePanel.PANEL_WIDTH - hpBarWidth) / 2;
            int hpBarY = GamePanel.PANEL_HEIGHT - hpBarHeight - 10;

            g.setColor(new Color(50, 50, 50));
            g.fillRect(hpBarX + GamePanel.cameraX, hpBarY + GamePanel.cameraY, hpBarWidth, hpBarHeight);

            g.setColor(Color.RED);
            int redWidth = (int) (Math.min(this.hp, 500) * hpBarWidth / 500);
            g.fillRect(hpBarX + GamePanel.cameraX, hpBarY + GamePanel.cameraY, redWidth, hpBarHeight);

            g.setColor(Color.BLACK);
            int numSections = 10;
            int sectionWidth = hpBarWidth / numSections;

            for (int i = 1; i < numSections; i++) {
                int sectionX = hpBarX + sectionWidth * i + GamePanel.cameraX;
                g.drawLine(sectionX, hpBarY + GamePanel.cameraY, sectionX, hpBarY + hpBarHeight + GamePanel.cameraY);
            }

            g.setColor(Color.BLACK);
            g.drawRect(hpBarX + GamePanel.cameraX, hpBarY + GamePanel.cameraY, hpBarWidth, hpBarHeight);
        }
    }

    public void dashAttack(int playerX, int playerY) {
        long currentTime = System.currentTimeMillis();

        if (isPreparingForDash) {
            if (currentTime - dashStartTime >= DASH_PREPARE_TIME) {
                isPreparingForDash = false;
                isDashing = true;
                dashStartTime = currentTime;
            }
        } else if (isDashing) {
            double progress = (double) (currentTime - dashStartTime) / DASH_DURATION;

            if (progress < 1.0) {
                x += (dashTargetX - x) * 0.1;
                y += (dashTargetY - y) * 0.1;
            } else {
                isDashing = false;
            }
        }
    }


    @Override
    public void moveTowards(int playerX, int playerY) {
        double deltaX = playerX - x;
        movingRight = deltaX > 0;

        super.moveTowards(playerX, playerY);

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameChange >= frameDuration) {
            currentFrame = (currentFrame + 1) % bossTexturesLeft.length;
            lastFrameChange = currentTime;
        }
    }

    public void summonMinions(CopyOnWriteArrayList<Enemy> enemies) {
        int radius = 150;
        int minionCount = 6;

        for (int i = 0; i < minionCount; i++) {
            double angle = 2 * Math.PI / minionCount * i;
            int offsetX = (int) (Math.cos(angle) * radius);
            int offsetY = (int) (Math.sin(angle) * radius);

            enemies.add(new Enemy(x + offsetX, y + offsetY, 50, Type.SHOOTING));
        }
    }


    public void areaAttack(Player player) {
        long currentTime = System.currentTimeMillis();

        if (isPreparingAreaAttack) {
            if (currentTime - areaAttackPrepareTime >= AREA_ATTACK_DELAY) {
                isPreparingAreaAttack = false;
                isAreaAttacking = true;
                areaAttackStartTime = currentTime;
            }
        } else if (isAreaAttacking) {
            if (currentTime - areaAttackStartTime <= AREA_ATTACK_DURATION) {
                int attackRadius = 400;
                double distance = Math.sqrt(Math.pow(player.getX() - (x + BOSS_SIZE / 2), 2) +
                        Math.pow(player.getY() - (y + BOSS_SIZE / 2), 2));
                if (distance <= attackRadius / 2) {
                    player.hit(1);
                }
            } else {
                isAreaAttacking = false;
            }
        } else {
            isPreparingAreaAttack = true;
            areaAttackPrepareTime = currentTime;
        }
    }
    public void updateBossBehavior(Player player, CopyOnWriteArrayList<Enemy> enemies) {
        if (isDead) {
            return;
        }

        if (hp <= 0 && !isDying) {
            isDying = true;
            deathStartTime = System.currentTimeMillis();
            return;
        }

        if (isDying) {
            if (deathFrame >= deathTextures.length) {
                isDead = true;
            }
            return;
        }

        long currentTime = System.currentTimeMillis();

        if (!isDashing && !isAreaAttacking && !isPreparingForDash) {
            moveTowards(player.getX(), player.getY());
        }

        if (currentTime - lastSpecialAttackTime >= SPECIAL_ATTACK_INTERVAL) {
            int attackType = (int) (Math.random() * 3);
            switch (attackType) {
                case 0 -> {
                    isPreparingForDash = true;
                    dashTargetX = player.getX();
                    dashTargetY = player.getY();
                    dashStartTime = currentTime;
                }
                case 1 -> areaAttack(player);
                case 2 -> summonMinions(enemies);
            }
            lastSpecialAttackTime = currentTime;
        }

        if (isPreparingForDash || isDashing) {
            dashAttack(dashTargetX, dashTargetY);
        }
    }


    @Override
    public Rectangle getCollider() {
        return new Rectangle(x, y, BOSS_SIZE, BOSS_SIZE);
    }

    protected int getWidth() {
        return BOSS_SIZE;
    }

    public boolean isDead() {
        return isDead;
    }

    public boolean isDying() {
        return isDying;
    }
}