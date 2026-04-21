package Pets;

import Core.Game;
import Logic.World.WallManager;
import Player.Player;
import Player.PlayerMovement;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.Serializable;

/**
 * Represents a pet – rarity, stats, level, animation, wall-aware movement.
 * Upgrading requires BOTH coins AND duplicate pets of the same type.
 */
public class Pet implements Serializable {
    private static final long serialVersionUID = 2L;

    public enum Rarity {
        COMMON    ("Common",    new Color(180,180,180), 1.0),
        UNCOMMON  ("Uncommon",  new Color( 80,200, 80), 1.5),
        RARE      ("Rare",      new Color( 60,120,255), 2.5),
        EPIC      ("Epic",      new Color(180, 60,255), 4.0),
        LEGENDARY ("Legendary", new Color(255,180,  0), 7.0);

        public final String name;
        public final Color  color;
        public final double statMult;
        Rarity(String n, Color c, double m){ name=n; color=c; statMult=m; }
    }

    public enum PetType {
        SLIMELING  ("Slimeling",   Rarity.COMMON,    StatBonus.HP_BONUS,     8,  0, false, 1,  60,
                "Adds max HP each level.",          "/WATVA/Pets/GreenSlime1"),
        MUSHROOM   ("Mushroom",    Rarity.COMMON,    StatBonus.REGEN,        2,  0, false, 1,  60,
                "Regenerates HP every second.",     "/WATVA/Pets/Mushroom"),
        FOXLING    ("Foxling",     Rarity.UNCOMMON,  StatBonus.SPEED,        1,  0, false, 2, 120,
                "Boosts movement speed.",           "/WATVA/Pets/Foxling"),
        STONELING  ("Stoneling",   Rarity.UNCOMMON,  StatBonus.DEFENSE,      4,  0, false, 2, 120,
                "Flat damage reduction.",           "/WATVA/Pets/Stoneling"),
        EMBERWOLF  ("Emberwolf",   Rarity.RARE,      StatBonus.ATTACK_PET,   0, 10, true,  3, 280,
                "Fires fire bolts at enemies.",     "/WATVA/Pets/Emberwolf"),
        CRYSTALFOX ("Crystalfox", Rarity.RARE,      StatBonus.DAMAGE_BONUS, 3,  0, false, 3, 280,
                "Increases projectile damage.",     "/WATVA/Pets/Crystalfox"),
        SHADOWCAT  ("Shadowcat",   Rarity.EPIC,      StatBonus.ATTACK_PET,   0, 18, true,  4, 600,
                "Shadow bolts slow + damage.",      "/WATVA/Pets/Shadowcat"),
        STORMBIRD  ("Stormbird",   Rarity.EPIC,      StatBonus.ATTACK_PET,   0, 24, true,  4, 600,
                "Lightning strikes nearby foes.",   "/WATVA/Pets/Stormbird"),
        DRAGONET   ("Dragonet",    Rarity.LEGENDARY, StatBonus.ATTACK_PET,   0, 40, true,  5,1400,
                "Dragon fire, massive damage.",     "/WATVA/Pets/FireDragon1"),
        PHOENIXLING("Phoenixling", Rarity.LEGENDARY, StatBonus.ALL_STATS,    0, 28, true,  5,1400,
                "Boosts ALL stats + attacks.",      "/WATVA/Pets/FireDragon1");

        public final String displayName;
        public final Rarity rarity;
        public final StatBonus statBonus;
        public final int baseStatValue;
        public final int baseAttackDmg;
        public final boolean isAttacker;
        /** Duplicate pets needed per level of upgrade (level N → level N requires N * this) */
        public final int dupesPerUpgradeLevel;
        /** Base coin cost (scales with level) */
        public final int baseUpgradeCoinCost;
        public final String description;
        public final String texturePath;

        PetType(String dn, Rarity r, StatBonus sb, int bsv, int bad, boolean ia,
                int dpu, int bucc, String desc, String tp) {
            displayName=dn; rarity=r; statBonus=sb; baseStatValue=bsv; baseAttackDmg=bad;
            isAttacker=ia; dupesPerUpgradeLevel=dpu; baseUpgradeCoinCost=bucc;
            description=desc; texturePath=tp;
        }
    }

    public enum StatBonus { HP_BONUS, REGEN, SPEED, DEFENSE, DAMAGE_BONUS, ATTACK_PET, ALL_STATS }

    private final PetType type;
    private int level = 1;
    public static final int MAX_LEVEL = 10;

    private transient Image[] rightFrames;
    private transient Image[] leftFrames;
    private transient boolean texturesLoaded = false;

    private transient double worldX, worldY;
    private transient double velX,   velY;
    private transient boolean movingRight = true;
    private transient int animFrame = 0;
    private transient long lastAnimTime = 0;
    private static final long ANIM_MS = 150;

    private transient long lastAttackTime = 0;
    private static final long ATTACK_CD_MS = 1600;

    public Pet(PetType type) { this.type = type; }

