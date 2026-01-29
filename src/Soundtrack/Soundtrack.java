package Soundtrack;

import javax.sound.sampled.*;
import java.io.IOException;
import java.io.InputStream;

/**
 * Handles audio playback and volume control for game soundtracks.
 */
public class Soundtrack {
    private Clip clip;
    private FloatControl volumeControl;

    /**
     * Creates a new Soundtrack instance from the specified audio file.
     * @param filePath Path to the audio file to load
     */
    public Soundtrack(String filePath) {
        try {
            InputStream audioSrc = getClass().getResourceAsStream(filePath);
            if (audioSrc == null) {
                throw new IllegalArgumentException("Sound file not found: " + filePath);
            }

            InputStream bufferedIn = new java.io.BufferedInputStream(audioSrc);
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bufferedIn);
            clip = AudioSystem.getClip();
            clip.open(audioInputStream);

            volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);

            setVolume(0.00f);

        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts continuous playback of the soundtrack in a loop.
     */
    public void playLoop() {
        if (clip != null) {
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.start();
        }
    }

    /**
     * Plays the soundtrack once from the beginning.
     */
    public void playOnce() {
        if (clip != null) {
            clip.stop();
            clip.setFramePosition(0);
            clip.start();
        }
    }

    /**
     * Stops the currently playing soundtrack.
     */
    public void stop() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }

    /**
     * Sets the playback volume (0.0 to 1.0 scale).
     * @param volume The volume level (0.0 = silent, 1.0 = max)
     */
    public void setVolume(float volume) {
        if (volumeControl != null) {
            float min = volumeControl.getMinimum();
            float max = volumeControl.getMaximum();
            float newVolume = min + (max - min) * volume;
            volumeControl.setValue(newVolume);
        }
    }

    /**
     * Gets the current volume level.
     * @return Current volume level (0.0 to 1.0)
     */
    public float getVolume() {
        if (volumeControl != null) {
            float min = volumeControl.getMinimum();
            float max = volumeControl.getMaximum();
            return (volumeControl.getValue() - min) / (max - min);
        }
        return 1.0f;
    }
}
