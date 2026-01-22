package Logic;

import Player.Player;
import UI.GamePanel;

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
                blockImages[i] = original.getScaledInstance(
                        GamePanel.BLOCK_SIZE,
                        GamePanel.BLOCK_SIZE,
                        Image.SCALE_SMOOTH
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void drawBackground(java.awt.Graphics g, Player player) {
        if (baseMap == null || baseWidth == 0 || baseHeight == 0) return;

        int px = player.getX();
        int py = player.getY();

        int chunkPixelW = baseWidth * GamePanel.BLOCK_SIZE;
        int chunkPixelH = baseHeight * GamePanel.BLOCK_SIZE;

        int playerChunkX = (int) Math.floor(px / (double) chunkPixelW);
        int playerChunkY = (int) Math.floor(py / (double) chunkPixelH);

        for (int cy = playerChunkY - 1; cy <= playerChunkY + 1; cy++) {
            for (int cx = playerChunkX - 1; cx <= playerChunkX + 1; cx++) {
                int chunkOffsetX = cx * chunkPixelW;
                int chunkOffsetY = cy * chunkPixelH;

                for (int ty = 0; ty < baseHeight; ty++) {
                    for (int tx = 0; tx < baseWidth; tx++) {
                        int blockType = baseMap[ty][tx];
                        Image blockImage = blockImages[blockType];
                        int worldX = chunkOffsetX + tx * GamePanel.BLOCK_SIZE;
                        int worldY = chunkOffsetY + ty * GamePanel.BLOCK_SIZE;
                        g.drawImage(blockImage, worldX, worldY, GamePanel.BLOCK_SIZE, GamePanel.BLOCK_SIZE, null);
                    }
                }
            }
        }
    }

    public int getBaseWidth() { return baseWidth; }
    public int getBaseHeight() { return baseHeight; }
}