    /** Coins needed to upgrade from current level to level+1 */
    public int getUpgradeCoinCost() {
        return (int)(type.baseUpgradeCoinCost * Math.pow(1.35, level - 1));
    }
    /** Duplicate pets of this type needed to upgrade from current level to level+1 */
    public int getDupesRequired() {
        return type.dupesPerUpgradeLevel * level;
    }
    public boolean canUpgrade() { return level < MAX_LEVEL; }
    public void upgrade() { if (canUpgrade()) level++; }

    public int getCurrentStatValue() {
        return (int)(type.baseStatValue * level * type.rarity.statMult);
    }
    public int getCurrentAttackDmg() {
        return (int)(type.baseAttackDmg * (1 + (level - 1) * 0.3) * type.rarity.statMult);
    }

    public void loadTextures(Object contextObject) {
        if (texturesLoaded) return;
        rightFrames = new Image[4];
        leftFrames  = new Image[4];
        Class<?> cls = contextObject.getClass();
        for (int i = 0; i < 4; i++) {
            try { rightFrames[i] = ImageIO.read(cls.getResourceAsStream(type.texturePath + ".png")); } catch (Exception ignored) {}
            try { leftFrames[i]  = ImageIO.read(cls.getResourceAsStream(type.texturePath  + ".png")); } catch (Exception ignored) {}
        }
        texturesLoaded = true;
    }

    public void update(double playerX, double playerY, WallManager wallManager, Player player) {
        if (worldX == 0 && worldY == 0) { worldX = playerX; worldY = playerY; }

        double targetX = playerX - Game.scale(48);
        double targetY = playerY + Game.scale(6);
        double dx = targetX - worldX;
        double dy = targetY - worldY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        double speed = player.getSpeed();
        if (dist > Game.scale(6)) {
            double nx = dx / dist, ny = dy / dist;
            double wantVx = nx * Math.min(speed, dist * 0.18);
            double wantVy = ny * Math.min(speed, dist * 0.18);

            int size = Game.scale(34);
            int nextX = (int)(worldX + wantVx);
            int nextY = (int)(worldY + wantVy);

            if (wallManager == null || !hitsWall(nextX, nextY, size, wallManager)) {
                velX = wantVx; velY = wantVy;
            } else if (wallManager == null || !hitsWall(nextX, (int)worldY, size, wallManager)) {
                velX = wantVx; velY = 0;
            } else if (wallManager == null || !hitsWall((int)worldX, nextY, size, wallManager)) {
                velX = 0; velY = wantVy;
            } else {
                velX *= 0.4; velY *= 0.4;
            }
            movingRight = dx > 0;
        } else {
            velX *= 0.55; velY *= 0.55;
        }

        worldX += velX;
        worldY += velY;

        long now = System.currentTimeMillis();
        if (now - lastAnimTime > ANIM_MS) { animFrame = (animFrame + 1) % 4; lastAnimTime = now; }
    }

    private boolean hitsWall(int x, int y, int size, WallManager wm) {
        int p = size / 4;
        return wm.isWall(x + p, y + p) || wm.isWall(x + size - p, y + p)
                || wm.isWall(x + p, y + size - p) || wm.isWall(x + size - p, y + size - p);
    }

    public boolean canAttack() {
        if (!type.isAttacker) return false;
        long now = System.currentTimeMillis();
        if (now - lastAttackTime >= ATTACK_CD_MS) { lastAttackTime = now; return true; }
        return false;
    }

    public void draw(Graphics g) {
        if (!texturesLoaded) return;
        Image[] frames = movingRight ? rightFrames : leftFrames;
        Image frame = (frames != null && animFrame < frames.length) ? frames[animFrame] : null;
        int size = Game.scale(38);
        int dx = (int)worldX, dy = (int)worldY;

        if (frame != null) {
            g.drawImage(frame, dx, dy, size, size, null);
        } else {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(type.rarity.color);
            g2.fillOval(dx, dy, size, size);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, Game.scale(14)));
            FontMetrics fm = g2.getFontMetrics();
            String ch = String.valueOf(type.displayName.charAt(0));
            g2.drawString(ch, dx + (size - fm.stringWidth(ch)) / 2, dy + (size + fm.getAscent() - fm.getDescent()) / 2);
        }
        Graphics2D g2 = (Graphics2D) g;
        int bx = (int)worldX + Game.scale(26), by = (int)worldY;
        int bs = Game.scale(14);
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillOval(bx, by, bs, bs);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, Game.scale(9)));
        g2.drawString(String.valueOf(level), bx + Game.scale(2), by + Game.scale(10));
    }

    public PetType getType()           { return type; }
    public int     getLevel()          { return level; }
    public int     getMaxLevel()       { return MAX_LEVEL; }
    public Rarity  getRarity()         { return type.rarity; }
    public String  getName()           { return type.displayName; }
    public double  getWorldX()         { return worldX; }
    public double  getWorldY()         { return worldY; }
    public boolean isTexturesLoaded()  { return texturesLoaded; }
    public void    setLevel(int l)     { this.level = Math.min(l, MAX_LEVEL); }
}