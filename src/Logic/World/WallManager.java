package Logic.World;

import Core.Game;
import Player.Player;
import UI.Game.GamePanel;

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

    private final java.util.HashSet<Long> wallTileSet = new java.util.HashSet<>();
    private final java.util.HashSet<Long> tempWallTileSet = new java.util.HashSet<>();
    private long lastTempWallRebuild = 0;
    private static final long TEMP_REBUILD_INTERVAL = 200;

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

    private static long tileKey(int worldX, int worldY) {
        int tx = Math.floorDiv(worldX, WALL_BLOCK_SIZE);
        int ty = Math.floorDiv(worldY, WALL_BLOCK_SIZE);
        return ((long)(tx + 100000)) << 20 | (ty + 100000);
    }

    private void addToWallSet(java.util.HashSet<Long> set, Rectangle rect) {
        int tx = rect.x / WALL_BLOCK_SIZE;
        int ty = rect.y / WALL_BLOCK_SIZE;
        set.add(((long)(tx + 100000)) << 20 | (ty + 100000));
    }

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
        warningWallImages   = new Image[6];

        try {
            for (int i = 0; i < 6; i++) {
                temporaryWallImages[i] = toBufferedImage(
                        ImageIO.read(getClass().getResourceAsStream("/WATVA/Background/Wall" + (i + 7) + ".png")));

                permanentWallImages[i] = toBufferedImage(
                        ImageIO.read(getClass().getResourceAsStream("/WATVA/Background/Wall6.png")));

                warningWallImages[i] = toBufferedImage(
                        ImageIO.read(getClass().getResourceAsStream("/WATVA/Background/Wall" + (i + 7) + ".png")));
            }
        } catch (IOException | NullPointerException e) {
            System.err.println("Error loading wall images!");
            e.printStackTrace();
        }
    }

    private java.awt.image.BufferedImage toBufferedImage(Image src) {
        if (src == null) return null;
        int bs = WALL_BLOCK_SIZE;
        java.awt.image.BufferedImage buf = new java.awt.image.BufferedImage(
                bs, bs, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D sg = buf.createGraphics();
        sg.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        sg.drawImage(src, 0, 0, bs, bs, null);
        sg.dispose();
        return buf;
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

        // Přidej do O(1) lookup setu
        for (Rectangle rect : chunkWalls) {
            addToWallSet(wallTileSet, rect);
        }
    }

    private void rebuildArenaTileSet() {
        wallTileSet.clear();
        if (bossArenaActive) {
            for (Rectangle wall : arenaWalls) addToWallSet(wallTileSet, wall);
            for (Rectangle pillar : arenaPillars) addToWallSet(wallTileSet, pillar);
        } else {
            for (List<Rectangle> chunkWalls : permanentChunkWalls.values()) {
                for (Rectangle rect : chunkWalls) addToWallSet(wallTileSet, rect);
            }
        }
    }

    private void rebuildTempWallTileSet() {
        tempWallTileSet.clear();
        for (WallPattern wp : temporaryWalls) {
            if (!wp.isSolid()) continue;
            for (int row = 0; row < wp.pattern.length; row++) {
                for (int col = 0; col < wp.pattern[row].length; col++) {
                    if (wp.pattern[row][col] == 1) {
                        int bx = wp.x + col * WALL_BLOCK_SIZE;
                        int by = wp.y + row * WALL_BLOCK_SIZE;
                        int tx = bx / WALL_BLOCK_SIZE;
                        int ty = by / WALL_BLOCK_SIZE;
                        tempWallTileSet.add(((long)(tx + 100000)) << 20 | (ty + 100000));
                    }
                }
            }
        }
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

        // Při aktivní aréně nebo boss vlně nejsou žádné dočasné zdi – přeskoč vše
        if (isBossWave || bossArenaActive) return;

        long currentTime = System.currentTimeMillis();
        boolean changed = temporaryWalls.removeIf(wall -> currentTime - wall.spawnTime >= wall.lifetime);

        if (currentTime - lastSpawnTime >= SPAWN_INTERVAL && temporaryWalls.size() < 15) {
            spawnNewTemporaryWall(player);
            lastSpawnTime = currentTime;
            changed = true;
        }

        if (changed || currentTime - lastTempWallRebuild > TEMP_REBUILD_INTERVAL) {
            rebuildTempWallTileSet();
            lastTempWallRebuild = currentTime;
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

            for (int thickness = 0; thickness < 4; thickness++) {
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

            for (int py = 0; py < 1; py++) {
                for (int px = 0; px < 1; px++) {
                    arenaPillars.add(new Rectangle(
                            pillarX + px * WALL_BLOCK_SIZE,
                            pillarY + py * WALL_BLOCK_SIZE,
                            WALL_BLOCK_SIZE,
                            WALL_BLOCK_SIZE
                    ));
                }
            }
        }
        rebuildArenaTileSet();
    }

    public Point getArenaCenter() { return arenaCenter; }
    public int getArenaRadius() { return arenaRadius; }

    public void clearBossArena() {
        bossArenaActive = false;
        arenaWalls.clear();
        arenaPillars.clear();
        arenaCenter = null;
        rebuildArenaTileSet();
        tempWallTileSet.clear();
    }

    public void despawnTemporaryWalls() {
        temporaryWalls.clear();
        tempWallTileSet.clear();
    }

    public boolean isWall(int worldX, int worldY) {
        long key = tileKey(worldX, worldY);
        if (wallTileSet.contains(key)) return true;
        if (!bossArenaActive && tempWallTileSet.contains(key)) return true;
        return false;
    }

    private boolean isPermanentWall(int worldX, int worldY) {
        return wallTileSet.contains(tileKey(worldX, worldY));
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

        int camRight  = cameraX + GamePanel.PANEL_WIDTH  + GamePanel.BLOCK_SIZE;
        int camBottom = cameraY + GamePanel.PANEL_HEIGHT + GamePanel.BLOCK_SIZE;

        if (!bossArenaActive) {
            Image tex = permanentWallImages[5]; // statická textura – žádný random každý frame
            if (tex != null) {
                for (List<Rectangle> chunkWalls : permanentChunkWalls.values()) {
                    for (Rectangle wall : chunkWalls) {
                        // Frustum culling – nekresli bloky mimo obrazovku
                        if (wall.x + GamePanel.BLOCK_SIZE < cameraX || wall.x > camRight) continue;
                        if (wall.y + GamePanel.BLOCK_SIZE < cameraY || wall.y > camBottom) continue;
                        g2d.drawImage(tex, wall.x, wall.y, null);
                    }
                }
            }
        }

        if (bossArenaActive) {
            Image wallTex   = permanentWallImages[5];
            Image pillarTex = permanentWallImages[3];

            if (wallTex != null) {
                for (Rectangle wall : arenaWalls) {
                    if (wall.x + GamePanel.BLOCK_SIZE < cameraX || wall.x > camRight) continue;
                    if (wall.y + GamePanel.BLOCK_SIZE < cameraY || wall.y > camBottom) continue;
                    g2d.drawImage(wallTex, wall.x, wall.y, null);
                }
            }

            if (pillarTex != null) {
                for (Rectangle pillar : arenaPillars) {
                    if (pillar.x + GamePanel.BLOCK_SIZE < cameraX || pillar.x > camRight) continue;
                    if (pillar.y + GamePanel.BLOCK_SIZE < cameraY || pillar.y > camBottom) continue;
                    g2d.drawImage(pillarTex, pillar.x, pillar.y, null);
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
        wallTileSet.clear();
        tempWallTileSet.clear();
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