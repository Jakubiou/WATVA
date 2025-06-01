package Lib;

import WATVA.GamePanel;
import WATVA.MenuPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileInputStream;

public class SettingsPanel extends JPanel {
    private JSlider volumeSlider;
    private JLabel volumeLabel;
    private JLabel volumeValueLabel;
    private JButton backButton;
    private JButton testButton;
    private JButton resetButton;
    private Soundtrack soundtrack;
    private MenuPanel menuPanel;
    private Font pixelPurlFont;

    private static final Color BACKGROUND_COLOR = new Color(45, 45, 50);
    private static final Color PANEL_COLOR = new Color(60, 60, 65);
    private static final Color TEXT_COLOR = new Color(220, 220, 220);
    private static final Color ACCENT_COLOR = new Color(100, 150, 255);

    public SettingsPanel(Soundtrack soundtrack, MenuPanel menuPanel) {
        this.soundtrack = soundtrack;
        this.menuPanel = menuPanel;

        try {
            FileInputStream fontStream = new FileInputStream("res/fonts/PixelPurl.ttf");
            pixelPurlFont = Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont(24f);
        } catch (Exception e) {
            pixelPurlFont = new Font("Arial", Font.BOLD, 24);
            e.printStackTrace();
        }

        initializeUI();
        setupEventListeners();
    }

    private void initializeUI() {
        setOpaque(false);
        setLayout(null);

        int panelWidth = 400;
        int panelHeight = 600;
        int panelX = (GamePanel.PANEL_WIDTH - panelWidth) / 2;
        int panelY = (GamePanel.PANEL_HEIGHT - panelHeight) / 2;
        setBounds(panelX, panelY, panelWidth, panelHeight);

        JLabel titleLabel = new JLabel("SETTINGS", JLabel.CENTER);
        titleLabel.setFont(pixelPurlFont.deriveFont(32f));
        titleLabel.setForeground(Color.ORANGE);
        titleLabel.setBounds(0, 30, panelWidth, 40);
        add(titleLabel);

        volumeLabel = new JLabel("🔊 VOLUME CONTROL", JLabel.CENTER);
        volumeLabel.setFont(pixelPurlFont.deriveFont(18f));
        volumeLabel.setForeground(TEXT_COLOR);
        volumeLabel.setBounds(0, 100, panelWidth, 30);
        add(volumeLabel);

        volumeValueLabel = new JLabel(String.format("%d%%", (int)(soundtrack.getVolume() * 100)), JLabel.CENTER);
        volumeValueLabel.setFont(pixelPurlFont.deriveFont(24f));
        volumeValueLabel.setForeground(ACCENT_COLOR);
        volumeValueLabel.setBounds(0, 140, panelWidth, 30);
        add(volumeValueLabel);

        volumeSlider = new JSlider(0, 100, (int)(soundtrack.getVolume() * 100));
        volumeSlider.setBounds(50, 180, panelWidth - 100, 50);
        setupSliderAppearance();
        add(volumeSlider);

        testButton = createStyledButton("🎵 TEST SOUND", panelWidth - 100, 50);
        testButton.setBounds(50, 260, panelWidth - 100, 50);
        testButton.addActionListener(e -> soundtrack.playOnce());
        add(testButton);

        resetButton = createStyledButton("↻ RESET", panelWidth - 100, 50);
        resetButton.setBounds(50, 330, panelWidth - 100, 50);
        resetButton.addActionListener(e -> {
            volumeSlider.setValue(50);
            soundtrack.setVolume(0.5f);
            volumeValueLabel.setText("50%");
        });
        add(resetButton);

        backButton = createStyledButton("← BACK", panelWidth - 100, 50);
        backButton.setBounds(50, 450, panelWidth - 100, 50);
        backButton.addActionListener(e -> {
            setVisible(false);
            menuPanel.setVisible(true);
        });
        add(backButton);

        setVisible(false);
    }

    private void setupSliderAppearance() {
        volumeSlider.setBackground(new Color(0, 0, 0, 0));
        volumeSlider.setForeground(TEXT_COLOR);
        volumeSlider.setMajorTickSpacing(25);
        volumeSlider.setMinorTickSpacing(5);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setPaintLabels(true);

        volumeSlider.setUI(new javax.swing.plaf.basic.BasicSliderUI(volumeSlider) {
            @Override
            public void paintTrack(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Rectangle trackBounds = trackRect;
                g2.setColor(new Color(100, 100, 105));
                g2.fillRoundRect(trackBounds.x, trackBounds.y + trackBounds.height / 2 - 3,
                        trackBounds.width, 6, 6, 6);

                int thumbPos = thumbRect.x + thumbRect.width / 2;
                g2.setColor(Color.ORANGE);
                g2.fillRoundRect(trackBounds.x, trackBounds.y + trackBounds.height / 2 - 3,
                        thumbPos - trackBounds.x, 6, 6, 6);
            }

            @Override
            public void paintThumb(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(Color.WHITE);
                g2.fillOval(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height);
                g2.setColor(Color.ORANGE);
                g2.setStroke(new BasicStroke(3));
                g2.drawOval(thumbRect.x + 2, thumbRect.y + 2, thumbRect.width - 4, thumbRect.height - 4);
            }
        });

        Font labelFont = pixelPurlFont.deriveFont(12f);
        volumeSlider.setFont(labelFont);
    }

    private JButton createStyledButton(String text, int width, int height) {
        JButton button = new JButton(text);
        button.setFont(pixelPurlFont.deriveFont(16f));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(40, 40, 40));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.ORANGE, 2),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(new Color(60, 60, 60));
                button.setForeground(Color.ORANGE);
            }

            @Override
            public void mouseExited(MouseEvent evt) {
                button.setBackground(new Color(40, 40, 40));
                button.setForeground(Color.WHITE);
            }
        });

        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(width, height));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return button;
    }

    private void setupEventListeners() {
        volumeSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int value = volumeSlider.getValue();
                volumeValueLabel.setText(value + "%");

                float volume = value / 100.0f;
                soundtrack.setVolume(volume);

                if (value == 0) {
                    volumeValueLabel.setText("MUTED");
                    volumeValueLabel.setForeground(Color.RED);
                } else if (value < 30) {
                    volumeValueLabel.setForeground(Color.ORANGE);
                } else {
                    volumeValueLabel.setForeground(ACCENT_COLOR);
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int arc = 40;
        g2.setColor(new Color(30, 30, 30, 220));
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

        g2.setColor(Color.ORANGE);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, arc, arc);
    }
}