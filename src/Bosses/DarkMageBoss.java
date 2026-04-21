package Bosses;

import Core.Game;
import Enemies.Enemy;
import Logic.GameLogic;
import Logic.PathFinding;
import Logic.World.WallManager;
import Player.Player;
import UI.Game.GamePanel;

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

    private boolean isTeleporting = false;
    private long teleportStartTime = 0;
    private static final long TELEPORT_DURATION = 800;
    private long lastTeleportTime = 0;
    private static final long TELEPORT_COOLDOWN = 2000;
    private int teleportTargetX, teleportTargetY;
    private float teleportAlpha = 1.0f;
    private int stuckCounter = 0; // Track how long stuck
    private Point lastPosition = null;

    private double moveAccX = 0, moveAccY = 0;

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
        this.baseSpeed = 3.0;
        this.lastPosition = new Point(x, y);
        loadTextures();
    }

    // Pre-scale all boss textures once at load time to avoid per-frame scaling during draw
    private java.awt.image.BufferedImage prescale(String path, int w, int h) {
        try {
            java.awt.image.BufferedImage src = ImageIO.read(getClass().getResourceAsStream(path));
            if (src == null) return null;
            java.awt.image.BufferedImage dst = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D sg = dst.createGraphics();
            sg.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            sg.drawImage(src, 0, 0, w, h, null);
            sg.dispose();
            return dst;
        } catch (Exception e) {
            System.err.println("Could not load/scale: " + path);
            return null;
        }
    }

    private void loadTextures() {
        bossTexturesLeft = new Image[5];
        bossTexturesRight = new Image[5];
        bossMeteorAttackTextures = new Image[5];
        deathTextures = new Image[10];

        for (int i = 0; i < 5; i++) {
            bossTexturesLeft[i]        = prescale("/WATVA/Boss/DarkMage/DarkMage" + (i + 1)  + ".png", BOSS_SIZE, BOSS_SIZE);
            bossTexturesRight[i]       = prescale("/WATVA/Boss/DarkMage/DarkMage" + (i + 6)  + ".png", BOSS_SIZE, BOSS_SIZE);
            bossMeteorAttackTextures[i]= prescale("/WATVA/Boss/DarkMage/DarkMage" + (i + 6)  + ".png", BOSS_SIZE, BOSS_SIZE);
        }

        for (int i = 0; i < 10; i++) {
            deathTextures[i] = prescale("/WATVA/Boss/DarkMage/DarkMage" + (i + 11) + ".png", BOSS_SIZE, BOSS_SIZE);
        }

        int meteorSize = Game.scale(160); // meteor zones are drawn at radius*2
        for (int i = 0; i < 6; i++) {
            meteorExplosionFrames[i] = prescale("/WATVA/Other/Boss_meteor" + (i + 1) + ".png", meteorSize, meteorSize);
        }

        hpBarFrame1 = prescale("/WATVA/Boss/DarkMage/DarkMageHPBar1.png", Game.scale(416), Game.scale(132));
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
                g.drawImage(deathTextures[deathFrame], x, y, null);
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
                g2d.drawImage(textures[currentFrame], x, y, null);
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
            int frameX = hpBarX + GameLogic.cameraX - Game.scale(54);
            int frameY = hpBarY + GameLogic.cameraY - Game.scale(58);
            g.drawImage(hpBarFrame1, frameX, frameY, null);
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
        } else if (currentTime - lastSpecialAttackTime >= SPECIAL_ATTACK_INTERVAL) {
            chooseRandomAttack(enemies);
            lastSpecialAttackTime = currentTime;
        }

        updateExistingProjectiles();

        // Boss se pohybuje vždy – i během útoků
        moveTowardsPlayer(player);

        if (currentTime - lastFrameChange >= frameDuration) {
            currentFrame = (currentFrame + 1) % 5;
            lastFrameChange = currentTime;
        }
    }

    private long lastStuckSampleTime = 0;
    private static final long STUCK_SAMPLE_INTERVAL = 300;

    private boolean shouldTeleportDueToWall(Player player, long currentTime) {
        if (currentTime - lastTeleportTime < TELEPORT_COOLDOWN) {
            return false;
        }

        if (isChannelingMeteors || isShootingProjectiles) {
            lastPosition = new Point(x, y);
            stuckCounter = 0;
            return false;
        }

        // Měř pohyb pouze každých STUCK_SAMPLE_INTERVAL ms – ne každý frame.
        // Jinak pomalý legitimní pohyb vypadá jako zaseknutí.
        if (currentTime - lastStuckSampleTime >= STUCK_SAMPLE_INTERVAL) {
            if (lastPosition != null) {
                int distMoved = (int) Math.hypot(x - lastPosition.x, y - lastPosition.y);
                if (distMoved < Game.scale(3)) {
                    stuckCounter++;
                } else {
                    stuckCounter = 0;
                }
            }
            lastPosition = new Point(x, y);
            lastStuckSampleTime = currentTime;
        }

        // Teleportuj po 4 po sobě jdoucích intervalech bez pohybu (= ~1.2s skutečného zaseknutí)
        if (stuckCounter >= 4) {
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

        for (int attempts = 0; attempts < 30; attempts++) {
            double angle = Math.random() * Math.PI * 2;
            int dist = Game.scale(150) + (int)(Math.random() * Game.scale(150));
            int tx = player.getX() + (int)(Math.cos(angle) * dist);
            int ty = player.getY() + (int)(Math.sin(angle) * dist);
            if (!bossHitsWall(tx, ty)) {
                teleportTargetX = tx;
                teleportTargetY = ty;
                return;
            }
        }
        teleportTargetX = player.getX();
        teleportTargetY = player.getY();
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

    private boolean bossHitsWall(int bx, int by) {
        if (wallManager == null) return false;
        // Používáme stejné offsety jako getCollider() (padding = scale(30))
        // a přidáme střední body hran pro spolehlivé zachycení 2×2 sloupů
        int pad = Game.scale(30);
        int inner = BOSS_SIZE - pad;
        int mid   = BOSS_SIZE / 2;
        return wallManager.isWall(bx + pad,       by + pad)
                || wallManager.isWall(bx + inner,     by + pad)
                || wallManager.isWall(bx + pad,       by + inner)
                || wallManager.isWall(bx + inner,     by + inner)
                || wallManager.isWall(bx + mid,       by + pad)
                || wallManager.isWall(bx + mid,       by + inner)
                || wallManager.isWall(bx + pad,       by + mid)
                || wallManager.isWall(bx + inner,     by + mid)
                || wallManager.isWall(bx + mid,       by + mid);
    }

    // Cache pro pathfinding – nepočítej každý frame
    private Point cachedPathTarget = null;
    private long lastPathCalcTime = 0;
    private static final long PATH_RECALC_INTERVAL = 600;  // bylo 400ms

    // Wall-steering stav (obcházení rohů sloupů)
    private int moveStuckCounter = 0;
    private int moveSteerDir = 0;
    private int lastMoveRecordedX = Integer.MIN_VALUE;
    private int lastMoveRecordedY = Integer.MIN_VALUE;
    private long lastMoveSampleTime = 0;
    private static final long MOVE_STUCK_SAMPLE = 250;

    private void moveTowardsPlayer(Player player) {
        int targetX = player.getX() + Player.WIDTH / 2;
        int targetY = player.getY() + Player.HEIGHT / 2;
        int bossCenterX = x + BOSS_SIZE / 2;
        int bossCenterY = y + BOSS_SIZE / 2;

        double distToPlayer = Math.hypot(targetX - bossCenterX, targetY - bossCenterY);
        if (distToPlayer <= Game.scale(100)) return;

        // Rozhodni kam jít – přímá cesta nebo pathfinding
        int moveTargetX, moveTargetY;

        boolean hasLineOfSight = wallManager == null || PathFinding.hasClearPath(
                bossCenterX, bossCenterY, targetX, targetY, wallManager);

        if (hasLineOfSight) {
            moveTargetX = targetX;
            moveTargetY = targetY;
            cachedPathTarget = null;
        } else {
            long now = System.currentTimeMillis();
            if (cachedPathTarget == null || now - lastPathCalcTime > PATH_RECALC_INTERVAL) {
                Point newStep = PathFinding.findNextStepLarge(
                        bossCenterX, bossCenterY, targetX, targetY, wallManager, BOSS_SIZE);
                if (newStep != null) {
                    cachedPathTarget = newStep;
                    lastPathCalcTime = now;
                }
                // null = frame limit → cachedPathTarget zůstane starý cached krok
            }
            if (cachedPathTarget != null) {
                moveTargetX = cachedPathTarget.x;
                moveTargetY = cachedPathTarget.y;
            } else {
                moveTargetX = targetX;
                moveTargetY = targetY;
            }
        }

        // Pohyb s accumulatorem (eliminuje int-truncation při šikmém pohybu)
        double dx = moveTargetX - bossCenterX;
        double dy = moveTargetY - bossCenterY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 1) return;

        movingRight = dx > 0;
        double speed = Game.scale(baseSpeed);

        double normX = dx / dist;
        double normY = dy / dist;

        // ── Wall-steering: detekce zaseknutí o roh sloupu ────────────────────
        long nowMs = System.currentTimeMillis();
        if (nowMs - lastMoveSampleTime >= MOVE_STUCK_SAMPLE) {
            if (lastMoveRecordedX != Integer.MIN_VALUE) {
                int moved = (int) Math.hypot(x - lastMoveRecordedX, y - lastMoveRecordedY);
                if (moved < Game.scale(3)) {
                    moveStuckCounter++;
                    if (moveStuckCounter == 1) {
                        // Zvolíme smysl obcházení: ten kolmý vektor, který míří blíže k cíli
                        double perpAX = -normY, perpAY = normX;
                        double dotA = perpAX * dx + perpAY * dy;
                        moveSteerDir = (dotA >= 0) ? 1 : -1;
                    }
                    cachedPathTarget = null;
                    lastPathCalcTime = 0;
                } else {
                    moveStuckCounter = 0;
                    moveSteerDir = 0;
                }
            }
            lastMoveRecordedX = x;
            lastMoveRecordedY = y;
            lastMoveSampleTime = nowMs;
        }

        // Přimíchej kolmou složku při zaseknutí
        double moveX = normX;
        double moveY = normY;
        if (moveStuckCounter >= 1 && moveSteerDir != 0) {
            double perpX = -normY * moveSteerDir;
            double perpY =  normX * moveSteerDir;
            double steer = Math.min(moveStuckCounter * 0.35, 1.0);
            moveX = normX * (1.0 - steer) + perpX * steer;
            moveY = normY * (1.0 - steer) + perpY * steer;
            double len = Math.sqrt(moveX * moveX + moveY * moveY);
            if (len > 0.001) { moveX /= len; moveY /= len; }
        }

        moveAccX += moveX * speed;
        moveAccY += moveY * speed;

        int stepX = (int) moveAccX;
        int stepY = (int) moveAccY;
        moveAccX -= stepX;
        moveAccY -= stepY;

        if (stepX == 0 && stepY == 0) return;

        // Sub-step sliding pro bosse: pohybuj se 1px po 1px na každé ose zvlášť.
        int signX = stepX >= 0 ? 1 : -1;
        int signY = stepY >= 0 ? 1 : -1;
        int absX  = Math.abs(stepX);
        int absY  = Math.abs(stepY);

        boolean hitWallX = false;
        for (int s = 0; s < absX; s++) {
            if (!bossHitsWall(x + signX, y)) { x += signX; }
            else { hitWallX = true; moveAccX = 0; break; }
        }

        boolean hitWallY = false;
        for (int s = 0; s < absY; s++) {
            if (!bossHitsWall(x, y + signY)) { y += signY; }
            else { hitWallY = true; moveAccY = 0; break; }
        }

        // Pokud narazíme do zdi na obou osách a steering nepomohl, přehoď smysl
        if (hitWallX && hitWallY && moveStuckCounter > 4) {
            moveSteerDir = -moveSteerDir;
            moveStuckCounter = 1;
        }

        if (hitWallX || hitWallY) {
            cachedPathTarget = null;
            lastPathCalcTime = 0;
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
        meteorsToSpawn = 40;
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
            lastSpecialAttackTime = System.currentTimeMillis();
        }
    }

    private void spawnSingleMeteor(Player player) {
        int range = Game.scale(400);
        int mx = player.getX() + (int)((Math.random() - 0.5) * 2 * range);
        int my = player.getY() + (int)((Math.random() - 0.5) * 2 * range);

        int radius = Game.scale(100);
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
            lastSpecialAttackTime = currentTime;
            return;
        }

        if (currentTime - lastProjectileTime >= PROJECTILE_INTERVAL) {
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

    /** Called by Collisions — absorbs any active projectile whose centre is inside the shield arc */
    public void absorbProjectilesInArc(java.awt.geom.Arc2D arc, Player player) {
        if (arc == null) return;
        for (DarkMageProjectile proj : projectiles) {
            if (!proj.isActive()) continue;
            if (arc.contains(proj.getX(), proj.getY())) {
                player.shieldBeamAbsorbProjectile();
                proj.setActive(false);
            }
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