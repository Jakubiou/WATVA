// PlayerMovement.java
package WATVA;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;

public class PlayerMovement {
    private Player player;
    private boolean up, down, left, right, idle;
    private boolean dashing = false;
    private int dashDirectionX = 0, dashDirectionY = 0;
    private int dashProgress = 0;

    public PlayerMovement(Player player) {
        this.player = player;
    }

    public void move() {
        boolean moving = false;
        int edgeLimit = 54;
        long currentTime = System.currentTimeMillis();

        if (player.getShieldLevel() > 0 && currentTime - player.getLastShieldRegenerationTime() >= Player.SHIELD_REGENERATION_INTERVAL) {
            player.setShieldHP(Math.min(player.getShieldHP() + 1, Player.MAX_SHIELD_HP));
            player.setLastShieldRegenerationTime(currentTime);
        }

        if (player.getRegenerationLevel() > 0 && player.getHp() < 100) {
            player.setHp(Math.min(player.getHp() + player.getRegenerationLevel(), 100));
        }

        if (dashing) {
            int newX = player.getX() + dashDirectionX * player.getDashSpeed();
            int newY = player.getY() + dashDirectionY * player.getDashSpeed();

            if (newX >= edgeLimit && newX + Player.WIDTH <= GamePanel.mapWidth * GamePanel.BLOCK_SIZE - edgeLimit &&
                    newY >= edgeLimit && newY + Player.HEIGHT <= GamePanel.mapHeight * GamePanel.BLOCK_SIZE - edgeLimit) {
                player.setX(newX);
                player.setY(newY);
            }

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
            if (newY >= edgeLimit) {
                player.setY(newY);
                moving = true;
            }
        }

        if (down) {
            int newY = player.getY() + player.getSpeed();
            if (newY + Player.HEIGHT <= GamePanel.mapHeight * GamePanel.BLOCK_SIZE - edgeLimit) {
                player.setY(newY);
                moving = true;
            }
        }

        if (left) {
            int newX = player.getX() - player.getSpeed();
            if (newX >= edgeLimit) {
                player.setX(newX);
                moving = true;
            }
        }

        if (right) {
            int newX = player.getX() + player.getSpeed();
            if (newX + Player.WIDTH <= GamePanel.mapWidth * GamePanel.BLOCK_SIZE - edgeLimit) {
                player.setX(newX);
                moving = true;
            }
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

    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP) { up = false; }
        if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN) { down = false; }
        if (key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT) { left = false; }
        if (key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT) { right = false; }
    }

    private boolean canDash() {
        return System.currentTimeMillis() - player.getLastDashTime() >= player.getDashCooldown();
    }

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

    public boolean isUp() { return up; }
    public boolean isDown() { return down; }
    public boolean isLeft() { return left; }
    public boolean isRight() { return right; }
    public boolean isIdle() { return idle; }
}