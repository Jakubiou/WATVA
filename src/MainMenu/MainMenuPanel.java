package MainMenu;
import Core.Game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Main menu frame with interactive buttons and background image.
 */
public class MainMenuPanel extends JFrame {
    private Image backgroundImage;
    private JButton playButton;
    private JButton settingsButton;
    private JButton quitGameButton;
    private int originalButtonWidth;
    private int originalButtonHeight;

    /**
     * Creates and displays the main menu in fullscreen mode.
     */
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
        backgroundPanel.add(quitGameButton, gbc);

        addActionListeners();

        setVisible(true);
    }

    /**
     * Creates and styles the menu buttons.
     * @param screenWidth The screen width for button sizing
     * @param screenHeight The screen height for button sizing
     */
    private void createButtons(int screenWidth, int screenHeight) {
        originalButtonWidth = (int) (screenWidth * 0.28);
        originalButtonHeight = (int) (originalButtonWidth * 0.18);

        ImageIcon playIcon1 = new ImageIcon(getClass().getClassLoader().getResource("Buttons/Play_button1.png"));
        Image playImage1 = playIcon1.getImage().getScaledInstance(originalButtonWidth, originalButtonHeight, Image.SCALE_SMOOTH);
        playIcon1 = new ImageIcon(playImage1);

        ImageIcon playIcon2 = new ImageIcon(getClass().getClassLoader().getResource("Buttons/Play_button2.png"));
        Image playImage2 = playIcon2.getImage().getScaledInstance((int)(originalButtonWidth * 1.1), (int)(originalButtonHeight * 1.1), Image.SCALE_SMOOTH);
        playIcon2 = new ImageIcon(playImage2);

        playButton = new JButton(playIcon1);
        styleButton(playButton, originalButtonWidth, originalButtonHeight);


        ImageIcon quitGameIcon1 = new ImageIcon(getClass().getClassLoader().getResource("Buttons/QuitGame_button1.png"));
        Image quitGameImage1 = quitGameIcon1.getImage().getScaledInstance(originalButtonWidth, originalButtonHeight, Image.SCALE_SMOOTH);
        quitGameIcon1 = new ImageIcon(quitGameImage1);

        ImageIcon quitGameIcon2 = new ImageIcon(getClass().getClassLoader().getResource("Buttons/QuitGame_button2.png"));
        Image quitGameImage2 = quitGameIcon2.getImage().getScaledInstance((int)(originalButtonWidth * 1.1), (int)(originalButtonHeight * 1.1), Image.SCALE_SMOOTH);
        quitGameIcon2 = new ImageIcon(quitGameImage2);

        quitGameButton = new JButton(quitGameIcon1);
        styleButton(quitGameButton, originalButtonWidth, originalButtonHeight);

        addHoverEffects(playButton, playIcon1, playIcon2);
        addHoverEffects(quitGameButton, quitGameIcon1, quitGameIcon2);
    }

    /**
     * Applies visual styling to a button.
     * @param button The button to style
     * @param width The button width
     * @param height The button height
     */
    private void styleButton(JButton button, int width, int height) {
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setPreferredSize(new Dimension(width, height));
    }

     /**
     * Adds hover effects to a button.
     * @param button The button to enhance
     * @param normalIcon The default button icon
     * @param hoverIcon The icon to show on hover
     */
    private void addHoverEffects(JButton button, ImageIcon normalIcon, ImageIcon hoverIcon) {
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setIcon(hoverIcon);
                int newWidth = (int)(originalButtonWidth * 1.1);
                int newHeight = (int)(originalButtonHeight * 1.1);
                button.setPreferredSize(new Dimension(newWidth, newHeight));
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

    /**
     * Adds action listeners to handle button clicks.
     */
    private void addActionListeners() {
        playButton.addActionListener(e -> {
            new Game();
            dispose();
        });

        quitGameButton.addActionListener(e -> {
            dispose();
        });
    }
}