package Logic.Level;

import java.io.*;

public class LevelManager {
    private static final String PROGRESS_FILE = "level_progress.dat";
    private LevelData[] levels;
    private int unlockedLevel = 1;
    private int currentLevel = 1;

    public LevelManager() {
        initializeLevels();
        loadProgress();
    }

    private void initializeLevels() {
        levels = new LevelData[10];

        levels[0] = new LevelData(1, "Map1.txt", 5, 150, 5);
        levels[0].setWave(0, 1, 0, 0, 0, 0, LevelData.BossType.BUNNY_BOSS);
        levels[0].setWave(1, 2, 0, 1, 0, 0);
        levels[0].setWave(2, 3, 1, 1, 0, 0);
        levels[0].setWave(3, 4, 1, 2, 1, 0);
        levels[0].setWave(4, 5, 2, 2, 1, 1);
        levels[0].setWave(5, 5, 2, 3, 2, 1);
        levels[0].setWave(6, 6, 3, 3, 2, 2);
        levels[0].setWave(7, 7, 3, 4, 3, 2);
        levels[0].setWave(8, 8, 4, 4, 3, 3);
        levels[0].setWave(9, 0, 0, 0, 0, 0, LevelData.BossType.DARK_MAGE_BOSS);

        levels[1] = new LevelData(2, "Map1.txt", 10, 30, 10);
        levels[1].setWave(0, 3, 1, 2, 1, 1);
        levels[1].setWave(1, 4, 2, 3, 1, 2);
        levels[1].setWave(2, 5, 2, 4, 2, 2);
        levels[1].setWave(3, 6, 3, 4, 2, 3);
        levels[1].setWave(4, 7, 3, 5, 3, 3, LevelData.BossType.BUNNY_BOSS);
        levels[1].setWave(5, 8, 4, 5, 3, 4);
        levels[1].setWave(6, 9, 4, 6, 4, 4);
        levels[1].setWave(7, 10, 5, 6, 4, 5);
        levels[1].setWave(8, 11, 5, 7, 5, 5);
        levels[1].setWave(9, 0, 0, 0, 0, 0, LevelData.BossType.DARK_MAGE_BOSS);

        levels[2] = new LevelData(3, "Map1.txt", 15, 40, 15);
        levels[2].setWave(0, 5, 2, 3, 2, 2);
        levels[2].setWave(1, 6, 3, 4, 3, 3);
        levels[2].setWave(2, 7, 3, 5, 3, 4);
        levels[2].setWave(3, 8, 4, 5, 4, 4);
        levels[2].setWave(4, 9, 4, 6, 4, 5, LevelData.BossType.BUNNY_BOSS);
        levels[2].setWave(5, 10, 5, 7, 5, 5);
        levels[2].setWave(6, 11, 5, 8, 5, 6);
        levels[2].setWave(7, 12, 6, 8, 6, 6);
        levels[2].setWave(8, 13, 6, 9, 6, 7);
        levels[2].setWave(9, 0, 0, 0, 0, 0, LevelData.BossType.DARK_MAGE_BOSS);

        levels[3] = new LevelData(4, "Map1.txt", 20, 50, 20);
        levels[3].setWave(0, 7, 3, 5, 3, 4);
        levels[3].setWave(1, 8, 4, 6, 4, 5);
        levels[3].setWave(2, 9, 4, 7, 4, 6);
        levels[3].setWave(3, 10, 5, 7, 5, 6);
        levels[3].setWave(4, 11, 5, 8, 5, 7, LevelData.BossType.BUNNY_BOSS);
        levels[3].setWave(5, 12, 6, 9, 6, 7);
        levels[3].setWave(6, 13, 6, 10, 6, 8);
        levels[3].setWave(7, 14, 7, 10, 7, 8);
        levels[3].setWave(8, 15, 7, 11, 7, 9);
        levels[3].setWave(9, 0, 0, 0, 0, 0, LevelData.BossType.DARK_MAGE_BOSS);

        levels[4] = new LevelData(5, "Map1.txt", 30, 70, 25);
        levels[4].setWave(0, 10, 5, 7, 5, 6);
        levels[4].setWave(1, 11, 6, 8, 6, 7);
        levels[4].setWave(2, 12, 6, 9, 6, 8);
        levels[4].setWave(3, 13, 7, 10, 7, 8);
        levels[4].setWave(4, 14, 7, 11, 7, 9, LevelData.BossType.BUNNY_BOSS);
        levels[4].setWave(5, 15, 8, 12, 8, 9);
        levels[4].setWave(6, 16, 8, 13, 8, 10);
        levels[4].setWave(7, 17, 9, 14, 9, 10);
        levels[4].setWave(8, 18, 9, 15, 9, 11);
        levels[4].setWave(9, 0, 0, 0, 0, 0, LevelData.BossType.DARK_MAGE_BOSS);

        levels[5] = new LevelData(6, "Map1.txt", 40, 90, 30);
        levels[5].setWave(0, 12, 6, 10, 7, 8);
        levels[5].setWave(1, 13, 7, 11, 8, 9);
        levels[5].setWave(2, 14, 7, 12, 8, 10);
        levels[5].setWave(3, 15, 8, 13, 9, 10);
        levels[5].setWave(4, 16, 8, 14, 9, 11, LevelData.BossType.BUNNY_BOSS);
        levels[5].setWave(5, 17, 9, 15, 10, 11);
        levels[5].setWave(6, 18, 9, 16, 10, 12);
        levels[5].setWave(7, 19, 10, 17, 11, 12);
        levels[5].setWave(8, 20, 10, 18, 11, 13);
        levels[5].setWave(9, 0, 0, 0, 0, 0, LevelData.BossType.DARK_MAGE_BOSS);

        levels[6] = new LevelData(7, "Map1.txt", 50, 110, 35);
        levels[6].setWave(0, 15, 8, 12, 9, 10);
        levels[6].setWave(1, 16, 9, 13, 10, 11);
        levels[6].setWave(2, 17, 9, 14, 10, 12);
        levels[6].setWave(3, 18, 10, 15, 11, 12);
        levels[6].setWave(4, 19, 10, 16, 11, 13, LevelData.BossType.BUNNY_BOSS);
        levels[6].setWave(5, 20, 11, 17, 12, 13);
        levels[6].setWave(6, 21, 11, 18, 12, 14);
        levels[6].setWave(7, 22, 12, 19, 13, 14);
        levels[6].setWave(8, 23, 12, 20, 13, 15);
        levels[6].setWave(9, 0, 0, 0, 0, 0, LevelData.BossType.DARK_MAGE_BOSS);

        levels[7] = new LevelData(8, "Map1.txt", 65, 130, 40);
        levels[7].setWave(0, 18, 10, 15, 11, 12);
        levels[7].setWave(1, 19, 11, 16, 12, 13);
        levels[7].setWave(2, 20, 11, 17, 12, 14);
        levels[7].setWave(3, 21, 12, 18, 13, 14);
        levels[7].setWave(4, 22, 12, 19, 13, 15, LevelData.BossType.BUNNY_BOSS);
        levels[7].setWave(5, 23, 13, 20, 14, 15);
        levels[7].setWave(6, 24, 13, 21, 14, 16);
        levels[7].setWave(7, 25, 14, 22, 15, 16);
        levels[7].setWave(8, 26, 14, 23, 15, 17);
        levels[7].setWave(9, 0, 0, 0, 0, 0, LevelData.BossType.DARK_MAGE_BOSS);

        levels[8] = new LevelData(9, "Map1.txt", 80, 150, 45);
        levels[8].setWave(0, 22, 12, 18, 14, 15);
        levels[8].setWave(1, 23, 13, 19, 15, 16);
        levels[8].setWave(2, 24, 13, 20, 15, 17);
        levels[8].setWave(3, 25, 14, 21, 16, 17);
        levels[8].setWave(4, 26, 14, 22, 16, 18, LevelData.BossType.BUNNY_BOSS);
        levels[8].setWave(5, 27, 15, 23, 17, 18);
        levels[8].setWave(6, 28, 15, 24, 17, 19);
        levels[8].setWave(7, 29, 16, 25, 18, 19);
        levels[8].setWave(8, 30, 16, 26, 18, 20);
        levels[8].setWave(9, 0, 0, 0, 0, 0, LevelData.BossType.DARK_MAGE_BOSS);

        levels[9] = new LevelData(10, "Map1.txt", 100, 200, 50);
        levels[9].setWave(0, 25, 15, 20, 16, 18);
        levels[9].setWave(1, 26, 16, 21, 17, 19);
        levels[9].setWave(2, 27, 16, 22, 17, 20);
        levels[9].setWave(3, 28, 17, 23, 18, 20);
        levels[9].setWave(4, 29, 17, 24, 18, 21, LevelData.BossType.BUNNY_BOSS);
        levels[9].setWave(5, 30, 18, 25, 19, 21);
        levels[9].setWave(6, 31, 18, 26, 19, 22);
        levels[9].setWave(7, 32, 19, 27, 20, 22);
        levels[9].setWave(8, 33, 19, 28, 20, 23);
        levels[9].setWave(9, 0, 0, 0, 0, 0, LevelData.BossType.DARK_MAGE_BOSS);
    }

