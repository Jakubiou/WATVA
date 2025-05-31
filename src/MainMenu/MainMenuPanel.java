package MainMenu;
import WATVA.Game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MainMenuPanel extends JFrame {
    private Image backgroundImage;
    private JButton playButton;
    private JButton settingsButton;
    private JButton quitGameButton;
    private int originalButtonWidth;
    private int originalButtonHeight;

    public MainMenuPanel() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = (int) screenSize.getWidth();
        int screenHeight = (int) screenSize.getHeight();

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setUndecorated(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        ImageIcon backgroundIcon = new ImageIcon("res/watva/background/background1.png");
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
        backgroundPanel.add(settingsButton, gbc);
        backgroundPanel.add(quitGameButton, gbc);

        addActionListeners();

        setVisible(true);
    }

    private void createButtons(int screenWidth, int screenHeight) {
        originalButtonWidth = (int) (screenWidth * 0.28);
        originalButtonHeight = (int) (originalButtonWidth * 0.18);

        ImageIcon playIcon1 = new ImageIcon("res/buttons/Play_button1.png");
        Image playImage1 = playIcon1.getImage().getScaledInstance(originalButtonWidth, originalButtonHeight, Image.SCALE_SMOOTH);
        playIcon1 = new ImageIcon(playImage1);

        ImageIcon playIcon2 = new ImageIcon("res/buttons/Play_button2.png");
        Image playImage2 = playIcon2.getImage().getScaledInstance((int)(originalButtonWidth * 1.1), (int)(originalButtonHeight * 1.1), Image.SCALE_SMOOTH);
        playIcon2 = new ImageIcon(playImage2);

        playButton = new JButton(playIcon1);
        styleButton(playButton, originalButtonWidth, originalButtonHeight);

        ImageIcon settingsIcon1 = new ImageIcon("res/buttons/Settings_button1.png");
        Image settingsImage1 = settingsIcon1.getImage().getScaledInstance(originalButtonWidth, originalButtonHeight, Image.SCALE_SMOOTH);
        settingsIcon1 = new ImageIcon(settingsImage1);

        ImageIcon settingsIcon2 = new ImageIcon("res/buttons/Settings_button2.png");
        Image settingsImage2 = settingsIcon2.getImage().getScaledInstance((int)(originalButtonWidth * 1.1), (int)(originalButtonHeight * 1.1), Image.SCALE_SMOOTH);
        settingsIcon2 = new ImageIcon(settingsImage2);

        settingsButton = new JButton(settingsIcon1);
        styleButton(settingsButton, originalButtonWidth, originalButtonHeight);

        ImageIcon quitGameIcon1 = new ImageIcon("res/buttons/QuitGame_button1.png");
        Image quitGameImage1 = quitGameIcon1.getImage().getScaledInstance(originalButtonWidth, originalButtonHeight, Image.SCALE_SMOOTH);
        quitGameIcon1 = new ImageIcon(quitGameImage1);

        ImageIcon quitGameIcon2 = new ImageIcon("res/buttons/QuitGame_button2.png");
        Image quitGameImage2 = quitGameIcon2.getImage().getScaledInstance((int)(originalButtonWidth * 1.1), (int)(originalButtonHeight * 1.1), Image.SCALE_SMOOTH);
        quitGameIcon2 = new ImageIcon(quitGameImage2);

        quitGameButton = new JButton(quitGameIcon1);
        styleButton(quitGameButton, originalButtonWidth, originalButtonHeight);

        addHoverEffects(playButton, playIcon1, playIcon2);
        addHoverEffects(settingsButton, settingsIcon1, settingsIcon2);
        addHoverEffects(quitGameButton, quitGameIcon1, quitGameIcon2);
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

    private void addActionListeners() {
        playButton.addActionListener(e -> {
            new Game();
            dispose();
        });

        settingsButton.addActionListener(e -> {
            System.out.println("Settings clicked");
        });

        quitGameButton.addActionListener(e -> {
            dispose();
        });
    }
}