package WATVA;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The Dark Mage boss enemy with special attacks and abilities.
 */
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
    public boolean isDying = false;
    protected boolean isDead = false;
    private long deathStartTime = 0;
    private int deathFrame = 0;
    private static final long DEATH_FRAME_DURATION = 100;
    private transient Image hpBarFrame1;

    private boolean isShootingProjectiles = false;
    private long projectileAttackStartTime = 0;
    private static final long PROJECTILE_ATTACK_DURATION = 10000;
    private static final long PROJECTILE_INTERVAL = 200;
    private long lastProjectileTime = 0;
    private CopyOnWriteArrayList<DarkMageProjectile> projectiles = new CopyOnWriteArrayList<>();
    private int projectilePhase = 0;
    protected int maxHp;

    /**
     * Creates a new Dark Mage boss at specified position with given health.
     * @param x The x-coordinate of the boss
     * @param y The y-coordinate of the boss
     * @param hp The initial health points of the boss
     */
    public DarkMageBoss(int x, int y, int hp) {
        super(x, y, hp, Type.DARK_MAGE_BOSS);
        this.baseSpeed = 1;
        this.maxHp = hp;

        bossTexturesLeft = new Image[5];
        bossTexturesRight = new Image[5];
        bossAreaAttackTextures = new Image[5];
        deathTextures = new Image[10];
        try {
            for (int i = 0; i < 5; i++) {
                bossTexturesLeft[i] = ImageIO.read(getClass().getResourceAsStream("/watva/boss/darkMage/darkMage" + (i + 1) + ".png"));
                bossTexturesRight[i] = ImageIO.read(getClass().getResourceAsStream("/watva/boss/darkMage/darkMage" + (i + 6) + ".png"));
                bossAreaAttackTextures[i] = ImageIO.read(getClass().getResourceAsStream("/watva/boss/darkMage/darkMage" + (i + 6) + ".png"));
            }
            for (int i = 0; i < 10; i++) {
                deathTextures[i] = ImageIO.read(getClass().getResourceAsStream("/watva/boss/darkMage/darkMage" + (i + 11) + ".png"));
            }
            hpBarFrame1 = ImageIO.read(getClass().getResourceAsStream("/watva/boss/darkMage/DarkMageHPBar1.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Draws the boss with animations and health bar.
     * @param g The Graphics object to draw with
     */
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
        } else if (isShootingProjectiles) {
            int animFrame = (int) ((System.currentTimeMillis() - projectileAttackStartTime) / frameDuration) % bossAreaAttackTextures.length;
            g.drawImage(bossAreaAttackTextures[animFrame], x, y, BOSS_SIZE, BOSS_SIZE, null);
        } else {
            Image[] textures = movingRight ? bossTexturesRight : bossTexturesLeft;
            g.drawImage(textures[currentFrame], x, y, BOSS_SIZE, BOSS_SIZE, null);
        }

        for (DarkMageProjectile projectile : projectiles) {
            projectile.draw(g);
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
            int hpBarWidth = (int)(320 * Game.getScaleFactor());
            int hpBarHeight = (int)(30 * Game.getScaleFactor());
            int hpBarX = (GamePanel.PANEL_WIDTH - hpBarWidth) / 2;
            int hpBarY = GamePanel.PANEL_HEIGHT - hpBarHeight - (int)(10 * Game.getScaleFactor());

            g.setColor(new Color(50, 50, 50));
            g.fillRect(hpBarX + GameLogic.cameraX, hpBarY + GameLogic.cameraY, hpBarWidth, hpBarHeight);

            g.setColor(Color.RED);
            int redWidth = (int)(Math.min(this.hp, this.maxHp) * hpBarWidth / this.maxHp);
            g.fillRect(hpBarX + GameLogic.cameraX, hpBarY + GameLogic.cameraY, redWidth, hpBarHeight);

            g.setColor(Color.BLACK);
            int numSections = 10;
            int sectionWidth = hpBarWidth / numSections;
            for (int i = 1; i < numSections; i++) {
                int sectionX = hpBarX + sectionWidth * i + GameLogic.cameraX;
                g.drawLine(sectionX, hpBarY + GameLogic.cameraY,
                        sectionX, hpBarY + hpBarHeight + GameLogic.cameraY);
            }

            g.setColor(Color.BLACK);
            g.drawRect(hpBarX + GameLogic.cameraX, hpBarY + GameLogic.cameraY, hpBarWidth, hpBarHeight);

            if (hpBarFrame1 != null) {
                int frameWidth = (int)((hpBarWidth + 96) * Game.getScaleFactor());
                int frameHeight = (int)((hpBarHeight * 4.4) * Game.getScaleFactor());
                int frameX = hpBarX + GameLogic.cameraX - (int)(54 * Game.getScaleFactor());
                int frameY = hpBarY + GameLogic.cameraY - (int)(58 * Game.getScaleFactor());

                g.drawImage(hpBarFrame1, frameX, frameY, frameWidth, frameHeight, null);
            }
        }
    }

    /**
     * Performs a dash attack towards the target position.
     * @param playerX The target x-coordinate
     * @param playerY The target y-coordinate
     */
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

    /**
     * Moves the boss towards the player position.
     * @param playerX The player's x-coordinate
     * @param playerY The player's y-coordinate
     */
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

    /**
     * Summons minion enemies around the boss.
     * @param enemies The list to add new minions to
     */
    public void summonMinions(CopyOnWriteArrayList<Enemy> enemies) {
        int radius = 150;
        int minionCount = 9;

        for (int i = 0; i < minionCount; i++) {
            double angle = 2 * Math.PI / minionCount * i;
            int offsetX = (int) (Math.cos(angle) * radius);
            int offsetY = (int) (Math.sin(angle) * radius);

            enemies.add(new Enemy(x + offsetX, y + offsetY, 1, Type.ZOMBIE));
        }
    }

    /**
     * Performs an area attack that damages the player.
     * @param player The player to damage
     */
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

    /**
     * Starts the projectile attack phase.
     */
    private void startProjectileAttack() {
        isShootingProjectiles = true;
        projectileAttackStartTime = System.currentTimeMillis();
        lastProjectileTime = projectileAttackStartTime;
        projectilePhase = 0;
    }

    /**
     * Updates the projectile attack state.
     * @param player The player target
     */
    private void updateProjectileAttack(Player player) {
        long currentTime = System.currentTimeMillis();

        if (currentTime - projectileAttackStartTime >= PROJECTILE_ATTACK_DURATION) {
            projectiles.forEach(p -> {
            });
            isShootingProjectiles = false;
        }

        if (isShootingProjectiles && currentTime - lastProjectileTime >= PROJECTILE_INTERVAL) {
            shootProjectiles(player);
            lastProjectileTime = currentTime;
            projectilePhase = (projectilePhase + 1) % 4;
        }

        for (DarkMageProjectile projectile : projectiles) {
            projectile.update();
        }

        projectiles.removeIf(p -> !p.isActive());
    }

    /**
     * Shoots projectiles in different patterns based on attack phase.
     * @param player The player target
     */
    private void shootProjectiles(Player player) {
        int centerX = x + BOSS_SIZE / 2;
        int centerY = y + BOSS_SIZE / 2;

        switch (projectilePhase) {
            case 0:
                for (int i = 0; i < 12; i++) {
                    double angle = Math.toRadians(i * 30);
                    projectiles.add(new DarkMageProjectile(
                            centerX, centerY,
                            Math.cos(angle), Math.sin(angle)
                    ));
                }
                break;

            case 1:
                double dx = player.getX() - centerX;
                double dy = player.getY() - centerY;
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist > 0) {
                    projectiles.add(new DarkMageProjectile(
                            centerX, centerY,
                            dx/dist, dy/dist
                    ));
                }
                break;

            case 2:
                for (int i = 0; i < 6; i++) {
                    double angle = Math.toRadians((i * 60) + (System.currentTimeMillis() % 360));
                    projectiles.add(new DarkMageProjectile(
                            centerX, centerY,
                            Math.cos(angle), Math.sin(angle)
                    ));
                }
                break;

            case 3:
                for (int i = 0; i < 4; i++) {
                    double angle = Math.toRadians(i * 90);
                    projectiles.add(new DarkMageProjectile(
                            centerX, centerY,
                            Math.cos(angle), Math.sin(angle)
                    ));
                    projectiles.add(new DarkMageProjectile(
                            centerX, centerY,
                            Math.cos(angle + 0.2), Math.sin(angle + 0.2)
                    ));
                    projectiles.add(new DarkMageProjectile(
                            centerX, centerY,
                            Math.cos(angle - 0.2), Math.sin(angle - 0.2)
                    ));
                }
                break;
        }
    }

    /**
     * Checks for projectile collisions with player.
     * @param player The player to check collisions against
     */
    public void checkProjectileCollisions(Player player) {
        Rectangle playerCollider = player.getCollider();

        for (DarkMageProjectile projectile : projectiles) {
            if (projectile.isActive() && projectile.getCollider().intersects(playerCollider)) {
                player.hit(10);
                projectile.setActive(false);
            }
        }
    }

    /**
     * Updates boss behavior and attack patterns.
     * @param player The player target
     * @param enemies The enemy list for minion summoning
     */
    public void updateBossBehavior(Player player, CopyOnWriteArrayList<Enemy> enemies) {
        if (isDead) return;

        if (hp <= 0 && !isDying) {
            isDying = true;
            deathStartTime = System.currentTimeMillis();
            return;
        }

        long currentTime = System.currentTimeMillis();

        if (!isDashing && !isAreaAttacking && !isPreparingForDash && !isShootingProjectiles) {
            moveTowards(player.getX(), player.getY());
        }

        if (currentTime - lastSpecialAttackTime >= SPECIAL_ATTACK_INTERVAL) {
            if (isShootingProjectiles) {
                projectiles.forEach(p -> {
                });
                isShootingProjectiles = false;
            }

            int attackType = (int)(Math.random() * 4);
            switch (attackType) {
                case 0 -> {
                    isPreparingForDash = true;
                    dashTargetX = player.getX();
                    dashTargetY = player.getY();
                    dashStartTime = currentTime;
                }
                case 1 -> areaAttack(player);
                case 2 -> summonMinions(enemies);
                case 3 -> startProjectileAttack();
            }
            lastSpecialAttackTime = currentTime;
        }

        if (isPreparingForDash || isDashing) {
            dashAttack(dashTargetX, dashTargetY);
        }

        if (isShootingProjectiles) {
            updateProjectileAttack(player);
        } else if (!projectiles.isEmpty()) {
            updateProjectileAttack(player);
        }
    }

    /**
     * @return The collision bounds of the boss
     */
    @Override
    public Rectangle getCollider() {
        return new Rectangle(x +(BOSS_SIZE / 2) / 2, y +(BOSS_SIZE / 2) / 2, BOSS_SIZE / 2, BOSS_SIZE / 2);
    }

    public int getWidth() {
        return BOSS_SIZE;
    }

    public boolean isDead() {
        return isDead;
    }

    public boolean isDying() {
        return isDying;
    }
}