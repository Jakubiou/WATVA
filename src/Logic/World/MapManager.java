package Logic.World;

import Player.Player;
import UI.Game.GamePanel;

import java.io.*;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import java.awt.Image;

public class MapManager {
    private int[][] baseMap;
    private int baseWidth, baseHeight;
    private Image[] blockImages;

    public MapManager(String filename) {
        loadBaseMap(filename);
        loadBlockImages();
    }

    private void loadBaseMap(String filename) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {

            ArrayList<int[]> mapList = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] tokens = line.split(" ");
                int[] row = new int[tokens.length];
                for (int i = 0; i < tokens.length; i++) {
                    try {
                        row[i] = Integer.parseInt(tokens[i].trim());
                    } catch (NumberFormatException e) {
                        row[i] = 0;
                    }
                }
                mapList.add(row);
            }

            baseHeight = mapList.size();
            baseWidth = mapList.isEmpty() ? 0 : mapList.get(0).length;
            baseMap = new int[baseHeight][baseWidth];
            for (int i = 0; i < baseHeight; i++) baseMap[i] = mapList.get(i);

        } catch (Exception e) {
            e.printStackTrace();
            baseWidth = baseHeight = 0;
            baseMap = new int[0][0];
        }
    }

    private void loadBlockImages() {
        blockImages = new Image[26];
        try {
            for (int i = 0; i < blockImages.length; i++) {
                Image original = ImageIO.read(getClass().getResourceAsStream("/WATVA/Background/Block" + i + ".png"));
                if (original == null) continue;
                int bs = GamePanel.BLOCK_SIZE;
                java.awt.image.BufferedImage buf = new java.awt.image.BufferedImage(
                        bs, bs, java.awt.image.BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D sg = buf.createGraphics();
                sg.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                        java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                sg.drawImage(original, 0, 0, bs, bs, null);
                sg.dispose();
                blockImages[i] = buf;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private java.util.HashMap<Long, java.awt.image.BufferedImage> chunkCache = new java.util.HashMap<>();
    private static final int TILES_PER_CHUNK_CACHE = 20; // same as chunk size used in drawBackground

    private long chunkKey(int cx, int cy) {
        return ((long)(cx + 100000)) << 20 | (cy + 100000);
    }

    private java.awt.image.BufferedImage renderChunk(int cx, int cy) {
        int chunkPixelW = baseWidth  * GamePanel.BLOCK_SIZE;
        int chunkPixelH = baseHeight * GamePanel.BLOCK_SIZE;
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                chunkPixelW, chunkPixelH, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D cg = img.createGraphics();
        cg.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for (int ty = 0; ty < baseHeight; ty++) {
            for (int tx = 0; tx < baseWidth; tx++) {
                int blockType = baseMap[ty][tx];
                Image blockImage = blockImages[blockType];
                if (blockImage != null) {
                    cg.drawImage(blockImage, tx * GamePanel.BLOCK_SIZE, ty * GamePanel.BLOCK_SIZE, null);
                }
            }
        }
        cg.dispose();
        return img;
    }

    public void drawBackground(java.awt.Graphics g, Player player) {
        if (baseMap == null || baseWidth == 0 || baseHeight == 0) return;

        int px = player.getX();
        int py = player.getY();

        int chunkPixelW = baseWidth  * GamePanel.BLOCK_SIZE;
        int chunkPixelH = baseHeight * GamePanel.BLOCK_SIZE;

        int playerChunkX = (int) Math.floor(px / (double) chunkPixelW);
        int playerChunkY = (int) Math.floor(py / (double) chunkPixelH);

        int camX   = Logic.GameLogic.cameraX;
        int camY   = Logic.GameLogic.cameraY;
        int camX2  = camX + GamePanel.PANEL_WIDTH  + chunkPixelW;
        int camY2  = camY + GamePanel.PANEL_HEIGHT + chunkPixelH;

        for (int cy = playerChunkY - 1; cy <= playerChunkY + 1; cy++) {
            for (int cx = playerChunkX - 1; cx <= playerChunkX + 1; cx++) {
                int chunkOffsetX = cx * chunkPixelW;
                int chunkOffsetY = cy * chunkPixelH;

                if (chunkOffsetX + chunkPixelW < camX || chunkOffsetX > camX2) continue;
                if (chunkOffsetY + chunkPixelH < camY || chunkOffsetY > camY2) continue;

                long key = chunkKey(cx, cy);
                java.awt.image.BufferedImage cached = chunkCache.get(key);
                if (cached == null) {
                    cached = renderChunk(cx, cy);
                    chunkCache.put(key, cached);
                }
                g.drawImage(cached, chunkOffsetX, chunkOffsetY, null);
            }
        }
    }

    public int getBaseWidth() { return baseWidth; }
    public int getBaseHeight() { return baseHeight; }
}