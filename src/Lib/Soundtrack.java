package Lib;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class Soundtrack {
    private Clip clip;
    private FloatControl volumeControl;

    public Soundtrack(String filePath) {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(filePath));
            clip = AudioSystem.getClip();
            clip.open(audioInputStream);

            volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);

            setVolume(0.5f);

        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void playLoop() {
        if (clip != null) {
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.start();
        }
    }

    public void playOnce() {
        if (clip != null) {
            clip.stop();
            clip.setFramePosition(0);
            clip.start();
        }
    }


    public void stop() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }

    public void setVolume(float volume) {
        if (volumeControl != null) {
            float min = volumeControl.getMinimum();
            float max = volumeControl.getMaximum();
            float newVolume = min + (max - min) * volume;
            volumeControl.setValue(newVolume);
        }
    }

    public float getVolume() {
        if (volumeControl != null) {
            float min = volumeControl.getMinimum();
            float max = volumeControl.getMaximum();
            return (volumeControl.getValue() - min) / (max - min);
        }
        return 1.0f;
    }
}
