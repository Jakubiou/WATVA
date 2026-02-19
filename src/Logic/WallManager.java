package Logic;

import Core.Game;
import Player.Player;
import UI.GamePanel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class WallManager {
    private List<WallPattern> temporaryWalls;
    private Map<String, List<Rectangle>> permanentChunkWalls;
    private Random random;
    private long lastSpawnTime;
    private static final long SPAWN_INTERVAL = 3000;
    private static final int MIN_DISTANCE_FROM_PLAYER = Game.scale(250);
    private static final int WALL_BLOCK_SIZE = GamePanel.BLOCK_SIZE;

    private boolean bossArenaActive = false;
    private List<Rectangle> arenaWalls = new ArrayList<>();
    private List<Rectangle> arenaPillars = new ArrayList<>();
    private Point arenaCenter;
    private int arenaRadius;

    private Image[] temporaryWallImages;
    private Image[] permanentWallImages;
    private Image[] warningWallImages;

    private static final int[][][] CHUNK_PATTERNS = {
            {{1, 1, 1}, {1, 0, 0}, {1, 0, 0}},
            {{1, 1, 1}, {0, 1, 0}},
            {{1, 1, 1, 1}},
            {{1, 1}, {1, 1}}
    };

    private static final int[][][] TEMP_PATTERNS = {
            {{1, 1, 1}},
            {{1, 1, 1, 1}},
            {{1, 1}, {1, 0}},
            {{1}},
            {{1, 1}}
    };

    public WallManager() {
        this.temporaryWalls = new ArrayList<>();
        this.permanentChunkWalls = new HashMap<>();
        this.random = new Random();
        this.lastSpawnTime = System.currentTimeMillis();
        loadWallImages();
    }

    private void loadWallImages() {
        temporaryWallImages = new Image[6];
        permanentWallImages = new Image[6];
        warningWallImages = new Image[6];

        try {
            for (int i = 0; i < 6; i++) {
                temporaryWallImages[i] = ImageIO.read(
                        getClass().getResourceAsStream("/WATVA/Background/Wall" + (i + 1) + ".png")
                ).getScaledInstance(WALL_BLOCK_SIZE, WALL_BLOCK_SIZE, Image.SCALE_SMOOTH);

                permanentWallImages[i] = ImageIO.read(
                        getClass().getResourceAsStream("/WATVA/Background/Wall6.png")
                ).getScaledInstance(WALL_BLOCK_SIZE, WALL_BLOCK_SIZE, Image.SCALE_SMOOTH);

                warningWallImages[i] = ImageIO.read(
                        getClass().getResourceAsStream("/WATVA/Background/Wall" + (i + 7) + ".png")
                ).getScaledInstance(WALL_BLOCK_SIZE, WALL_BLOCK_SIZE, Image.SCALE_SMOOTH);
            }
        } catch (IOException | NullPointerException e) {
            System.err.println("Error loading wall images!");
            e.printStackTrace();
        }
    }

    private void generateChunkWalls(int chunkX, int chunkY) {
        String chunkKey = chunkX + "," + chunkY;
        if (permanentChunkWalls.containsKey(chunkKey)) return;

        List<Rectangle> chunkWalls = new ArrayList<>();
        Random chunkRand = new Random(chunkKey.hashCode());
        int structureCount = 2 + chunkRand.nextInt(3);
        int chunkWorldX = chunkX * GamePanel.BLOCK_SIZE * 20;
        int chunkWorldY = chunkY * GamePanel.BLOCK_SIZE * 20;

        for (int i = 0; i < structureCount; i++) {
            int[][] pattern = CHUNK_PATTERNS[chunkRand.nextInt(CHUNK_PATTERNS.length)];
            int offsetX = chunkRand.nextInt(15) * WALL_BLOCK_SIZE;
            int offsetY = chunkRand.nextInt(15) * WALL_BLOCK_SIZE;
            int wallX = chunkWorldX + offsetX;
            int wallY = chunkWorldY + offsetY;

            for (int row = 0; row < pattern.length; row++) {
                for (int col = 0; col < pattern[row].length; col++) {
                    if (pattern[row][col] == 1) {
                        int blockX = wallX + col * WALL_BLOCK_SIZE;
                        int blockY = wallY + row * WALL_BLOCK_SIZE;
                        chunkWalls.add(new Rectangle(blockX, blockY, WALL_BLOCK_SIZE, WALL_BLOCK_SIZE));
                    }
                }
            }
        }
        permanentChunkWalls.put(chunkKey, chunkWalls);
    }

    private void updateChunkWalls(Player player) {
        if (bossArenaActive) return;

        int chunkSize = GamePanel.BLOCK_SIZE * 20;
        int playerChunkX = player.getX() / chunkSize;
        int playerChunkY = player.getY() / chunkSize;

        for (int cy = playerChunkY - 1; cy <= playerChunkY + 1; cy++) {
            for (int cx = playerChunkX - 1; cx <= playerChunkX + 1; cx++) {
                generateChunkWalls(cx, cy);
            }
        }
    }

    public void update(Player player, boolean isBossWave) {
        if (!bossArenaActive) {
            updateChunkWalls(player);
        }

        if (isBossWave) return;

        long currentTime = System.currentTimeMillis();
        temporaryWalls.removeIf(wall -> currentTime - wall.spawnTime >= wall.lifetime);

        if (currentTime - lastSpawnTime >= SPAWN_INTERVAL && temporaryWalls.size() < 15) {
            spawnNewTemporaryWall(player);
            lastSpawnTime = currentTime;
        }
    }

    private void spawnNewTemporaryWall(Player player) {
        int maxAttempts = 10;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int[][] pattern = TEMP_PATTERNS[random.nextInt(TEMP_PATTERNS.length)];
            int angle = random.nextInt(360);
            double angleRad = Math.toRadians(angle);
            int distance = MIN_DISTANCE_FROM_PLAYER + random.nextInt(Game.scale(500));
            int spawnX = player.getX() + (int)(Math.cos(angleRad) * distance);
            int spawnY = player.getY() + (int)(Math.sin(angleRad) * distance);
            spawnX = (spawnX / WALL_BLOCK_SIZE) * WALL_BLOCK_SIZE;
            spawnY = (spawnY / WALL_BLOCK_SIZE) * WALL_BLOCK_SIZE;

            if (isValidSpawnPosition(spawnX, spawnY, pattern, player)) {
                WallPattern newWall = new WallPattern(spawnX, spawnY, pattern, this);
                temporaryWalls.add(newWall);
                break;
            }
        }
    }

    private boolean isValidSpawnPosition(int x, int y, int[][] pattern, Player player) {
        int dx = x - player.getX();
        int dy = y - player.getY();
        if (Math.sqrt(dx * dx + dy * dy) < MIN_DISTANCE_FROM_PLAYER) return false;

        for (WallPattern wall : temporaryWalls) {
            if (wallsOverlap(x, y, pattern, wall)) return false;
        }

        for (int row = 0; row < pattern.length; row++) {
            for (int col = 0; col < pattern[row].length; col++) {
                if (pattern[row][col] == 1) {
                    int blockX = x + col * WALL_BLOCK_SIZE;
                    int blockY = y + row * WALL_BLOCK_SIZE;
                    if (isPermanentWall(blockX + WALL_BLOCK_SIZE/2, blockY + WALL_BLOCK_SIZE/2)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean wallsOverlap(int x, int y, int[][] pattern, WallPattern existingWall) {
        for (int row = 0; row < pattern.length; row++) {
            for (int col = 0; col < pattern[row].length; col++) {
                if (pattern[row][col] == 1) {
                    int blockX = x + col * WALL_BLOCK_SIZE;
                    int blockY = y + row * WALL_BLOCK_SIZE;
                    if (existingWall.containsBlock(blockX, blockY)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void createBossArena(Player player) {
        arenaWalls.clear();
        arenaPillars.clear();
        bossArenaActive = true;

        int centerX = player.getX();
        int centerY = player.getY();
        arenaRadius = Game.scale(1000);
        arenaCenter = new Point(centerX, centerY);

        int segments = 160;
        for (int i = 0; i < segments; i++) {
            double angle = (i / (double)segments) * Math.PI * 2;

            for (int thickness = 0; thickness < 2; thickness++) {
                int currentRadius = arenaRadius - (thickness * WALL_BLOCK_SIZE);
                int wallX = centerX + (int)(Math.cos(angle) * currentRadius);
                int wallY = centerY + (int)(Math.sin(angle) * currentRadius);
                wallX = (wallX / WALL_BLOCK_SIZE) * WALL_BLOCK_SIZE;
                wallY = (wallY / WALL_BLOCK_SIZE) * WALL_BLOCK_SIZE;
                arenaWalls.add(new Rectangle(wallX, wallY, WALL_BLOCK_SIZE, WALL_BLOCK_SIZE));
            }
        }

        int pillarDistance = Game.scale(400);
        for (int i = 0; i < 8; i++) {
            double angle = (i / 8.0) * Math.PI * 2;
            int pillarX = centerX + (int)(Math.cos(angle) * pillarDistance);
            int pillarY = centerY + (int)(Math.sin(angle) * pillarDistance);
            pillarX = (pillarX / WALL_BLOCK_SIZE) * WALL_BLOCK_SIZE;
            pillarY = (pillarY / WALL_BLOCK_SIZE) * WALL_BLOCK_SIZE;

            for (int py = 0; py < 2; py++) {
                for (int px = 0; px < 2; px++) {
                    arenaPillars.add(new Rectangle(
                            pillarX + px * WALL_BLOCK_SIZE,
                            pillarY + py * WALL_BLOCK_SIZE,
                            WALL_BLOCK_SIZE,
                            WALL_BLOCK_SIZE
                    ));
                }
            }
        }
    }

    public Point getArenaCenter() { return arenaCenter; }
    public int getArenaRadius() { return arenaRadius; }

    public void clearBossArena() {
        bossArenaActive = false;
        arenaWalls.clear();
        arenaPillars.clear();
        arenaCenter = null;
    }

    public void despawnTemporaryWalls() {
        temporaryWalls.clear();
    }

    public boolean isWall(int worldX, int worldY) {
        if (bossArenaActive) {
            for (Rectangle wall : arenaWalls) {
                if (wall.contains(worldX, worldY)) return true;
            }
            for (Rectangle pillar : arenaPillars) {
                if (pillar.contains(worldX, worldY)) return true;
            }
            return false;
        }

        if (isPermanentWall(worldX, worldY)) return true;

        for (WallPattern wall : temporaryWalls) {
            if (wall.isSolid() && wall.containsPoint(worldX, worldY)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPermanentWall(int worldX, int worldY) {
        for (List<Rectangle> chunkWalls : permanentChunkWalls.values()) {
            for (Rectangle wall : chunkWalls) {
                if (wall.contains(worldX, worldY)) return true;
            }
        }
        return false;
    }

    public boolean hasLineOfSight(int x1, int y1, int x2, int y2) {
        int steps = (int)(Math.hypot(x2 - x1, y2 - y1) / (WALL_BLOCK_SIZE / 4));
        if (steps == 0) return true;

        double dx = (x2 - x1) / (double)steps;
        double dy = (y2 - y1) / (double)steps;

        for (int i = 0; i <= steps; i++) {
            int checkX = (int)(x1 + dx * i);
            int checkY = (int)(y1 + dy * i);

            if (isWall(checkX, checkY)) {
                return false;
            }
        }
        return true;
    }

    public Point unstuckFromWall(int entityX, int entityY, int entityWidth, int entityHeight) {
        int centerX = entityX + entityWidth / 2;
        int centerY = entityY + entityHeight / 2;

        if (!isWall(centerX, centerY)) {
            return new Point(entityX, entityY);
        }

        for (int radius = 1; radius <= 5; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    if (Math.abs(dx) != radius && Math.abs(dy) != radius) continue;

                    int newX = entityX + dx * WALL_BLOCK_SIZE;
                    int newY = entityY + dy * WALL_BLOCK_SIZE;
                    int newCenterX = newX + entityWidth / 2;
                    int newCenterY = newY + entityHeight / 2;

                    if (!isWall(newCenterX, newCenterY)) {
                        return new Point(newX, newY);
                    }
                }
            }
        }
        return new Point(entityX, entityY);
    }

    public void draw(Graphics g, int cameraX, int cameraY) {
        Graphics2D g2d = (Graphics2D) g;

        if (!bossArenaActive) {
            for (List<Rectangle> chunkWalls : permanentChunkWalls.values()) {
                for (Rectangle wall : chunkWalls) {
                    Image texture = permanentWallImages[random.nextInt(permanentWallImages.length)];
                    if (texture != null) {
                        g2d.drawImage(texture, wall.x, wall.y, null);
                    }
                }
            }
        }

        if (bossArenaActive) {
            if (permanentWallImages[5] != null) {
                for (Rectangle wall : arenaWalls) {
                    g2d.drawImage(permanentWallImages[5], wall.x, wall.y, null);
                }
            }

            if (permanentWallImages[3] != null) {
                for (Rectangle pillar : arenaPillars) {
                    g2d.drawImage(permanentWallImages[3], pillar.x, pillar.y, null);
                }
            }
        }

        for (WallPattern wall : temporaryWalls) {
            wall.draw(g2d);
        }
    }

    public void clearWalls() {
        temporaryWalls.clear();
        permanentChunkWalls.clear();
        clearBossArena();
    }

    public Image[] getTemporaryWallImages() { return temporaryWallImages; }
    public Image[] getWarningWallImages() { return warningWallImages; }

    private static class WallPattern {
        int x, y;
        int[][] pattern;
        long spawnTime;
        long lifetime;
        WallManager manager;

        private static final long WARNING_DURATION = 500;
        private static final long SOLID_DURATION = 12000;
        private static final long FADE_DURATION = 500;

        public WallPattern(int x, int y, int[][] pattern, WallManager manager) {
            this.x = x;
            this.y = y;
            this.pattern = pattern;
            this.manager = manager;
            this.spawnTime = System.currentTimeMillis();
            this.lifetime = WARNING_DURATION + SOLID_DURATION + FADE_DURATION;
        }

        public boolean isSolid() {
            long age = System.currentTimeMillis() - spawnTime;
            return age >= WARNING_DURATION && age < (WARNING_DURATION + SOLID_DURATION);
        }

        public boolean containsPoint(int worldX, int worldY) {
            for (int row = 0; row < pattern.length; row++) {
                for (int col = 0; col < pattern[row].length; col++) {
                    if (pattern[row][col] == 1) {
                        int blockX = x + col * WALL_BLOCK_SIZE;
                        int blockY = y + row * WALL_BLOCK_SIZE;

                        if (worldX >= blockX && worldX < blockX + WALL_BLOCK_SIZE &&
                                worldY >= blockY && worldY < blockY + WALL_BLOCK_SIZE) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        public boolean containsBlock(int blockX, int blockY) {
            for (int row = 0; row < pattern.length; row++) {
                for (int col = 0; col < pattern[row].length; col++) {
                    if (pattern[row][col] == 1) {
                        int wallBlockX = x + col * WALL_BLOCK_SIZE;
                        int wallBlockY = y + row * WALL_BLOCK_SIZE;
                        if (Math.abs(blockX - wallBlockX) < WALL_BLOCK_SIZE &&
                                Math.abs(blockY - wallBlockY) < WALL_BLOCK_SIZE) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        public void draw(Graphics2D g2d) {
            long age = System.currentTimeMillis() - spawnTime;
            Image texture = null;

            if (age < WARNING_DURATION) {
                int frame = (int)((age / (double)WARNING_DURATION) * 6);
                frame = Math.min(5, frame);
                texture = manager.getWarningWallImages()[frame];
            }
            else if (age < WARNING_DURATION + SOLID_DURATION) {
                texture = manager.getTemporaryWallImages()[5];
            }
            else {
                long fadeAge = age - WARNING_DURATION - SOLID_DURATION;
                int frame = 5 - (int)((fadeAge / (double)FADE_DURATION) * 6);
                frame = Math.max(0, Math.min(5, frame));
                texture = manager.getTemporaryWallImages()[frame];
            }

            if (texture == null) return;

            for (int row = 0; row < pattern.length; row++) {
                for (int col = 0; col < pattern[row].length; col++) {
                    if (pattern[row][col] == 1) {
                        int worldBlockX = x + col * WALL_BLOCK_SIZE;
                        int worldBlockY = y + row * WALL_BLOCK_SIZE;
                        g2d.drawImage(texture, worldBlockX, worldBlockY, null);
                    }
                }
            }
        }
    }
}