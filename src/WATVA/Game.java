package WATVA;

import javax.swing.*;
import java.awt.*;

public class Game extends JFrame {
    private GamePanel gamePanel;
    private Player player;

    public Game() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (gd.isFullScreenSupported()) {
            setUndecorated(true);
            gd.setFullScreenWindow(this);
        } else {
            System.err.println("Fullscreen not supported");
            setSize(Toolkit.getDefaultToolkit().getScreenSize());
        }
        player = new Player(Player.PANEL_WIDTH, Player.PANEL_HEIGHT, 100);
        gamePanel = new GamePanel(this, player);
        add(gamePanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }
}
