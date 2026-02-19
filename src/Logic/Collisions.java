package Logic;

import Bosses.BunnyBoss;
import Bosses.DarkMageBoss;
import Enemies.Enemy;
import Enemies.EnemyProjectile;
import Logic.DamageNumber.DamageNumberManager;
import Player.Player;
import Player.PlayerProjectile;
import Player.Explosion;
import UI.GamePanel;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Collisions {
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
            if (enemy instanceof DarkMageBoss) {
                ((DarkMageBoss)enemy).checkProjectileCollisions(player);
            }
        }
    }

    private void checkEnemyProjectileCollisions() {
        List<EnemyProjectile> globalProjectiles = Enemy.getAllProjectiles();
        Iterator<EnemyProjectile> projectileIterator = globalProjectiles.iterator();

        while (projectileIterator.hasNext()) {
            EnemyProjectile projectile = projectileIterator.next();

            if (projectile.isActive() && projectile.checkCollisionWithPlayer(player)) {
                player.hit(20);
                projectileIterator.remove();

                if (player.getHp() <= 0) {
                    gameOver = true;
                }
                break;
            }
        }
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
                    enemy.hit(player.getDamage(), damageManager);

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
        for (int i = 0; i < enemies.size(); i++) {
            for (int j = i + 1; j < enemies.size(); j++) {
                Enemy e1 = enemies.get(i);
                Enemy e2 = enemies.get(j);

                if (e1.getType() == Enemy.Type.SHOOTING || e2.getType() == Enemy.Type.SHOOTING) {
                    continue;
                }

                Rectangle r1 = e1.getCollider();
                Rectangle r2 = e2.getCollider();

                if (r1.intersects(r2)) {
                    e1.moveAwayFrom(e2.getX(), e2.getY());
                    e2.moveAwayFrom(e1.getX(), e1.getY());
                }
            }
        }
    }

    public boolean isGameOver() {
        return gameOver;
    }
}