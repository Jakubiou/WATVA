package MainMenu;

import Core.Game;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.InputStream;

public class MainMenuPanel extends JFrame {
    private Image backgroundImage;
    private JButton playButton;
    private JButton tutorialButton;
    private JButton creditsButton;
    private JButton quitGameButton;
    private int originalButtonWidth;
    private int originalButtonHeight;

    private ImageIcon playIcon1, playIcon2;
    private ImageIcon tutorialIcon1, tutorialIcon2;
    private ImageIcon creditsIcon1, creditsIcon2;
    private ImageIcon quitIcon1, quitIcon2;

    private Font buttonFont;

    public MainMenuPanel() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = (int) screenSize.getWidth();
        int screenHeight = (int) screenSize.getHeight();

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setUndecorated(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setCustomCursor();

        try {
            buttonFont = Font.createFont(Font.TRUETYPE_FONT,
                            getClass().getResourceAsStream("/fonts/PixelPurl.ttf"))
                    .deriveFont(Font.BOLD, 64);
        } catch (Exception e) {
            buttonFont = new Font("Arial", Font.BOLD, 64);
        }

        ImageIcon backgroundIcon = new ImageIcon(getClass().getClassLoader().getResource("WATVA/Background/Background1.png"));
        backgroundImage = backgroundIcon.getImage().getScaledInstance(screenWidth, screenHeight, Image.SCALE_SMOOTH);

        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            }
        };
        backgroundPanel.setLayout(new GridBagLayout());
        setContentPane(backgroundPanel);

        createButtons(screenWidth, screenHeight);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(15, 0, 15, 0);

        backgroundPanel.add(playButton, gbc);
        backgroundPanel.add(tutorialButton, gbc);
        backgroundPanel.add(creditsButton, gbc);
        backgroundPanel.add(quitGameButton, gbc);

        addActionListeners();

        setVisible(true);
    }

    private void setCustomCursor() {
        try {
            InputStream is = getClass().getResourceAsStream("/WATVA/Other/Cursor.png");
            if (is != null) {
                Image cursorImage = ImageIO.read(is);

                int targetSize = 32;

                Image scaledCursor = cursorImage.getScaledInstance(targetSize, targetSize, Image.SCALE_SMOOTH);
                ImageIcon tempIcon = new ImageIcon(scaledCursor);

                int actualWidth = tempIcon.getIconWidth();
                int actualHeight = tempIcon.getIconHeight();

                if (actualWidth <= 0 || actualHeight <= 0) {
                    System.err.println("Invalid cursor dimensions: " + actualWidth + "x" + actualHeight);
                    setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
                    return;
                }

                int hotspotX = actualWidth / 2;
                int hotspotY = actualHeight / 2;

                if (hotspotX >= actualWidth) hotspotX = actualWidth - 1;
                if (hotspotY >= actualHeight) hotspotY = actualHeight - 1;

                if (hotspotX < 0) hotspotX = 0;
                if (hotspotY < 0) hotspotY = 0;

                Point hotspot = new Point(hotspotX, hotspotY);

                Cursor customCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                        tempIcon.getImage(), hotspot, "MenuCursor"
                );

                this.setCursor(customCursor);
            } else {
                setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
            }
        } catch (Exception e) {
            System.err.println("Error loading custom cursor: " + e.getMessage());
            e.printStackTrace();
            setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        }
    }

    private void createButtons(int screenWidth, int screenHeight) {
        originalButtonWidth = (int) (screenWidth * 0.28);
        originalButtonHeight = (int) (originalButtonWidth * 0.18);

        try {
            playIcon1 = loadIcon("Buttons/Play_button1.png");
            playIcon2 = loadIcon("Buttons/Play_button2.png");
            tutorialIcon1 = loadIcon("Buttons/Tutorial_button1.png");
            tutorialIcon2 = loadIcon("Buttons/Tutorial_button2.png");
            creditsIcon1 = loadIcon("Buttons/Credit_button1.png");
            creditsIcon2 = loadIcon("Buttons/Credit_button2.png");
            quitIcon1 = loadIcon("Buttons/QuitGame_button1.png");
            quitIcon2 = loadIcon("Buttons/QuitGame_button2.png");
        } catch (Exception e) {
            System.err.println("Error loading button images: " + e.getMessage());
        }

        playButton = createButtonWithText(playIcon1, playIcon2, "PLAY");
        tutorialButton = createButtonWithText(tutorialIcon1, tutorialIcon2, "TUTORIAL");
        creditsButton = createButtonWithText(creditsIcon1, creditsIcon2, "CREDITS");
        quitGameButton = createButtonWithText(quitIcon1, quitIcon2, "QUIT GAME");
    }

    private ImageIcon loadIcon(String path) {
        try {
            Image img = new ImageIcon(getClass().getClassLoader().getResource(path)).getImage();
            return new ImageIcon(img);
        } catch (Exception e) {
            System.err.println("Could not load icon: " + path);
            return null;
        }
    }


    private JButton createButtonWithText(final ImageIcon normalIcon, final ImageIcon hoverIcon, final String text) {
        JButton button = new JButton(normalIcon) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                g2d.setFont(buttonFont);
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(text);
                int textHeight = fm.getHeight();

                int x = (getWidth() - textWidth) / 2;
                int y = (getHeight() + textHeight) / 2 - fm.getDescent();

                Boolean hoveredProp = (Boolean) getClientProperty("hovered");
                boolean isHovered = (hoveredProp != null && hoveredProp);
                Color textColor = isHovered ? new Color(255, 215, 0) : Color.WHITE;

                drawOutlinedText(g2d, text, x, y, textColor);
            }

            @Override
            public void setIcon(Icon icon) {
                super.setIcon(icon);
                repaint();
            }
        };

        styleButton(button);
        addIndividualHoverEffect(button, normalIcon, hoverIcon);

        return button;
    }

    private void drawOutlinedText(Graphics2D g2d, String text, int x, int y, Color mainColor) {
        int outlineSize = Game.scale(3);

        g2d.setColor(Color.BLACK);
        g2d.drawString(text, x - outlineSize, y - outlineSize);
        g2d.drawString(text, x + outlineSize, y - outlineSize);
        g2d.drawString(text, x - outlineSize, y + outlineSize);
        g2d.drawString(text, x + outlineSize, y + outlineSize);
        g2d.drawString(text, x - outlineSize, y);
        g2d.drawString(text, x + outlineSize, y);
        g2d.drawString(text, x, y - outlineSize);
        g2d.drawString(text, x, y + outlineSize);

        g2d.setColor(mainColor);
        g2d.drawString(text, x, y);
    }

    private void styleButton(JButton button) {
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setPreferredSize(new Dimension(originalButtonWidth, originalButtonHeight));
        button.setMinimumSize(new Dimension(originalButtonWidth, originalButtonHeight));
        button.setMaximumSize(new Dimension(originalButtonWidth, originalButtonHeight));
    }

    private void addIndividualHoverEffect(final JButton button,
                                          final ImageIcon normalIcon,
                                          final ImageIcon hoverIcon) {

        int normalW = originalButtonWidth;
        int normalH = originalButtonHeight;

        int hoverW = (int)(normalW * 1.05);
        int hoverH = (int)(normalH * 1.05);

        button.setIcon(scaleIcon(normalIcon, normalW, normalH));

        button.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                button.setIcon(scaleIcon(hoverIcon, hoverW, hoverH));
                button.putClientProperty("hovered", true);

                button.setPreferredSize(new Dimension(hoverW, hoverH));
                button.revalidate();
                button.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setIcon(scaleIcon(normalIcon, normalW, normalH));
                button.putClientProperty("hovered", false);

                button.setPreferredSize(new Dimension(normalW, normalH));
                button.revalidate();
                button.repaint();
            }
        });
    }

    private ImageIcon scaleIcon(ImageIcon icon, int width, int height) {
        Image scaled = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }




    private void addActionListeners() {
        playButton.addActionListener(e -> {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override protected Void doInBackground() { new Game(false); return null; }
                @Override protected void done() { dispose(); }
            };
            worker.execute();
        });

        tutorialButton.addActionListener(e -> {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override protected Void doInBackground() { new Game(true); return null; }
                @Override protected void done() { dispose(); }
            };
            worker.execute();
        });

        creditsButton.addActionListener(e -> {
            new CreditsPanel();
            dispose();
        });

        quitGameButton.addActionListener(e -> System.exit(0));
    }
}