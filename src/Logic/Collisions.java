package Logic;

import Bosses.BunnyBoss;
import Bosses.DarkMageBoss;
import Core.Game;
import Enemies.Enemy;
import Enemies.EnemyProjectile;
import Logic.DamageNumber.DamageNumberManager;
import Logic.World.WallManager;
import Player.Player;
import Player.PlayerProjectile;
import Player.Explosion;
import UI.Game.GamePanel;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class Collisions {
    private static final Random DAMAGE_RNG = new Random();
    private Player player;
    private CopyOnWriteArrayList<Enemy> enemies;
    private CopyOnWriteArrayList<PlayerProjectile> playerProjectiles;
    private boolean gameOver;
    private DamageNumberManager damageManager;
    private WallManager wallManager;
    private GamePanel gamePanel;
    private long lastPlayerUnstuckCheck = 0;
    private static final long UNSTUCK_CHECK_INTERVAL = 500;

    public Collisions(Player player, CopyOnWriteArrayList<Enemy> enemies,
                      CopyOnWriteArrayList<PlayerProjectile> playerProjectiles,
                      DamageNumberManager damageManager, WallManager wallManager, GamePanel gamePanel) {
        this.player = player;
        this.enemies = enemies;
        this.playerProjectiles = playerProjectiles;
        this.damageManager = damageManager;
        this.wallManager = wallManager;
        this.gamePanel = gamePanel;
        this.gameOver = false;
    }

    public void checkCollisions() {
        checkPlayerUnstuck();
        checkBossProjectileCollisions();
        checkEnemyProjectileCollisions();
        checkDeadBosses();
        checkPlayerEnemyCollisions();
        checkExplosionCollisions();
        checkPlayerProjectileCollisions();
        resolveEnemyCollisions();
    }

    private void checkPlayerUnstuck() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPlayerUnstuckCheck >= UNSTUCK_CHECK_INTERVAL) {
            int centerX = player.getX() + Player.WIDTH / 2;
            int centerY = player.getY() + Player.HEIGHT / 2;

            if (wallManager.isWall(centerX, centerY)) {
                Point newPos = wallManager.unstuckFromWall(
                        player.getX(),
                        player.getY(),
                        Player.WIDTH,
                        Player.HEIGHT
                );
                player.setX(newPos.x);
                player.setY(newPos.y);
            }
            lastPlayerUnstuckCheck = currentTime;
        }
    }

    private void checkBossProjectileCollisions() {
        for (Enemy enemy : enemies) {
            if (enemy instanceof DarkMageBoss boss) {
                // Check shield first
                if (player.isShieldBeamActive()) {
                    java.awt.geom.Arc2D arc = getShieldArc(player);
                    boss.absorbProjectilesInArc(arc, player);
                }
                boss.checkProjectileCollisions(player);
            }
        }
    }

    private void checkEnemyProjectileCollisions() {
        Player p = player;
        List<EnemyProjectile> globalProjectiles = Enemy.getAllProjectiles();
        Iterator<EnemyProjectile> projectileIterator = globalProjectiles.iterator();

        while (projectileIterator.hasNext()) {
            EnemyProjectile projectile = projectileIterator.next();
            if (!projectile.isActive()) { projectileIterator.remove(); continue; }

            // Arc shield absorb check
            if (p.isShieldBeamActive()) {
                java.awt.geom.Arc2D arc = getShieldArc(p);
                if (arc.contains(projectile.getCollider().getCenterX(), projectile.getCollider().getCenterY())) {
                    p.shieldBeamAbsorbProjectile();
                    projectileIterator.remove();
                    continue;
                }
            }

            if (projectile.checkCollisionWithPlayer(p)) {
                p.hit(20);
                projectileIterator.remove();
                if (p.getHp() <= 0) gameOver = true;
                break;
            }
        }
    }

    public static java.awt.geom.Arc2D getShieldArc(Player p) {
        int cx = p.getX() + Player.WIDTH / 2;
        int cy = p.getY() + Player.HEIGHT / 2;
        int r = (int)(Player.WIDTH * 1.35);  // reasonable half-circle size

        double dx = p.getShieldMouseX() - cx;
        double dy = p.getShieldMouseY() - cy;
        // Screen Y grows downward, so negate dy for standard math angle
        double angleDeg = Math.toDegrees(Math.atan2(-dy, dx));
        // Semicircle centred on direction to mouse. Arc2D uses CCW-positive angles.
        double arcStart = angleDeg - 90.0;
        return new java.awt.geom.Arc2D.Double(
                cx - r, cy - r, r * 2, r * 2,
                arcStart, 180.0,
                java.awt.geom.Arc2D.PIE);
    }

    private void checkDeadBosses() {
        List<Enemy> enemiesToRemove = new ArrayList<>();

        for (Enemy enemy : enemies) {
            if (enemy instanceof DarkMageBoss darkMageBoss) {
                if (darkMageBoss.isDead()) {
                    enemiesToRemove.add(enemy);
                    int x = 0;
                    while(x <= GamePanel.getWaveNumber() * 50) {
                        x++;
                        GameLogic.killCountPlus();
                    }
                }
            }
        }

        enemies.removeAll(enemiesToRemove);
    }

    private void checkPlayerEnemyCollisions() {
        Rectangle playerCollider = player.getCollider();

        for (Enemy enemy : enemies) {
            if (!(enemy instanceof DarkMageBoss darkMageBoss) || !darkMageBoss.isDying) {
                enemy.moveTowards(player.getX(), player.getY(), wallManager);
            }

            Rectangle enemyCollider = enemy.getCollider();
            if (playerCollider.intersects(enemyCollider)) {
                int pushX = 0;
                int pushY = 0;

                if (player.getX() < enemy.getX()) {
                    pushX = -2;
                } else {
                    pushX = 2;
                }

                if (player.getY() < enemy.getY()) {
                    pushY = -2;
                } else {
                    pushY = 2;
                }

                int newPlayerX = player.getX() + pushX;
                int newPlayerY = player.getY() + pushY;

                boolean hitsWallX = wallManager.isWall(
                        newPlayerX + Player.WIDTH/2,
                        player.getY() + Player.HEIGHT/2
                );
                boolean hitsWallY = wallManager.isWall(
                        player.getX() + Player.WIDTH/2,
                        newPlayerY + Player.HEIGHT/2
                );

                if (!hitsWallX) {
                    player.setX(newPlayerX);
                }
                if (!hitsWallY) {
                    player.setY(newPlayerY);
                }

                if (enemy.getType() != Enemy.Type.GIANT &&
                        enemy.getType() != Enemy.Type.DARK_MAGE_BOSS) {
                    enemy.setX(enemy.getX() - pushX);
                    enemy.setY(enemy.getY() - pushY);
                }

                if (enemy.canAttack()) {
                    if (!gamePanel.isTutorialMode()) {
                        player.hit(enemy.getDamage());
                        if (player.getHp() <= 0) {
                            gameOver = true;
                        }
                    }
                }
            }
        }
    }

    private void checkExplosionCollisions() {
        List<Enemy> enemiesToRemove = new ArrayList<>();
        List<Explosion> explosionsToRemove = new ArrayList<>();

        for (Explosion explosion : player.getExplosions()) {
            boolean explosionDamaged = false;

            for (Enemy enemy : enemies) {
                if (explosion.isInRange(enemy.getX(), enemy.getY())) {
                    if (!explosion.hasDamaged()) {
                        enemy.hit(100, damageManager);
                        explosionDamaged = true;
                    }

                    if (!(enemy instanceof DarkMageBoss) && !(enemy instanceof BunnyBoss)) {
                        enemiesToRemove.add(enemy);
                        GameLogic.killCountPlus();
                        player.earnCoins(10);

                        if (gamePanel.isTutorialMode()) {
                            gamePanel.onTutorialEnemyKilled();
                        }
                    }
                }
            }

            if (explosionDamaged) {
                explosion.setDamaged(true);
            }

            if (explosion.isComplete()) {
                explosionsToRemove.add(explosion);
            }
        }

        enemies.removeAll(enemiesToRemove);
        player.getExplosions().removeAll(explosionsToRemove);
    }

    private void checkPlayerProjectileCollisions() {
        List<Enemy> enemiesToRemove = new ArrayList<>();
        List<PlayerProjectile> arrowsToRemove = new ArrayList<>();

        for (PlayerProjectile playerProjectile : playerProjectiles) {
            int projCenterX = playerProjectile.getX() + PlayerProjectile.SIZE / 2;
            int projCenterY = playerProjectile.getY() + PlayerProjectile.SIZE / 2;

            if (wallManager.isWall(projCenterX, projCenterY)) {
                if (playerProjectile.canRicochet() && playerProjectile.tryRicochet(wallManager)) {
                    continue; // bounced, don't remove
                }
                arrowsToRemove.add(playerProjectile);
                continue;
            }

            Rectangle arrowCollider = new Rectangle(
                    playerProjectile.getX(),
                    playerProjectile.getY(),
                    PlayerProjectile.SIZE,
                    PlayerProjectile.SIZE
            );

            for (Enemy enemy : enemies) {
                Rectangle enemyCollider = enemy.getCollider();

                if (arrowCollider.intersects(enemyCollider)) {
                    // Damage with crit + small normal variance
                    int baseDmg = player.getDamage();
                    boolean isCrit = playerProjectile.isCrit();
                    int finalDmg;
                    if (isCrit) {
                        // 2x–4x, random in that range
                        double mult = 2.0 + DAMAGE_RNG.nextDouble() * 2.0;
                        finalDmg = (int)(baseDmg * mult);
                    } else {
                        // ±15% variance on every hit
                        double variance = 0.85 + DAMAGE_RNG.nextDouble() * 0.30;
                        finalDmg = (int)(baseDmg * variance);
                    }
                    finalDmg = Math.max(1, finalDmg);
                    enemy.hitCrit(finalDmg, damageManager, isCrit);

                    if (playerProjectile.getFireDamageLevel() > 0) {
                        enemy.setFire(
                                playerProjectile.getFireDamageLevel() * 5,
                                3000,
                                damageManager
                        );
                    }

                    if (playerProjectile.hasSlowEffect()) {
                        enemy.applySlow(1000 + (player.getSlowLevel() * 1000));
                    }

                    if (enemy.getHp() <= 0 &&
                            !(enemy instanceof DarkMageBoss) &&
                            !(enemy instanceof BunnyBoss)) {
                        if (!enemiesToRemove.contains(enemy)) {
                            enemiesToRemove.add(enemy);
                            GameLogic.killCountPlus();
                            player.earnCoins(10);

                            if (gamePanel.isTutorialMode()) {
                                gamePanel.onTutorialEnemyKilled();
                            }
                        }
                    }

                    playerProjectile.setPierceCount(playerProjectile.getPierceCount() - 1);

                    if (playerProjectile.getPierceCount() <= 0) {
                        arrowsToRemove.add(playerProjectile);
                        break;
                    }
                }
            }

            if (!arrowsToRemove.contains(playerProjectile) && playerProjectile.move()) {
                arrowsToRemove.add(playerProjectile);
            }
        }

        playerProjectiles.removeAll(arrowsToRemove);
        enemies.removeAll(enemiesToRemove);
    }

    private void resolveEnemyCollisions() {
        // Spatial grid separace – O(N) místo O(N²)
        // Vampire Survivors styl: push overlap resolution bez rušení pathfindingu
        int gridCell = Game.scale(80); // zhruba velikost nepřítele
        java.util.HashMap<Long, java.util.List<Enemy>> grid = new java.util.HashMap<>();

        // Vloži každého do grid buněk
        for (Enemy e : enemies) {
            if (e.getType() == Enemy.Type.SHOOTING ||
                    e.getType() == Enemy.Type.DARK_MAGE_BOSS ||
                    e.getType() == Enemy.Type.BUNNY_BOSS) continue;

            int gx = e.getX() / gridCell;
            int gy = e.getY() / gridCell;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    long key = ((long)(gx + dx + 5000)) << 20 | (gy + dy + 5000);
                    grid.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(e);
                }
            }
        }

        // Zkontroluj a resolvi kolize jen v blízkých buňkách
        java.util.Set<Enemy> processed = new java.util.HashSet<>();
        for (Enemy e1 : enemies) {
            if (e1.getType() == Enemy.Type.SHOOTING ||
                    e1.getType() == Enemy.Type.DARK_MAGE_BOSS ||
                    e1.getType() == Enemy.Type.BUNNY_BOSS) continue;

            int gx = e1.getX() / gridCell;
            int gy = e1.getY() / gridCell;
            long key = ((long)(gx + 5000)) << 20 | (gy + 5000);
            java.util.List<Enemy> neighbors = grid.get(key);
            if (neighbors == null) continue;

            for (Enemy e2 : neighbors) {
                if (e1 == e2 || processed.contains(e2)) continue;

                int dx = e1.getX() - e2.getX();
                int dy = e1.getY() - e2.getY();
                int minDist = (e1.getWidth() + e2.getWidth()) / 2;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                if (dist < minDist && dist > 0.1f) {
                    // Tiny push – jen minimální aby se nepřekrývali, neruší pathfinding
                    float push = (minDist - dist) * 0.3f;
                    int px = (int)(dx / dist * push);
                    int py = (int)(dy / dist * push);
                    e1.nudge(px, py);
                    e2.nudge(-px, -py);
                }
            }
            processed.add(e1);
        }
    }

    public boolean isGameOver() {
        return gameOver;
    }
}