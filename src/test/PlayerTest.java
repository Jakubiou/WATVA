/*package test;

import Player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {
    private Player player;
    private static final String TEST_SAVE_FILE = "test_player_save.dat";

    @BeforeEach
    void setUp() {
        player = new Player(100, 100, 100);
    }

    @Test
    void playerInitialization() {
        assertEquals(100, player.getX());
        assertEquals(100, player.getY());
        assertEquals(100, player.getHp());
        assertEquals(0, player.getCoins());
        assertEquals(1, player.getDamage());
        assertEquals(0, player.getDefense());
    }

    @Test
    void upgradeSystem() {
        player.increaseDamage();
        assertEquals(2, player.getDamage());

        player.increaseHp();
        assertEquals(110, player.getHp());

        player.increaseDefense();
        assertEquals(1, player.getDefense());

        player.upgradeShield();
        assertEquals(1, player.getShieldLevel());
        assertEquals(Player.MAX_SHIELD_HP, player.getShieldHP());
    }

    @Test
    void coinSystem() {
        player.earnCoins(50);
        assertEquals(50, player.getCoins());

        player.setCoins(100);
        assertEquals(100, player.getCoins());
    }

    @Test
    void serialization() throws IOException, ClassNotFoundException {
        player.setCoins(150);
        player.increaseDamage();
        player.increaseHp();
        player.upgradeShield();
        player.saveState(TEST_SAVE_FILE);

        Player loadedPlayer = Player.loadState(TEST_SAVE_FILE);

        assertNotNull(loadedPlayer);
        assertEquals(150, loadedPlayer.getCoins());
        assertEquals(2, loadedPlayer.getDamage());
        assertEquals(110, loadedPlayer.getHp());
        assertEquals(1, loadedPlayer.getShieldLevel());

        new File(TEST_SAVE_FILE).delete();
    }

    @Test
    void regenerationSystems() {
        player.upgradeShield();
        player.setShieldHP(50);
        player.setLastShieldRegenerationTime(System.currentTimeMillis() - Player.SHIELD_REGENERATION_INTERVAL - 1);
        player.move();
        assertEquals(51, player.getShieldHP());

        player.upgradeRegeneration();
        player.setHp(80);
        player.setLastHpRegenerationTime(System.currentTimeMillis() - Player.HP_REGENERATION_INTERVAL - 1);
        player.move();
        assertEquals(81, player.getHp());
    }

    @Test
    void collisionDetection() {
        Rectangle collider = player.getCollider();
        assertEquals(100, collider.x);
        assertEquals(100, collider.y);
        assertEquals(45, collider.width);
        assertEquals(45, collider.height);
    }

    @Test
    void animationSystem() {
        assertEquals(0, player.getCurrentFrame());
        player.setCurrentFrame(2);
        assertEquals(2, player.getCurrentFrame());

        assertEquals(200, player.getFrameDuration());
    }
}*/