    public void loadProgress() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(PROGRESS_FILE))) {
            unlockedLevel = ois.readInt();
            System.out.println("Progress loaded: unlocked level " + unlockedLevel);
        } catch (Exception e) {
            System.out.println("No progress found, starting from level 1");
            unlockedLevel = 1;
        }
    }

    public void saveProgress() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(PROGRESS_FILE))) {
            oos.writeInt(unlockedLevel);
            System.out.println("Progress saved: unlocked level " + unlockedLevel);
        } catch (Exception e) {
            System.err.println("Error saving progress: " + e.getMessage());
        }
    }

    public void unlockNextLevel() {
        if (unlockedLevel < 10) {
            unlockedLevel++;
            saveProgress();
            System.out.println("Level " + unlockedLevel + " unlocked!");
        }
    }

    public boolean isLevelUnlocked(int levelNumber) {
        return levelNumber <= unlockedLevel;
    }

    public LevelData getLevel(int levelNumber) {
        if (levelNumber >= 1 && levelNumber <= 10) {
            return levels[levelNumber - 1];
        }
        return levels[0];
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(int level) {
        if (isLevelUnlocked(level)) {
            this.currentLevel = level;
        }
    }

    public int getUnlockedLevel() {
        return unlockedLevel;
    }

    public int getTotalLevels() {
        return levels.length;
    }
}