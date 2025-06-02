package WATVA;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Handles all collision detection and resolution in the game.
 */
public class Collisions {
    private Player player;
    private CopyOnWriteArrayList<Enemy> enemies;
    private CopyOnWriteArrayList<PlayerProjectile> playerProjectiles;
    private boolean gameOver;
    private static final int EDGE_LIMIT = 50;
    private static final int SCREEN_WIDTH = GamePanel.PANEL_WIDTH * 4;
    private static final int SCREEN_HEIGHT = GamePanel.PANEL_HEIGHT * 4;

    /**
     * Creates a new Collisions handler for the specified game objects.
     * @param player The player character
     * @param enemies List of enemies in the game
     * @param playerProjectiles List of player projectiles
     */
    public Collisions(Player player, CopyOnWriteArrayList<Enemy> enemies, CopyOnWriteArrayList<PlayerProjectile> playerProjectiles) {
        this.player = player;
        this.enemies = enemies;
        this.playerProjectiles = playerProjectiles;
        this.gameOver = false;
    }

    /**
     * Checks all possible collisions in the game.
     */
    public void checkCollisions() {
        checkBossProjectileCollisions();
        checkEnemyProjectileCollisions();
        checkDeadBosses();
        checkPlayerEnemyCollisions();
        checkExplosionCollisions();
        checkPlayerProjectileCollisions();
        resolveEnemyCollisions();
    }

    /**
     * Checks collisions between boss projectiles and player.
     */
    private void checkBossProjectileCollisions() {
        for (Enemy enemy : enemies) {
            if (enemy instanceof DarkMageBoss) {
                ((DarkMageBoss)enemy).checkProjectileCollisions(player);
            }
        }
    }

