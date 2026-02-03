package Logic;

import Core.Game;
import Player.Player;
import UI.GamePanel;
import java.awt.*;

/**
 * Wave completion effect - player casting animation followed by explosion wave
 */
public class CrystalExplosion {
    private long startTime = 0;
    private static final long CASTING_DURATION = 1500;

    private boolean castingComplete = false;
    private boolean waveExpanding = false;
    private int waveRadius = 0;
    private int maxWaveRadius = Game.scale(1500);
    private int waveSpeed = Game.scale(100);

    private boolean complete = false;
    private Player player;

    public CrystalExplosion() {
        this.startTime = System.currentTimeMillis();
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void update() {
        long elapsed = System.currentTimeMillis() - startTime;

        if (!castingComplete) {
            if (elapsed >= CASTING_DURATION) {
                castingComplete = true;
                waveExpanding = true;
            }
        } else if (waveExpanding) {
            waveRadius += waveSpeed;

            if (waveRadius >= maxWaveRadius) {
                complete = true;
            }
        }
    }

    /**
     * Draws the effect - player glow during casting, then explosion wave
     */
    public void draw(Graphics g, int cameraX, int cameraY) {
        Graphics2D g2d = (Graphics2D) g;

        if (!castingComplete && player != null) {
            long elapsed = System.currentTimeMillis() - startTime;
            float phase = (float)(elapsed % 500) / 500f;

            int glowSize = Game.scale(80) + (int)(Math.sin(phase * Math.PI * 2) * Game.scale(20));
            int alpha = 100 + (int)(Math.sin(phase * Math.PI * 2) * 50);

            g2d.setColor(new Color(100, 200, 255, Math.max(0, Math.min(255, alpha))));
            g2d.fillOval(
                    player.getX() - glowSize/2 + Player.WIDTH/2,
                    player.getY() - glowSize/2 + Player.HEIGHT/2,
                    glowSize, glowSize
            );

            int innerGlow = glowSize / 2;
            g2d.setColor(new Color(150, 220, 255, Math.max(0, Math.min(255, alpha + 50))));
            g2d.fillOval(
                    player.getX() - innerGlow/2 + Player.WIDTH/2,
                    player.getY() - innerGlow/2 + Player.HEIGHT/2,
                    innerGlow, innerGlow
            );

            int numParticles = 12;
            int ringRadius = Game.scale(60);
            for (int i = 0; i < numParticles; i++) {
                double angle = (i / (double)numParticles) * Math.PI * 2 + (elapsed / 200.0);
                int particleX = player.getX() + Player.WIDTH/2 + (int)(Math.cos(angle) * ringRadius);
                int particleY = player.getY() + Player.HEIGHT/2 + (int)(Math.sin(angle) * ringRadius);

                g2d.setColor(new Color(200, 230, 255, Math.max(0, Math.min(255, alpha))));
                g2d.fillOval(particleX - Game.scale(5), particleY - Game.scale(5),
                        Game.scale(10), Game.scale(10));
            }

        } else if (waveExpanding && waveRadius > 0) {
            int playerCenterX = player.getX() + Player.WIDTH/2;
            int playerCenterY = player.getY() + Player.HEIGHT/2;

            float progress = (float)waveRadius / maxWaveRadius;
            int baseAlpha = (int)(200 * (1 - progress));

            g2d.setColor(new Color(50, 150, 255, Math.max(0, baseAlpha)));
            g2d.fillOval(
                    playerCenterX - waveRadius,
                    playerCenterY - waveRadius,
                    waveRadius * 2,
                    waveRadius * 2
            );

            int innerRadius = (int)(waveRadius * 0.7);
            int innerAlpha = Math.max(0, (int)(150 * (1 - progress)));
            g2d.setColor(new Color(100, 200, 255, innerAlpha));
            g2d.fillOval(
                    playerCenterX - innerRadius,
                    playerCenterY - innerRadius,
                    innerRadius * 2,
                    innerRadius * 2
            );
        }
    }

    public boolean isComplete() {
        return complete;
    }

    public boolean isWaveActive() {
        return waveExpanding;
    }

    public int getWaveRadius() {
        return waveRadius;
    }

    public Player getPlayer() {
        return player;
    }
}