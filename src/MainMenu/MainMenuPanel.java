package MainMenu;

import Core.Game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Main menu panel with Play, Credits, and Quit buttons
 */
public class MainMenuPanel extends JFrame {
    private Image backgroundImage;
    private JButton playButton;
    private JButton creditsButton;
    private JButton quitGameButton;
    private int originalButtonWidth;
    private int originalButtonHeight;
    private ImageIcon playIcon1;
    private ImageIcon playIcon2;

    public MainMenuPanel() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = (int) screenSize.getWidth();
        int screenHeight = (int) screenSize.getHeight();

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setUndecorated(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

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
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(20, 0, 20, 0);

        backgroundPanel.add(playButton, gbc);
        backgroundPanel.add(creditsButton, gbc);
        backgroundPanel.add(quitGameButton, gbc);

        addActionListeners();

        setVisible(true);
    }

    private void createButtons(int screenWidth, int screenHeight) {
        originalButtonWidth = (int) (screenWidth * 0.28);
        originalButtonHeight = (int) (originalButtonWidth * 0.18);

        // Play Button
        playIcon1 = new ImageIcon(new ImageIcon(getClass().getClassLoader().getResource("Buttons/Play_button1.png"))
                .getImage().getScaledInstance(originalButtonWidth, originalButtonHeight, Image.SCALE_SMOOTH));

        playIcon2 = new ImageIcon(new ImageIcon(getClass().getClassLoader().getResource("Buttons/Play_button2.png"))
                .getImage().getScaledInstance((int)(originalButtonWidth * 1.1), (int)(originalButtonHeight * 1.1), Image.SCALE_SMOOTH));

        playButton = new JButton(playIcon1);
        styleButton(playButton, originalButtonWidth, originalButtonHeight);

        // Credits Button (vytvoříme vlastní vzhled)
        creditsButton = createStyledTextButton("CREDITS", originalButtonWidth, originalButtonHeight);

        // Quit Button
        ImageIcon quitGameIcon1 = new ImageIcon(new ImageIcon(getClass().getClassLoader().getResource("Buttons/QuitGame_button1.png"))
                .getImage().getScaledInstance(originalButtonWidth, originalButtonHeight, Image.SCALE_SMOOTH));

        ImageIcon quitGameIcon2 = new ImageIcon(new ImageIcon(getClass().getClassLoader().getResource("Buttons/QuitGame_button2.png"))
                .getImage().getScaledInstance((int)(originalButtonWidth * 1.1), (int)(originalButtonHeight * 1.1), Image.SCALE_SMOOTH));

        quitGameButton = new JButton(quitGameIcon1);
        styleButton(quitGameButton, originalButtonWidth, originalButtonHeight);

        addHoverEffects(playButton, playIcon1, playIcon2);
        addHoverEffects(quitGameButton, quitGameIcon1, quitGameIcon2);
    }

    private JButton createStyledTextButton(String text, int width, int height) {
        JButton button = new JButton(text);

        // Načtení fontu
        Font buttonFont;
        try {
            buttonFont = Font.createFont(Font.TRUETYPE_FONT,
                    getClass().getResourceAsStream("/fonts/PixelPurl.ttf")).deriveFont(Font.BOLD, 48f);
        } catch (Exception e) {
            buttonFont = new Font("Arial", Font.BOLD, 48);
        }

        button.setFont(buttonFont);
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(50, 50, 70));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 165, 0), 4),
                BorderFactory.createEmptyBorder(20, 40, 20, 40)
        ));
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(width, height));

        // Hover efekt pro text button
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(70, 70, 100));
                button.setForeground(new Color(255, 215, 0));
                button.setPreferredSize(new Dimension((int)(width * 1.1), (int)(height * 1.1)));
                button.revalidate();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(50, 50, 70));
                button.setForeground(Color.WHITE);
                button.setPreferredSize(new Dimension(width, height));
                button.revalidate();
            }
        });

        return button;
    }

    private void styleButton(JButton button, int width, int height) {
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setPreferredSize(new Dimension(width, height));
    }

    private void addHoverEffects(JButton button, ImageIcon normalIcon, ImageIcon hoverIcon) {
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setIcon(hoverIcon);
                button.setPreferredSize(new Dimension((int)(originalButtonWidth * 1.1), (int)(originalButtonHeight * 1.1)));
                button.revalidate();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setIcon(normalIcon);
                button.setPreferredSize(new Dimension(originalButtonWidth, originalButtonHeight));
                button.revalidate();
            }
        });
    }

    private void addActionListeners() {
        // Play button
        playButton.addActionListener(e -> {
            playButton.setIcon(playIcon1);
            playButton.setPreferredSize(new Dimension(originalButtonWidth, originalButtonHeight));
            playButton.revalidate();
            playButton.repaint();

            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    new Game();
                    return null;
                }
                @Override
                protected void done() {
                    setCursor(Cursor.getDefaultCursor());
                    dispose();
                }
            };
            worker.execute();
        });

        // Credits button
        creditsButton.addActionListener(e -> {
            new CreditsPanel();
            dispose();
        });

        // Quit button
        quitGameButton.addActionListener(e -> dispose());
    }
}