package WATVA;

import java.awt.event.KeyEvent;
import java.util.Iterator;

/**
 * Handles all player movement logic including:
 * - Basic directional movement (WASD/arrow keys)
 * - Dash ability with cooldown
 * - Explosion ability management
 * - Player idle state detection
 * - Movement boundary enforcement
 */
public class PlayerMovement {
    private Player player;
    private boolean up, down, left, right, idle;
    private boolean dashing = false;
    private int dashDirectionX = 0, dashDirectionY = 0;
    private int dashProgress = 0;

    /**
     * Creates a new PlayerMovement instance tied to a specific player.
     *
     * @param player The Player instance this movement controller will manage
     */
    public PlayerMovement(Player player) {
        this.player = player;
    }

    /**
     * Main movement update method called every game tick.
     * Handles:
     * - Shield regeneration
     * - Health regeneration
     * - Dash movement
     * - Normal movement
     * - Idle state detection
     * - Animation frame updates
     * - Explosion updates
     */
    public void move() {
        boolean moving = false;
        long currentTime = System.currentTimeMillis();

        if (player.getShieldLevel() > 0 && currentTime - player.getLastShieldRegenerationTime() >= Player.SHIELD_REGENERATION_INTERVAL) {
            player.setShieldHP(Math.min(player.getShieldHP() + 1, Player.MAX_SHIELD_HP));
            player.setLastShieldRegenerationTime(currentTime);
        }

        if (player.getRegenerationLevel() > 0 && player.getHp() < 100 &&
                currentTime - player.getLastHpRegenerationTime() >= Player.HP_REGENERATION_INTERVAL) {
            player.setHp(Math.min(player.getHp() + player.getRegenerationLevel(), 100));
            player.setLastHpRegenerationTime(currentTime);
        }

        if (dashing) {

            dashProgress += player.getDashSpeed();

            if (dashProgress >= player.getDashDistance()) {
                dashing = false;
            }
            player.setLastMovementTime(currentTime);
            idle = false;
            return;
        }

        if (up) {
            int newY = player.getY() - player.getSpeed();
                player.setY(newY);
                moving = true;
        }

        if (down) {
            int newY = player.getY() + player.getSpeed();
                player.setY(newY);
                moving = true;
        }

        if (left) {
            int newX = player.getX() - player.getSpeed();
                player.setX(newX);
                moving = true;
        }

        if (right) {
            int newX = player.getX() + player.getSpeed();
                player.setX(newX);
                moving = true;
        }

        if (moving) {
            player.setLastMovementTime(currentTime);
            idle = false;
            if (currentTime - player.getLastFrameChange() >= player.getFrameDuration()) {
                player.setCurrentFrame((player.getCurrentFrame() + 1) % 4);
                player.setLastFrameChange(currentTime);
            }
        } else {
            if (!idle && currentTime - player.getLastMovementTime() > Player.IDLE_TRIGGER_DELAY) {
                idle = true;
                player.setIdleAnimationStartTime(currentTime);
                player.setCurrentFrame(0);
            }

            if (idle && currentTime - player.getLastFrameChange() >= player.getFrameDuration()) {
                player.setCurrentFrame((player.getCurrentFrame() + 1) % player.getGraphics().getIdleTextures().length);
                player.setLastFrameChange(currentTime);
            }
        }

        updateExplosions();
    }

    /**
     * Updates all active explosion effects.
     * Removes completed explosions from the player's explosion list.
     */
    private void updateExplosions() {
        Iterator<Explosion> iterator = player.getExplosions().iterator();
        while (iterator.hasNext()) {
            Explosion explosion = iterator.next();
            explosion.update();
            if (explosion.isComplete()) {
                iterator.remove();
            }
        }
    }

    /**
     * Handles key press events for player movement and abilities.
     *
     * @param e The KeyEvent containing key press information
     */
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP) { up = true; }
        if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN) { down = true; }
        if (key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT) { left = true; }
        if (key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT) { right = true; }
        if (key == KeyEvent.VK_SHIFT && canDash()) {
            startDash();
        }
        if (key == KeyEvent.VK_Q && player.canUseExplosion()) {
            player.triggerExplosion();
        }
    }

    /**
     * Handles key release events for player movement.
     *
     * @param e The KeyEvent containing key release information
     */
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP) { up = false; }
        if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN) { down = false; }
        if (key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT) { left = false; }
        if (key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT) { right = false; }
    }

    /**
     * Checks if the dash ability is off cooldown and available for use.
     *
     * @return true if dash can be used, false otherwise
     */
    private boolean canDash() {
        return System.currentTimeMillis() - player.getLastDashTime() >= player.getDashCooldown();
    }

    /**
     * Initiates a dash in the current movement direction.
     * Calculates dash vector based on active movement keys.
     */
    private void startDash() {
        dashing = true;
        dashProgress = 0;
        player.setLastDashTime(System.currentTimeMillis());

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

    public boolean isUp() { return up && !down; }
    public boolean isDown() { return down && !up; }
    public boolean isLeft() { return left && !right; }
    public boolean isRight() { return right && !left; }
    public boolean isIdle() { return idle; }
}