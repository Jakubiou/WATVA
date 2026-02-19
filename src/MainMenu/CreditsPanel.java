package MainMenu;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class CreditsPanel extends JFrame {
    private Image backgroundImage;
    private JButton backButton;
    private int scrollY;
    private Timer scrollTimer;
    private Font titleFont;
    private Font headerFont;
    private Font roleFont;
    private Font nameFont;
    private boolean isScrolling = true;

    public CreditsPanel() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = (int) screenSize.getWidth();
        int screenHeight = (int) screenSize.getHeight();

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setUndecorated(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setCustomCursor();

        try {
            ImageIcon backgroundIcon = new ImageIcon(getClass().getClassLoader().getResource("WATVA/Background/Background1.png"));
            backgroundImage = backgroundIcon.getImage().getScaledInstance(screenWidth, screenHeight, Image.SCALE_SMOOTH);
        } catch (Exception e) {
            System.err.println("Background not found.");
        }

        loadFonts();

        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                if (backgroundImage != null) {
                    g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                }

                g2d.setColor(new Color(0, 0, 0, 180));
                g2d.fillRect(0, 0, getWidth(), getHeight());

                drawCredits(g2d);
            }
        };
        mainPanel.setLayout(null);
        setContentPane(mainPanel);

        createBackButton(screenWidth, screenHeight);
        mainPanel.add(backButton);

        scrollY = screenHeight;
        scrollTimer = new Timer(30, e -> {
            if (isScrolling) {
                scrollY -= 2;
                repaint();

                if (scrollY < -5000) {
                    scrollY = screenHeight;
                }
            }
        });
        scrollTimer.start();

        mainPanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    isScrolling = !isScrolling;
                }
            }
        });
        mainPanel.setFocusable(true);
        mainPanel.requestFocusInWindow();

        setVisible(true);
    }

    private void setCustomCursor() {
        try {
            java.io.InputStream is = getClass().getResourceAsStream("/WATVA/Other/Cursor.png");
            if (is != null) {
                Image cursorImage = javax.imageio.ImageIO.read(is);

                int targetSize = 32;

                Image scaledCursor = cursorImage.getScaledInstance(targetSize, targetSize, Image.SCALE_SMOOTH);
                ImageIcon tempIcon = new ImageIcon(scaledCursor);

                int actualWidth = tempIcon.getIconWidth();
                int actualHeight = tempIcon.getIconHeight();

                if (actualWidth <= 0 || actualHeight <= 0) {
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
                        tempIcon.getImage(), hotspot, "CreditsCursor"
                );

                setCursor(customCursor);
            } else {
                setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
            }
        } catch (Exception e) {
            System.err.println("Error loading cursor: " + e.getMessage());
            setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        }
    }

    private void loadFonts() {
        try {
            Font pixelFont = Font.createFont(Font.TRUETYPE_FONT,
                    getClass().getResourceAsStream("/fonts/PixelPurl.ttf"));
            titleFont = pixelFont.deriveFont(Font.BOLD, 140f);
            headerFont = pixelFont.deriveFont(Font.BOLD, 65f);
            roleFont = pixelFont.deriveFont(Font.PLAIN, 50f);
        } catch (Exception e) {
            titleFont = new Font("Arial", Font.BOLD, 140);
            headerFont = new Font("Arial", Font.BOLD, 65);
            roleFont = new Font("Arial", Font.PLAIN, 50);
        }
        nameFont = new Font("SansSerif", Font.BOLD, 32);
    }

    private void createBackButton(int screenWidth, int screenHeight) {
        backButton = new JButton("BACK");
        backButton.setFont(new Font("SansSerif", Font.BOLD, 28));
        backButton.setForeground(Color.WHITE);
        backButton.setBackground(new Color(200, 50, 50));
        backButton.setFocusPainted(false);
        backButton.setBounds(20, 20, 180, 55);

        backButton.addActionListener(e -> {
            scrollTimer.stop();
            new MainMenuPanel();
            dispose();
        });
    }

    private void drawCredits(Graphics2D g2d) {
        int centerX = getWidth() / 2;
        int currentY = scrollY;

        g2d.setFont(titleFont);
        g2d.setColor(new Color(255, 215, 0));
        drawCenteredText(g2d, "WATVA", centerX, currentY, false);
        currentY += 160;

        g2d.setFont(headerFont);
        g2d.setColor(Color.CYAN);
        drawCenteredText(g2d, "Survivor Game", centerX, currentY, false);
        currentY += 250;

        currentY = drawSection(g2d, centerX, currentY, "GAME DEVELOPMENT",
                "Lead Developer", "Jakub Šrámek",
                "Lead Programmer", "Jakub Šrámek",
                "Game Designer", "Jakub Šrámek"
        );
        currentY += 150;

        currentY = drawSection(g2d, centerX, currentY, "ART & UI",
                "Character Artist", "Jakub Šrámek",
                "Environment Artist", "Jakub Šrámek",
                "UI / UX Design", "Jakub Šrámek"
        );
        currentY += 150;

        currentY = drawSection(g2d, centerX, currentY, "INSPIRATION & IDEAS",
                "Art Inspiration", "Vilma Tomanová",
                "", "David Tesař",
                "", "Nikola Poláchová",
                "", "Filip Heger",
                "", "Michal Bělina",
                "Mechanics Consultant", "Michal Bělina",
                "", "Filip Heger",
                "", "Dan Oujeský"
        );
        currentY += 150;

        currentY = drawSection(g2d, centerX, currentY, "QUALITY ASSURANCE",
                "Lead Tester", "Jakub Šrámek",
                "Beta Testing", ""
        );
        currentY += 150;

        currentY = drawSection(g2d, centerX, currentY, "TOOLS & TECH",
                "Language", "Java",
                "IDE", "IntelliJ IDEA"
        );
        currentY += 150;

        currentY = drawSection(g2d, centerX, currentY, "SPECIAL THANKS",
                "Family & Friends", "For endless support"
        );
    }

    private int drawSection(Graphics2D g2d, int centerX, int startY, String sectionTitle, String... lines) {
        int currentY = startY;

        g2d.setFont(headerFont);
        g2d.setColor(new Color(255, 165, 0));
        drawCenteredText(g2d, sectionTitle, centerX, currentY, false);
        currentY += 100;

        for (int i = 0; i < lines.length; i += 2) {
            if (i + 1 < lines.length) {
                String role = lines[i];
                String name = lines[i + 1];

                if (!role.isEmpty()) {
                    currentY += 20;
                    g2d.setFont(roleFont);
                    g2d.setColor(Color.CYAN);
                    drawCenteredText(g2d, role, centerX, currentY, false);
                    currentY += 55;
                } else {
                    currentY += 5;
                }

                g2d.setFont(nameFont);
                g2d.setColor(Color.WHITE);
                drawCenteredText(g2d, name, centerX, currentY, true);
                currentY += 45;
            }
        }
        return currentY;
    }

    private void drawCenteredText(Graphics2D g2d, String text, int centerX, int y, boolean isName) {
        if (text.isEmpty()) return;
        Font originalFont = g2d.getFont();
        if (isName) g2d.setFont(nameFont);

        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int x = centerX - textWidth / 2;

        Color textColor = g2d.getColor();
        g2d.setColor(Color.BLACK);
        g2d.drawString(text, x + 3, y + 3);

        g2d.setColor(textColor);
        g2d.drawString(text, x, y);

        g2d.setFont(originalFont);
    }
}