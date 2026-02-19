package Bosses;

import Core.Game;
import Enemies.Enemy;
import Logic.GameLogic;
import Logic.PathFinding;
import Logic.WallManager;
import Player.Player;
import UI.GamePanel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DarkMageBoss extends Enemy {
    private static final int BOSS_SIZE = Game.scale(128);
    static final Color COLOR_METEOR_WARN = new Color(255, 100, 200, 80);

    private long lastSpecialAttackTime = 0;
    private static final long SPECIAL_ATTACK_INTERVAL = 3000;

    private boolean isChannelingMeteors = false;
    private long meteorChannelStartTime = 0;
    private List<MeteorZone> meteorZones = new ArrayList<>();
    private long lastMeteorSpawnTime = 0;
    private int meteorsToSpawn = 0;

    private Image[] meteorExplosionFrames = new Image[6];

    private Image[] bossTexturesLeft;
    private Image[] bossTexturesRight;
    private Image[] bossMeteorAttackTextures;
    private Image[] deathTextures;
    private Image hpBarFrame1;

    public boolean isDying = false;
    protected boolean isDead = false;
    private long deathStartTime = 0;
    private int deathFrame = 0;
    private static final long DEATH_FRAME_DURATION = 100;
    protected int maxHp;

    private boolean isShootingProjectiles = false;
    private long projectileAttackStartTime = 0;
    private static final long PROJECTILE_ATTACK_DURATION = 10000;
    private static final long PROJECTILE_INTERVAL = 200;
    private long lastProjectileTime = 0;
    private CopyOnWriteArrayList<DarkMageProjectile> projectiles = new CopyOnWriteArrayList<>();
    private int projectilePhase = 0;

    private int currentFrame = 0;
    private long lastFrameChange = 0;
    private long frameDuration = 100;
    private boolean movingRight = true;

    private static WallManager wallManager;
    private long lastStuckCheck = 0;

    private boolean isTeleporting = false;
    private long teleportStartTime = 0;
    private static final long TELEPORT_DURATION = 800;
    private long lastTeleportTime = 0;
    private static final long TELEPORT_COOLDOWN = 5000;
    private int teleportTargetX, teleportTargetY;
    private float teleportAlpha = 1.0f;
    private int stuckCounter = 0; // Track how long stuck
    private Point lastPosition = null;

    private Point arenaCenter;
    private int arenaRadius;

    public static void setWallManager(WallManager wm) {
        wallManager = wm;
    }

    public void setArenaCenter(int centerX, int centerY, int radius) {
        this.arenaCenter = new Point(centerX, centerY);
        this.arenaRadius = radius;
    }

    public DarkMageBoss(int x, int y, int hp) {
        super(x, y, hp, Type.DARK_MAGE_BOSS);
        this.maxHp = hp;
        this.baseSpeed = Game.scale(1.0);
        this.lastPosition = new Point(x, y);
        loadTextures();
    }

    private void loadTextures() {
        bossTexturesLeft = new Image[5];
        bossTexturesRight = new Image[5];
        bossMeteorAttackTextures = new Image[5];
        deathTextures = new Image[10];

        try {
            for (int i = 0; i < 5; i++) {
                bossTexturesLeft[i] = ImageIO.read(getClass().getResourceAsStream("/WATVA/Boss/DarkMage/DarkMage" + (i + 1) + ".png"));
                bossTexturesRight[i] = ImageIO.read(getClass().getResourceAsStream("/WATVA/Boss/DarkMage/DarkMage" + (i + 6) + ".png"));
                bossMeteorAttackTextures[i] = ImageIO.read(getClass().getResourceAsStream("/WATVA/Boss/DarkMage/DarkMage" + (i + 21) + ".png"));
            }

            for (int i = 0; i < 10; i++) {
                deathTextures[i] = ImageIO.read(getClass().getResourceAsStream("/WATVA/Boss/DarkMage/DarkMage" + (i + 11) + ".png"));
            }

            for (int i = 0; i < 6; i++) {
                try {
                    meteorExplosionFrames[i] = ImageIO.read(getClass().getResourceAsStream("/WATVA/Other/Boss_meteor" + (i + 1) + ".png"));
                } catch (Exception e) {
                    System.err.println("Could not load meteor explosion frame " + (i + 1));
                }
            }

            hpBarFrame1 = ImageIO.read(getClass().getResourceAsStream("/WATVA/Boss/DarkMage/DarkMageHPBar1.png"));
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
        } else {
            Graphics2D g2d = (Graphics2D) g;

            if (isTeleporting) {
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, teleportAlpha));
            }

            Image[] textures;
            if (isChannelingMeteors) {
                textures = bossMeteorAttackTextures;
            } else {
                textures = movingRight ? bossTexturesRight : bossTexturesLeft;
            }

            if (textures[currentFrame] != null) {
                g2d.drawImage(textures[currentFrame], x, y, BOSS_SIZE, BOSS_SIZE, null);
            }

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }

        for (MeteorZone zone : meteorZones) {
            zone.draw(g, meteorExplosionFrames);
        }

        for (DarkMageProjectile projectile : projectiles) {
            projectile.draw(g);
        }

        if (this.hp > 0) {
            drawHealthBar(g);
        }
    }

    private void drawHealthBar(Graphics g) {
        int hpBarWidth = Game.scale(320);
        int hpBarHeight = Game.scale(30);
        int hpBarX = (GamePanel.PANEL_WIDTH - hpBarWidth) / 2;
        int hpBarY = GamePanel.PANEL_HEIGHT - hpBarHeight - Game.scale(10);

        g.setColor(new Color(50, 50, 50));
        g.fillRect(hpBarX + GameLogic.cameraX, hpBarY + GameLogic.cameraY, hpBarWidth, hpBarHeight);

        g.setColor(Color.RED);
        int redWidth = (int)(Math.min(this.hp, this.maxHp) * hpBarWidth / this.maxHp);
        g.fillRect(hpBarX + GameLogic.cameraX, hpBarY + GameLogic.cameraY, redWidth, hpBarHeight);

        g.setColor(Color.BLACK);
        for (int i = 1; i < 10; i++) {
            int sectionX = hpBarX + (hpBarWidth * i / 10) + GameLogic.cameraX;
            g.drawLine(sectionX, hpBarY + GameLogic.cameraY,
                    sectionX, hpBarY + hpBarHeight + GameLogic.cameraY);
        }

        g.drawRect(hpBarX + GameLogic.cameraX, hpBarY + GameLogic.cameraY, hpBarWidth, hpBarHeight);

        if (hpBarFrame1 != null) {
            int frameWidth = Game.scale(416);
            int frameHeight = Game.scale(132);
            int frameX = hpBarX + GameLogic.cameraX - Game.scale(54);
            int frameY = hpBarY + GameLogic.cameraY - Game.scale(58);
            g.drawImage(hpBarFrame1, frameX, frameY, frameWidth, frameHeight, null);
        }
    }

    public void updateBossBehavior(Player player, CopyOnWriteArrayList<Enemy> enemies) {
        if (isDead) return;

        if (hp <= 0 && !isDying) {
            isDying = true;
            deathStartTime = System.currentTimeMillis();
            return;
        }

        long currentTime = System.currentTimeMillis();

        if (isTeleporting) {
            updateTeleport(currentTime);
            return;
        }

        if (shouldTeleportDueToWall(player, currentTime)) {
            return;
        }

        if (isChannelingMeteors) {
            updateMeteorAttack(player);
        } else if (isShootingProjectiles) {
            updateProjectileAttack(player);
            updateExistingProjectiles();
        } else if (currentTime - lastSpecialAttackTime >= SPECIAL_ATTACK_INTERVAL) {
            chooseRandomAttack(enemies);
            lastSpecialAttackTime = currentTime;
        }

        if (!isChannelingMeteors && !isShootingProjectiles) {
            moveTowardsPlayer(player);
        }

        if (currentTime - lastFrameChange >= frameDuration) {
            currentFrame = (currentFrame + 1) % 5;
            lastFrameChange = currentTime;
        }
    }

    private boolean shouldTeleportDueToWall(Player player, long currentTime) {
        if (currentTime - lastTeleportTime < TELEPORT_COOLDOWN) {
            return false;
        }

        if (lastPosition != null) {
            int distMoved = (int) Math.hypot(x - lastPosition.x, y - lastPosition.y);

            if (distMoved < Game.scale(5)) {
                stuckCounter++;
            } else {
                stuckCounter = 0;
            }
        }

        lastPosition = new Point(x, y);

        if (stuckCounter < 10) {
            return false;
        }

        int playerCenterX = player.getX() + Player.WIDTH / 2;
        int playerCenterY = player.getY() + Player.HEIGHT / 2;
        int bossCenterX = x + BOSS_SIZE / 2;
        int bossCenterY = y + BOSS_SIZE / 2;

        boolean hasLineOfSight = PathFinding.hasClearPath(
                bossCenterX, bossCenterY,
                playerCenterX, playerCenterY,
                wallManager
        );

        if (!hasLineOfSight) {
            startTeleport(player);
            stuckCounter = 0;
            return true;
        }

        return false;
    }

    private void startTeleport(Player player) {
        isTeleporting = true;
        teleportStartTime = System.currentTimeMillis();
        lastTeleportTime = teleportStartTime;
        teleportAlpha = 1.0f;

        int attempts = 0;
        int maxAttempts = 20;

        while (attempts < maxAttempts) {
            int range = Game.scale(200);
            int offsetX = (int)((Math.random() - 0.5) * 2 * range);
            int offsetY = (int)((Math.random() - 0.5) * 2 * range);

            teleportTargetX = player.getX() + offsetX;
            teleportTargetY = player.getY() + offsetY;

            if (wallManager == null || !wallManager.isWall(
                    teleportTargetX + BOSS_SIZE/2,
                    teleportTargetY + BOSS_SIZE/2)) {
                break;
            }
            attempts++;
        }

        if (attempts >= maxAttempts) {
            teleportTargetX = player.getX();
            teleportTargetY = player.getY();
        }
    }

    private void updateTeleport(long currentTime) {
        long elapsed = currentTime - teleportStartTime;

        if (elapsed < TELEPORT_DURATION / 2) {
            teleportAlpha = 1.0f - (elapsed / (float)(TELEPORT_DURATION / 2));
        } else if (elapsed < TELEPORT_DURATION) {
            if (teleportAlpha != 0) {
                x = teleportTargetX;
                y = teleportTargetY;
                teleportAlpha = 0;
            }
        } else {
            teleportAlpha = (elapsed - TELEPORT_DURATION) / (float)(TELEPORT_DURATION / 2);

            if (teleportAlpha >= 1.0f) {
                teleportAlpha = 1.0f;
                isTeleporting = false;
            }
        }
    }

    private void moveTowardsPlayer(Player player) {
        int deltaX = (player.getX() + Player.WIDTH/2) - (x + BOSS_SIZE/2);
        int deltaY = (player.getY() + Player.HEIGHT/2) - (y + BOSS_SIZE/2);
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

        if (distance > Game.scale(100)) {
            movingRight = deltaX > 0;

            double normalizedX = deltaX / distance;
            double normalizedY = deltaY / distance;

            int nextX = x + (int)(normalizedX * baseSpeed);
            int nextY = y + (int)(normalizedY * baseSpeed);

            boolean hitWall = false;
            if (wallManager != null) {
                int centerNextX = nextX + BOSS_SIZE/2;
                int centerNextY = nextY + BOSS_SIZE/2;
                hitWall = wallManager.isWall(centerNextX, centerNextY);
            }

            if (!hitWall) {
                x = nextX;
                y = nextY;
            }
        }
    }

    private void chooseRandomAttack(CopyOnWriteArrayList<Enemy> enemies) {
        int choice = (int) (Math.random() * 3);

        switch (choice) {
            case 0 -> startMeteorAttack();
            case 1 -> startProjectileAttack();
            case 2 -> summonMinions(enemies);
        }
    }

    private void startMeteorAttack() {
        isChannelingMeteors = true;
        meteorChannelStartTime = System.currentTimeMillis();
        meteorsToSpawn = 25;
        meteorZones.clear();
    }

    private void updateMeteorAttack(Player player) {
        long currentTime = System.currentTimeMillis();

        if (meteorsToSpawn > 0 && currentTime - lastMeteorSpawnTime > 150) {
            spawnSingleMeteor(player);
            lastMeteorSpawnTime = currentTime;
            meteorsToSpawn--;
        }

        Iterator<MeteorZone> it = meteorZones.iterator();
        while (it.hasNext()) {
            MeteorZone zone = it.next();
            zone.update();

            if (zone.shouldDealDamage() && !zone.hasDealtDamage) {
                double dist = Math.hypot(
                        (zone.x + zone.radius) - (player.getX() + Player.WIDTH/2.0),
                        (zone.y + zone.radius) - (player.getY() + Player.HEIGHT/2.0)
                );

                if (dist < zone.radius) {
                    player.hit(15);
                    zone.hasDealtDamage = true;
                }
            }

            if (zone.isFinished()) {
                it.remove();
            }
        }

        if (meteorsToSpawn <= 0 && meteorZones.isEmpty()) {
            isChannelingMeteors = false;
        }
    }

    private void spawnSingleMeteor(Player player) {
        int range = Game.scale(400);
        int mx = player.getX() + (int)((Math.random() - 0.5) * 2 * range);
        int my = player.getY() + (int)((Math.random() - 0.5) * 2 * range);

        int radius = Game.scale(80);
        meteorZones.add(new MeteorZone(mx, my, radius));
    }



    private void startProjectileAttack() {
        isShootingProjectiles = true;
        projectileAttackStartTime = System.currentTimeMillis();
        lastProjectileTime = projectileAttackStartTime;
        projectilePhase = 0;
    }

    private void updateProjectileAttack(Player player) {
        long currentTime = System.currentTimeMillis();

        if (currentTime - projectileAttackStartTime >= PROJECTILE_ATTACK_DURATION) {
            isShootingProjectiles = false;
        }

        if (isShootingProjectiles && currentTime - lastProjectileTime >= PROJECTILE_INTERVAL) {
            shootProjectiles(player);
            lastProjectileTime = currentTime;
            projectilePhase = (projectilePhase + 1) % 4;
        }
    }

    private void updateExistingProjectiles() {
        projectiles.removeIf(p -> {
            p.update();
            if (wallManager != null && wallManager.isWall(p.getX(), p.getY())) {
                p.setActive(false);
            }
            return !p.isActive();
        });
    }

    private void shootProjectiles(Player player) {
        int centerX = x + BOSS_SIZE / 2;
        int centerY = y + BOSS_SIZE / 2;

        switch (projectilePhase) {
            case 0:
                for (int i = 0; i < 12; i++) {
                    double a = Math.toRadians(i * 30);
                    projectiles.add(new DarkMageProjectile(centerX, centerY, Math.cos(a), Math.sin(a)));
                }
                break;
            case 1:
                double dx = player.getX() - centerX, dy = player.getY() - centerY;
                double d = Math.sqrt(dx * dx + dy * dy);
                if (d > 0) projectiles.add(new DarkMageProjectile(centerX, centerY, dx / d, dy / d));
                break;
            case 2:
                for (int i = 0; i < 6; i++) {
                    double a = Math.toRadians((i * 60) + (System.currentTimeMillis() % 360));
                    projectiles.add(new DarkMageProjectile(centerX, centerY, Math.cos(a), Math.sin(a)));
                }
                break;
            case 3:
                for (int i = 0; i < 4; i++) {
                    double a = Math.toRadians(i * 90);
                    projectiles.add(new DarkMageProjectile(centerX, centerY, Math.cos(a), Math.sin(a)));
                    projectiles.add(new DarkMageProjectile(centerX, centerY, Math.cos(a + 0.2), Math.sin(a + 0.2)));
                    projectiles.add(new DarkMageProjectile(centerX, centerY, Math.cos(a - 0.2), Math.sin(a - 0.2)));
                }
                break;
        }
    }

    public void checkProjectileCollisions(Player player) {
        Rectangle playerCollider = player.getCollider();
        for (DarkMageProjectile projectile : projectiles) {
            if (projectile.isActive() && projectile.getCollider().intersects(playerCollider)) {
                player.hit(10);
                projectile.setActive(false);
            }
        }
    }

    public void summonMinions(CopyOnWriteArrayList<Enemy> enemies) {
        int radius = Game.scale(150);
        int minionCount = 6;

        for (int i = 0; i < minionCount; i++) {
            double angle = 2 * Math.PI / minionCount * i;
            int offsetX = (int) (Math.cos(angle) * radius);
            int offsetY = (int) (Math.sin(angle) * radius);

            int spawnX = x + offsetX;
            int spawnY = y + offsetY;

            if (wallManager == null || !wallManager.isWall(spawnX + 25, spawnY + 25)) {
                enemies.add(new Enemy(spawnX, spawnY, 1, Type.ZOMBIE));
            }
        }
    }

    @Override
    public Rectangle getCollider() {
        int padding = Game.scale(30);
        return new Rectangle(x + padding, y + padding, BOSS_SIZE - padding*2, BOSS_SIZE - padding*2);
    }

    @Override
    public int getWidth() { return BOSS_SIZE; }
    public boolean isDead() { return isDead; }
    public boolean isDying() { return isDying; }
}