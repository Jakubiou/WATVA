package Pets;

import Core.Game;
import Enemies.Enemy;
import Logic.DamageNumber.DamageNumberManager;
import Logic.World.WallManager;
import Player.Player;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages the active pet during gameplay.
 * Pet is chosen BEFORE the run in PetShopPanel (via LevelMapPanel).
 * Applies stat bonuses once on activation, handles wall-aware movement and attacks.
 */
public class PetManager {
    private final PetInventory inventory;
    private final Player player;
    private final ArrayList<PetAttackProjectile> projectiles = new ArrayList<>();

    private Pet    lastActivePet      = null;
    private int    appliedHpBonus     = 0;
    private int    appliedDamageBonus = 0;

    private long lastRegenTick = 0;
    private static final long REGEN_INTERVAL = 1000;

    public PetManager(PetInventory inventory, Player player) {
        this.inventory = inventory;
        this.player    = player;
    }

    public void update(CopyOnWriteArrayList<Enemy> enemies,
                       DamageNumberManager damageManager,
                       WallManager wallManager) {
        Pet active = inventory.getSelectedPet();

        if (active != lastActivePet) {
            removeBonuses();
            lastActivePet = active;
            if (active != null) applyBonuses(active);
        }
        if (active == null) return;

        active.loadTextures(this);
        active.update(player.getX(), player.getY(), wallManager, player);

        long now = System.currentTimeMillis();
        if (now - lastRegenTick >= REGEN_INTERVAL) {
            lastRegenTick = now;
            tickRegen(active);
        }

        if (active.getType().isAttacker && !enemies.isEmpty() && active.canAttack()) {
            spawnAttackAtNearest(active, enemies);
        }

        java.util.List<Enemy> toRemove = new java.util.ArrayList<>();
        Iterator<PetAttackProjectile> it = projectiles.iterator();
        while (it.hasNext()) {
            PetAttackProjectile proj = it.next();
            proj.update();
            if (!proj.isActive()) { it.remove(); continue; }
            for (Enemy e : enemies) {
                if (proj.getCollider().intersects(e.getCollider())) {
                    e.hit(proj.getDamage(), damageManager);
                    proj.setActive(false);
                    if (e.getHp() <= 0
                            && !(e instanceof Bosses.DarkMageBoss)
                            && !(e instanceof Bosses.BunnyBoss)
                            && !toRemove.contains(e)) {
                        toRemove.add(e);
                        Logic.GameLogic.killCountPlus();
                        player.earnCoins(10);
                    }
                    break;
                }
            }
        }
        if (!toRemove.isEmpty()) enemies.removeAll(toRemove);
    }

    private void applyBonuses(Pet pet) {
        switch (pet.getType().statBonus) {
            case HP_BONUS -> {
                appliedHpBonus = pet.getCurrentStatValue();
                player.setHp(Math.min(500, player.getHp() + appliedHpBonus));
            }
            case DAMAGE_BONUS -> {
                appliedDamageBonus = pet.getCurrentStatValue();
                for (int i = 0; i < appliedDamageBonus; i++) player.increaseDamage();
            }
            case ALL_STATS -> {
                appliedHpBonus = pet.getCurrentStatValue();
                appliedDamageBonus = Math.max(1, pet.getCurrentStatValue() / 3);
                player.setHp(Math.min(500, player.getHp() + appliedHpBonus));
                for (int i = 0; i < appliedDamageBonus; i++) player.increaseDamage();
            }
            default -> {}
        }
    }

    private void removeBonuses() {
        if (appliedHpBonus > 0) {
            player.setHp(Math.max(1, player.getHp() - appliedHpBonus));
            appliedHpBonus = 0;
        }
        appliedDamageBonus = 0;
    }

    private void tickRegen(Pet pet) {
        if (pet.getType().statBonus == Pet.StatBonus.REGEN) {
            player.setHp(Math.min(500, player.getHp() + pet.getCurrentStatValue()));
        }
        if (pet.getType().statBonus == Pet.StatBonus.ALL_STATS) {
            int regen = Math.max(1, pet.getCurrentStatValue() / 5);
            player.setHp(Math.min(500, player.getHp() + regen));
        }
    }

    private void spawnAttackAtNearest(Pet pet, CopyOnWriteArrayList<Enemy> enemies) {
        Enemy nearest = null;
        double nearestDistSq = Game.scale(550.0) * Game.scale(550.0);
        double px = pet.getWorldX() + Game.scale(19);
        double py = pet.getWorldY() + Game.scale(19);

        for (Enemy e : enemies) {
            double dx = e.getX() - px, dy = e.getY() - py;
            double dSq = dx*dx + dy*dy;
            if (dSq < nearestDistSq) { nearestDistSq = dSq; nearest = e; }
        }
        if (nearest == null) return;

        projectiles.add(new PetAttackProjectile(
                px, py,
                nearest.getX() + nearest.getWidth()  / 2.0,
                nearest.getY() + nearest.getHeight() / 2.0,
                pet.getCurrentAttackDmg(),
                pet.getType()
        ));
    }

    /** Extra speed to add to player speed this frame */
    public int getSpeedBonus() {
        Pet p = inventory.getSelectedPet();
        if (p == null) return 0;
        return switch (p.getType().statBonus) {
            case SPEED     -> p.getCurrentStatValue();
            case ALL_STATS -> Math.max(1, p.getCurrentStatValue() / 4);
            default        -> 0;
        };
    }

    /** Extra defense to add when computing damage */
    public int getDefenseBonus() {
        Pet p = inventory.getSelectedPet();
        if (p == null) return 0;
        return switch (p.getType().statBonus) {
            case DEFENSE   -> p.getCurrentStatValue();
            case ALL_STATS -> Math.max(1, p.getCurrentStatValue() / 5);
            default        -> 0;
        };
    }

    public void draw(Graphics g) {
        Pet active = inventory.getSelectedPet();
        if (active != null) active.draw(g);
        for (PetAttackProjectile proj : projectiles) proj.draw(g);
    }

    public PetInventory getInventory() { return inventory; }
}