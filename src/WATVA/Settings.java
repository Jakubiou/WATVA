package WATVA;

import javax.swing.*;
import java.awt.*;

public class Settings extends JFrame {
    private JSlider volumeSlider;
    private JLabel volumeLabel;
    private Soundtrack soundtrack;

    public Settings(Soundtrack soundtrack) {
        this.soundtrack = soundtrack;

        setTitle("Settings");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel volumePanel = new JPanel();
        volumePanel.setLayout(new BorderLayout());
        volumeLabel = new JLabel("Volume: " + (int) (soundtrack.getVolume() * 100) + "%", JLabel.CENTER);
        volumeSlider = new JSlider(0, 100, (int) (soundtrack.getVolume() * 100));
        volumeSlider.setMajorTickSpacing(20);
        volumeSlider.setMinorTickSpacing(5);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setPaintLabels(true);

        volumeSlider.addChangeListener(e -> {
            int value = volumeSlider.getValue();
            volumeLabel.setText("Volume: " + value + "%");
            soundtrack.setVolume(value / 100.0f);
        });

        volumePanel.add(volumeLabel, BorderLayout.NORTH);
        volumePanel.add(volumeSlider, BorderLayout.CENTER);

        add(volumePanel, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        add(closeButton, BorderLayout.SOUTH);
    }
}
