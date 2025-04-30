package MainMenu;
import WATVA.Game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MainMenuPanel extends JFrame {

    public MainMenuPanel() {
        setSize(1600, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        ImageIcon backgroundIcon = new ImageIcon("res/watva/background/background1.png");
        Image backgroundImage = backgroundIcon.getImage().getScaledInstance(1600, 850, Image.SCALE_SMOOTH);
        backgroundIcon = new ImageIcon(backgroundImage);

        ImageIcon finalBackgroundIcon = backgroundIcon;
        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(finalBackgroundIcon.getImage(), 0, 0, null);
            }
        };
        backgroundPanel.setLayout(null);
        setContentPane(backgroundPanel);

        ImageIcon playRPGIcon1 = new ImageIcon("res/buttons/PlayRPG_button1.png");
        Image playRPGImage1 = playRPGIcon1.getImage().getScaledInstance(300, 50, Image.SCALE_SMOOTH);
        playRPGIcon1 = new ImageIcon(playRPGImage1);

        ImageIcon playRPGIcon2 = new ImageIcon("res/buttons/PlayRPG_button2.png");
        Image playRPGImage2 = playRPGIcon2.getImage().getScaledInstance(320, 60, Image.SCALE_SMOOTH);
        playRPGIcon2 = new ImageIcon(playRPGImage2);

        JButton playRPGButton = new JButton(playRPGIcon1);

        playRPGButton.setBorderPainted(false);
        playRPGButton.setContentAreaFilled(false);
        playRPGButton.setFocusPainted(false);
        playRPGButton.setOpaque(false);

        ImageIcon playTDIcon1 = new ImageIcon("res/buttons/PlayTD_button1.png");
        Image playTDImage1 = playTDIcon1.getImage().getScaledInstance(300, 50, Image.SCALE_SMOOTH);
        playTDIcon1 = new ImageIcon(playTDImage1);

        ImageIcon playTDIcon2 = new ImageIcon("res/buttons/PlayTD_button2.png");
        Image playTDImage2 = playTDIcon2.getImage().getScaledInstance(320, 60, Image.SCALE_SMOOTH);
        playTDIcon2 = new ImageIcon(playTDImage2);

        JButton playTDButton = new JButton(playTDIcon1);

        playTDButton.setBorderPainted(false);
        playTDButton.setContentAreaFilled(false);
        playTDButton.setFocusPainted(false);
        playTDButton.setOpaque(false);

        ImageIcon settingsIcon1 = new ImageIcon("res/buttons/Settings_button1.png");
        Image settingsImage1 = settingsIcon1.getImage().getScaledInstance(300, 50, Image.SCALE_SMOOTH);
        settingsIcon1 = new ImageIcon(settingsImage1);

        ImageIcon settingsIcon2 = new ImageIcon("res/buttons/Settings_button2.png");
        Image settingsImage2 = settingsIcon2.getImage().getScaledInstance(320, 60, Image.SCALE_SMOOTH);
        settingsIcon2 = new ImageIcon(settingsImage2);

        JButton settingsButton = new JButton(settingsIcon1);

        settingsButton.setBorderPainted(false);
        settingsButton.setContentAreaFilled(false);
        settingsButton.setFocusPainted(false);
        settingsButton.setOpaque(false);

        ImageIcon quitGameIcon1 = new ImageIcon("res/buttons/QuitGame_button1.png");
        Image quitGameImage1 = quitGameIcon1.getImage().getScaledInstance(300, 50, Image.SCALE_SMOOTH);
        quitGameIcon1 = new ImageIcon(quitGameImage1);

        ImageIcon quitGameIcon2 = new ImageIcon("res/buttons/QuitGame_button2.png");
        Image quitGameImage2 = quitGameIcon2.getImage().getScaledInstance(320, 60, Image.SCALE_SMOOTH);
        quitGameIcon2 = new ImageIcon(quitGameImage2);

        JButton quitGameButton = new JButton(quitGameIcon1);

        quitGameButton.setBorderPainted(false);
        quitGameButton.setContentAreaFilled(false);
        quitGameButton.setFocusPainted(false);
        quitGameButton.setOpaque(false);

        ImageIcon playRPGTDIcon1 = new ImageIcon("res/buttons/PlayRPGTD_button1.png");
        Image playRPGTDImage1 = playRPGTDIcon1.getImage().getScaledInstance(300, 50, Image.SCALE_SMOOTH);
        playRPGTDIcon1 = new ImageIcon(playRPGTDImage1);

        ImageIcon playRPGTDIcon2 = new ImageIcon("res/buttons/PlayRPGTD_button2.png");
        Image playRPGTDImage2 = playRPGTDIcon2.getImage().getScaledInstance(320, 60, Image.SCALE_SMOOTH);
        playRPGTDIcon2 = new ImageIcon(playRPGTDImage2);

        JButton playRPGTDButton = new JButton(playRPGTDIcon1);

        playRPGTDButton.setBorderPainted(false);
        playRPGTDButton.setContentAreaFilled(false);
        playRPGTDButton.setFocusPainted(false);
        playRPGTDButton.setOpaque(false);

        playRPGButton.setBounds(615, 150, 300, 50);
        playTDButton.setBounds(615, 250, 300, 50);
        playRPGTDButton.setBounds(615, 350, 300, 50);
        settingsButton.setBounds(615, 450, 300, 50);
        quitGameButton.setBounds(615, 550, 300, 50);

        backgroundPanel.add(playRPGButton);
        backgroundPanel.add(playTDButton);
        backgroundPanel.add(playRPGTDButton);
        backgroundPanel.add(settingsButton);
        backgroundPanel.add(quitGameButton);

        ImageIcon finalPlayRPGIcon = playRPGIcon2;
        ImageIcon finalPlayRPGIcon1 = playRPGIcon1;
        playRPGButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                playRPGButton.setIcon(finalPlayRPGIcon);
                playRPGButton.setBounds(605, 140, 320, 60);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                playRPGButton.setIcon(finalPlayRPGIcon1);
                playRPGButton.setBounds(615, 150, 300, 50);
            }
        });

        ImageIcon finalPlayTDIcon = playTDIcon2;
        ImageIcon finalPlayTDIcon1 = playTDIcon1;
        playTDButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                playTDButton.setIcon(finalPlayTDIcon);
                playTDButton.setBounds(605, 240, 320, 60);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                playTDButton.setIcon(finalPlayTDIcon1);
                playTDButton.setBounds(615, 250, 300, 50);
            }
        });

        ImageIcon finalSettingsIcon = settingsIcon2;
        ImageIcon finalSettingsIcon1 = settingsIcon1;
        settingsButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                settingsButton.setIcon(finalSettingsIcon);
                settingsButton.setBounds(605, 440, 320, 60);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                settingsButton.setIcon(finalSettingsIcon1);
                settingsButton.setBounds(615, 450, 300, 50);
            }
        });

        ImageIcon finalQuitGameIcon = quitGameIcon2;
        ImageIcon finalQuitGameIcon1 = quitGameIcon1;
        quitGameButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                quitGameButton.setIcon(finalQuitGameIcon);
                quitGameButton.setBounds(605, 540, 320, 60);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                quitGameButton.setIcon(finalQuitGameIcon1);
                quitGameButton.setBounds(615, 550, 300, 50);
            }
        });

        ImageIcon finalPlayRPGTDIcon = playRPGTDIcon2;
        ImageIcon finalPlayRPGTDIcon1 = playRPGTDIcon1;
        playRPGTDButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                playRPGTDButton.setIcon(finalPlayRPGTDIcon);
                playRPGTDButton.setBounds(605, 340, 320, 60);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                playRPGTDButton.setIcon(finalPlayRPGTDIcon1);
                playRPGTDButton.setBounds(615, 350, 300, 50);
            }
        });

        setVisible(true);

        playRPGButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Game();
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                dispose();
            }
        });

        playTDButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                dispose();
            }
        });

        playRPGTDButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Play RPGTD clicked");
            }
        });

        settingsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Settings clicked");
            }
        });

        quitGameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }
}