    /**
     * Checks collisions between enemy projectiles and player.
     */
    private void checkEnemyProjectileCollisions() {
        for (Enemy enemy : enemies) {
            if (enemy.getType() == Enemy.Type.SHOOTING) {
                Iterator<EnemyProjectile> projectileIterator = enemy.getProjectiles().iterator();
                while (projectileIterator.hasNext()) {
                    EnemyProjectile projectile = projectileIterator.next();
                    if (projectile.isActive() && projectile.checkCollisionWithPlayer(player)) {
                        player.hit(enemy.getDamage());
                        projectileIterator.remove();
                        if (player.getHp() <= 0) {
                            gameOver = true;
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * Removes dead bosses from the game.
     */
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

    /**
     * Checks collisions between player and enemies.
     */
    private void checkPlayerEnemyCollisions() {
        Rectangle playerCollider = player.getCollider();

        for (Enemy enemy : enemies) {
            if (!(enemy instanceof DarkMageBoss darkMageBoss) || !darkMageBoss.isDying) {
                enemy.moveTowards(player.getX(), player.getY());
            }

            Rectangle enemyCollider = enemy.getCollider();
            if (playerCollider.intersects(enemyCollider)) {
                int newPlayerX = player.getX();
                int newPlayerY = player.getY();
                int newEnemyX = enemy.getX();
                int newEnemyY = enemy.getY();

                if (player.getX() < enemy.getX()) {
                    newPlayerX = Math.max(EDGE_LIMIT, player.getX() - 1);
                    newEnemyX = Math.min(SCREEN_WIDTH - enemy.getWidth() - EDGE_LIMIT, enemy.getX() + 1);
                } else {
                    newPlayerX = Math.min(SCREEN_WIDTH - Player.WIDTH - EDGE_LIMIT, player.getX() + 1);
                    newEnemyX = Math.max(EDGE_LIMIT, enemy.getX() - 1);
                }

                if (player.getY() < enemy.getY()) {
                    newPlayerY = Math.max(EDGE_LIMIT, player.getY() - 1);
                    newEnemyY = Math.min(SCREEN_HEIGHT - enemy.getHeight() - EDGE_LIMIT, enemy.getY() + 1);
                } else {
                    newPlayerY = Math.min(SCREEN_HEIGHT - Player.HEIGHT - EDGE_LIMIT, player.getY() + 1);
                    newEnemyY = Math.max(EDGE_LIMIT, enemy.getY() - 1);
                }

                player.setX(newPlayerX);
                player.setY(newPlayerY);
                enemy.setX(newEnemyX);
                enemy.setY(newEnemyY);

                if (enemy.canAttack()) {
                    player.hit(enemy.getDamage());
                    if (player.getHp() <= 0) {
                        gameOver = true;
                    }
                }
            }
        }
    }

    /**
     * Checks collisions between explosions and enemies.
     */
    private void checkExplosionCollisions() {
        List<Enemy> enemiesToRemove = new ArrayList<>();
        List<Explosion> explosionsToRemove = new ArrayList<>();

        for (Explosion explosion : player.getExplosions()) {
            boolean explosionDamaged = false;

            for (Enemy enemy : enemies) {
                if (explosion.isInRange(enemy.getX(), enemy.getY())) {
                    if (!explosion.hasDamaged()) {
                        enemy.hit(100);
                        explosionDamaged = true;
                    }

                    if (!(enemy instanceof DarkMageBoss) && !(enemy instanceof BunnyBoss)) {
                        enemiesToRemove.add(enemy);
                        GameLogic.killCountPlus();
                        player.earnCoins(10);
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

    /**
     * Checks collisions between player projectiles and enemies.
     */
    private void checkPlayerProjectileCollisions() {
        List<Enemy> enemiesToRemove = new ArrayList<>();
        List<PlayerProjectile> arrowsToRemove = new ArrayList<>();

        for (PlayerProjectile playerProjectile : playerProjectiles) {
            Rectangle arrowCollider = new Rectangle(playerProjectile.getX(), playerProjectile.getY(),
                    PlayerProjectile.SIZE, PlayerProjectile.SIZE);

            for (Enemy enemy : enemies) {
                Rectangle enemyCollider = enemy.getCollider();
                if (arrowCollider.intersects(enemyCollider)) {
                    enemy.hit(player.getDamage());

                    if (playerProjectile.getFireDamageLevel() > 0) {
                        enemy.setFire(playerProjectile.getFireDamageLevel() * 5, 3000);
                    }

                    if (playerProjectile.hasSlowEffect()) {
                        enemy.applySlow(1000 + (player.getSlowLevel() * 1000));
                    }

                    if (enemy.getHp() <= 0 && !(enemy instanceof DarkMageBoss)) {
                        enemiesToRemove.add(enemy);
                        GameLogic.killCountPlus();
                        player.earnCoins(10);
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

    /**
     * Resolves collisions between enemies.
     */
    private void resolveEnemyCollisions() {
        for (int i = 0; i < enemies.size(); i++) {
            for (int j = i + 1; j < enemies.size(); j++) {
                Rectangle collider1 = enemies.get(i).getCollider();
                Rectangle collider2 = enemies.get(j).getCollider();
                if (collider1.intersects(collider2)) {
                    Enemy enemy1 = enemies.get(i);
                    Enemy enemy2 = enemies.get(j);

                    double dx = enemy1.getX() - enemy2.getX();
                    double dy = enemy1.getY() - enemy2.getY();
                    double distance = Math.sqrt(dx * dx + dy * dy);

                    if (enemy1.getType() != Enemy.Type.SHOOTING && enemy2.getType() != Enemy.Type.SHOOTING) {
                        if (distance < enemy1.getWidth()) {
                            double moveDistance = (enemy1.getWidth() - distance) / 2;
                            dx = (dx / distance) * moveDistance;
                            dy = (dy / distance) * moveDistance;

                            enemy1.moveAwayFrom(enemy2.getX() - (int) dx, enemy2.getY() - (int) dy);
                            enemy2.moveAwayFrom(enemy1.getX() + (int) dx, enemy1.getY() + (int) dy);
                        }
                    }
                }
            }
        }
    }

    public boolean isGameOver() {
        return gameOver;
    }
}
