package Logic.Level;

public class LevelData {
    private int levelNumber;
    private String mapTexture;
    private WaveData[] waves;
    private int maxDamageUpgrade;
    private int maxHpUpgrade;
    private int maxDefenseUpgrade;

    public enum BossType {
        NONE,
        BUNNY_BOSS,
        DARK_MAGE_BOSS
    }

    public LevelData(int levelNumber, String mapTexture, int maxDamage, int maxHp, int maxDefense) {
        this.levelNumber = levelNumber;
        this.mapTexture = mapTexture;
        this.maxDamageUpgrade = maxDamage;
        this.maxHpUpgrade = maxHp;
        this.maxDefenseUpgrade = maxDefense;
        this.waves = new WaveData[10];
    }

    public void setWave(int waveIndex, int normal, int giant, int small, int shooting, int slime) {
        setWave(waveIndex, normal, giant, small, shooting, slime, BossType.NONE);
    }

    public void setWave(int waveIndex, int normal, int giant, int small, int shooting, int slime, BossType bossType) {
        if (waveIndex >= 0 && waveIndex < 10) {
            waves[waveIndex] = new WaveData(normal, giant, small, shooting, slime, bossType);
        }
    }

    public int getLevelNumber() { return levelNumber; }
    public String getMapTexture() { return mapTexture; }
    public WaveData getWave(int index) { return waves[index]; }
    public int getMaxDamageUpgrade() { return maxDamageUpgrade; }
    public int getMaxHpUpgrade() { return maxHpUpgrade; }
    public int getMaxDefenseUpgrade() { return maxDefenseUpgrade; }

    public static class WaveData {
        public int normalPerSecond;
        public int giantPerSecond;
        public int smallPerSecond;
        public int shootingPerSecond;
        public int slimePerSecond;
        public BossType bossType;

        public WaveData(int normal, int giant, int small, int shooting, int slime, BossType bossType) {
            this.normalPerSecond = normal;
            this.giantPerSecond = giant;
            this.smallPerSecond = small;
            this.shootingPerSecond = shooting;
            this.slimePerSecond = slime;
            this.bossType = bossType;
        }

        public boolean hasBoss() {
            return bossType != BossType.NONE;
        }
    }
}