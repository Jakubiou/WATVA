package Logic;

import Core.Game;
import Player.Player;
import UI.GamePanel;
import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;

public class CrystalExplosion {
    private long startTime = 0;
    private static final long CASTING_DURATION = 1500;
    private static final long ANIMATION_FRAME_TIME = 50;

    private boolean castingComplete = false;
    private boolean complete = false;
    private boolean damageDealt = false;
    private Player player;

    private Image innerGlowImg;
    private Image outerGlowImg;
    private Image crystalParticleImg;

    private Image[] explosionFrames = new Image[6];
    private long explosionStartTime = 0;

    public CrystalExplosion() {
        this.startTime = System.currentTimeMillis();
        loadImages();
    }

    private void loadImages() {
        try {
            outerGlowImg = ImageIO.read(getClass().getResourceAsStream("/WATVA/Crystal/OuterGlow.png"));
            innerGlowImg = ImageIO.read(getClass().getResourceAsStream("/WATVA/Crystal/InnerGlow.png"));
            crystalParticleImg = ImageIO.read(getClass().getResourceAsStream("/WATVA/Crystal/Crystal1.png"));

            for (int i = 0; i < 6; i++) {
                explosionFrames[i] = ImageIO.read(getClass().getResourceAsStream("/WATVA/Crystal/Crys_ex" + (i + 1) + ".png"));
            }
        } catch (IOException | NullPointerException e) {
        }
    }

    public void setPlayer(Player player) { this.player = player; }
    public Player getPlayer() { return player; }

    public void update() {
        if (complete) return;
        long elapsed = System.currentTimeMillis() - startTime;

        if (!castingComplete) {
            if (elapsed >= CASTING_DURATION) {
                castingComplete = true;
                explosionStartTime = System.currentTimeMillis();
            }
        } else {
            long explosionElapsed = System.currentTimeMillis() - explosionStartTime;
            if (explosionElapsed >= ANIMATION_FRAME_TIME * explosionFrames.length) {
                complete = true;
            }
        }
    }

    public boolean shouldTriggerDamage() {
        if (complete && !damageDealt) {
            damageDealt = true;
            return true;
        }
        return false;
    }

    public void draw(Graphics g, int cameraX, int cameraY) {
        if (complete || player == null) return;
        Graphics2D g2d = (Graphics2D) g;
        Composite originalComposite = g2d.getComposite();

        int pCenterX = player.getX() + Player.WIDTH / 2;
        int pCenterY = player.getY() + Player.HEIGHT / 2;

        if (!castingComplete) {
            long elapsed = System.currentTimeMillis() - startTime;
            float phase = (elapsed % 500) / 500f;
            float alphaVal = (150 + (float)(Math.sin(phase * Math.PI * 2) * 50)) / 255f;
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.1f, alphaVal)));

            int glowSize = Game.scale(150);
            if (outerGlowImg != null) g2d.drawImage(outerGlowImg, pCenterX - glowSize / 2, pCenterY - glowSize / 2, glowSize, glowSize, null);

            int numParticles = 8;
            int ringRadius = Game.scale(70);
            int crystalSize = Game.scale(25);
            if (crystalParticleImg != null) {
                for (int i = 0; i < numParticles; i++) {
                    double angle = (i / (double)numParticles) * Math.PI * 2 + (elapsed / 200.0);
                    int px = pCenterX + (int)(Math.cos(angle) * ringRadius);
                    int py = pCenterY + (int)(Math.sin(angle) * ringRadius);
                    g2d.drawImage(crystalParticleImg, px - crystalSize / 2, py - crystalSize / 2, crystalSize, crystalSize, null);
                }
            }
        } else {
            long explosionElapsed = System.currentTimeMillis() - explosionStartTime;
            int frameIndex = (int)(explosionElapsed / ANIMATION_FRAME_TIME);

            if (frameIndex < explosionFrames.length && explosionFrames[frameIndex] != null) {
                int expand = frameIndex * Game.scale(20);
                int screenW = GamePanel.PANEL_WIDTH + Game.scale(600) + expand;
                int screenH = GamePanel.PANEL_HEIGHT + Game.scale(600) + expand;

                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
                g2d.drawImage(explosionFrames[frameIndex], pCenterX - screenW / 2, pCenterY - screenH / 2, screenW, screenH, null);
            }
        }
        g2d.setComposite(originalComposite);
    }

    public boolean isComplete() { return complete; }
    public boolean isWaveActive() { return castingComplete && !complete; }
}