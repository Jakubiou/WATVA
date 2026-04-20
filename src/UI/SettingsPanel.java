package UI;

import Core.Game;
import Soundtrack.Soundtrack;
import UI.Game.GamePanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Full settings panel with:
 *  - Volume control
 *  - Keybind remapping (WASD/arrows, Dash, Explosion)
 *  - Display options (FPS display toggle)
 *  - Reset to defaults
 */
public class SettingsPanel extends JPanel {

    public static final String PREF_KEY_UP       = "key_up";
    public static final String PREF_KEY_DOWN     = "key_down";
    public static final String PREF_KEY_LEFT     = "key_left";
    public static final String PREF_KEY_RIGHT    = "key_right";
    public static final String PREF_KEY_DASH     = "key_dash";
    public static final String PREF_KEY_EXPLODE  = "key_explode";
    public static final String PREF_VOLUME       = "volume";
    public static final String PREF_SHOW_FPS     = "show_fps";

    private static final Preferences PREFS = Preferences.userNodeForPackage(SettingsPanel.class);

    public static final int DEFAULT_KEY_UP      = KeyEvent.VK_W;
    public static final int DEFAULT_KEY_DOWN    = KeyEvent.VK_S;
    public static final int DEFAULT_KEY_LEFT    = KeyEvent.VK_A;
    public static final int DEFAULT_KEY_RIGHT   = KeyEvent.VK_D;
    public static final int DEFAULT_KEY_DASH    = KeyEvent.VK_SHIFT;
    public static final int DEFAULT_KEY_EXPLODE = KeyEvent.VK_Q;

    private final Soundtrack soundtrack;
    private final MenuPanel menuPanel;
    private Font pixelFont;

    private int keyUp, keyDown, keyLeft, keyRight, keyDash, keyExplode;

    private JSlider volumeSlider;
    private JLabel  volumeValueLabel;
    private JCheckBox showFpsBox;
    private Map<String, JButton> keybindButtons = new HashMap<>();
    private String awaitingRebind = null;

    private static final Color BG        = new Color(20, 20, 30, 240);
    private static final Color ACCENT    = new Color(255, 160, 0);
    private static final Color TEXT      = new Color(220, 220, 220);
    private static final Color HIGHLIGHT = new Color(60, 140, 255);

    public SettingsPanel(Soundtrack soundtrack, MenuPanel menuPanel) {
        this.soundtrack = soundtrack;
        this.menuPanel  = menuPanel;
        loadPrefs();
        initFont();
        buildUI();
    }

    private void loadPrefs() {
        keyUp      = PREFS.getInt(PREF_KEY_UP,      DEFAULT_KEY_UP);
        keyDown    = PREFS.getInt(PREF_KEY_DOWN,    DEFAULT_KEY_DOWN);
        keyLeft    = PREFS.getInt(PREF_KEY_LEFT,    DEFAULT_KEY_LEFT);
        keyRight   = PREFS.getInt(PREF_KEY_RIGHT,   DEFAULT_KEY_RIGHT);
        keyDash    = PREFS.getInt(PREF_KEY_DASH,    DEFAULT_KEY_DASH);
        keyExplode = PREFS.getInt(PREF_KEY_EXPLODE, DEFAULT_KEY_EXPLODE);
    }

    private void savePrefs() {
        PREFS.putInt(PREF_KEY_UP,      keyUp);
        PREFS.putInt(PREF_KEY_DOWN,    keyDown);
        PREFS.putInt(PREF_KEY_LEFT,    keyLeft);
        PREFS.putInt(PREF_KEY_RIGHT,   keyRight);
        PREFS.putInt(PREF_KEY_DASH,    keyDash);
        PREFS.putInt(PREF_KEY_EXPLODE, keyExplode);
        if (volumeSlider != null)
            PREFS.putFloat(PREF_VOLUME, volumeSlider.getValue() / 100f);
        if (showFpsBox != null)
            PREFS.putBoolean(PREF_SHOW_FPS, showFpsBox.isSelected());
    }

    /** Static helpers for other classes to read saved keybinds */
    public static int getSavedKey(String prefKey, int defaultKey) {
        return PREFS.getInt(prefKey, defaultKey);
    }
    public static boolean isShowFps() {
        return PREFS.getBoolean(PREF_SHOW_FPS, false);
    }

    private void initFont() {
        try {
            pixelFont = Font.createFont(Font.TRUETYPE_FONT,
                            getClass().getResourceAsStream("/fonts/PixelPurl.ttf"))
                    .deriveFont((float) Game.scale(16));
        } catch (Exception e) {
            pixelFont = new Font("Arial", Font.BOLD, Game.scale(16));
        }
    }

    private void buildUI() {
        setOpaque(false);
        setLayout(null);

        int pw = (int)(Game.getRealScreenWidth()  * 0.55);
        int ph = (int)(Game.getRealScreenHeight() * 0.80);
        int px = (GamePanel.PANEL_WIDTH  - pw) / 2;
        int py = (GamePanel.PANEL_HEIGHT - ph) / 2;
        setBounds(px, py, pw, ph);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(Game.scale(20), Game.scale(30), Game.scale(20), Game.scale(30)));

        content.add(centeredLabel("SETTINGS", Game.scale(28), ACCENT));
        content.add(vgap(Game.scale(16)));

        content.add(sectionLabel("AUDIO"));
        content.add(vgap(Game.scale(8)));

        JPanel volRow = new JPanel(new FlowLayout(FlowLayout.LEFT, Game.scale(12), 0));
        volRow.setOpaque(false);
        JLabel volLabel = styledLabel("Volume:", Game.scale(15));
        volumeValueLabel = styledLabel(PREFS.getInt("vol_pct", 0) + "%", Game.scale(15));
        volumeValueLabel.setForeground(HIGHLIGHT);
        volumeSlider = new JSlider(0, 100, PREFS.getInt("vol_pct", 0));
        volumeSlider.setPreferredSize(new Dimension(Game.scale(220), Game.scale(30)));
        volumeSlider.setOpaque(false);
        volumeSlider.setForeground(TEXT);
        volumeSlider.addChangeListener(e -> {
            int v = volumeSlider.getValue();
            volumeValueLabel.setText(v + "%");
            PREFS.putInt("vol_pct", v);
            if (soundtrack != null) soundtrack.setVolume(v / 100f);
        });
        volRow.add(volLabel);
        volRow.add(volumeSlider);
        volRow.add(volumeValueLabel);
        content.add(volRow);
        content.add(vgap(Game.scale(20)));

        content.add(sectionLabel("DISPLAY"));
        content.add(vgap(Game.scale(8)));

        JPanel fpsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, Game.scale(12), 0));
        fpsRow.setOpaque(false);
        showFpsBox = new JCheckBox("Show FPS counter", PREFS.getBoolean(PREF_SHOW_FPS, false));
        showFpsBox.setFont(pixelFont);
        showFpsBox.setForeground(TEXT);
        showFpsBox.setOpaque(false);
        showFpsBox.addActionListener(e -> savePrefs());
        fpsRow.add(showFpsBox);
        content.add(fpsRow);
        content.add(vgap(Game.scale(20)));

        content.add(sectionLabel("KEYBINDS"));
        content.add(vgap(Game.scale(8)));

        String[][] binds = {
                { "Move Up",    PREF_KEY_UP,      String.valueOf(keyUp)      },
                { "Move Down",  PREF_KEY_DOWN,    String.valueOf(keyDown)    },
                { "Move Left",  PREF_KEY_LEFT,    String.valueOf(keyLeft)    },
                { "Move Right", PREF_KEY_RIGHT,   String.valueOf(keyRight)   },
                { "Dash",       PREF_KEY_DASH,    String.valueOf(keyDash)    },
                { "Explosion",  PREF_KEY_EXPLODE, String.valueOf(keyExplode) },
        };

        for (String[] bind : binds) {
            content.add(buildKeybindRow(bind[0], bind[1]));
            content.add(vgap(Game.scale(6)));
        }
        content.add(vgap(Game.scale(20)));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, Game.scale(16), 0));
        btnRow.setOpaque(false);
        btnRow.add(makeButton("RESET DEFAULTS", new Color(120, 40, 40), e -> resetDefaults()));
        btnRow.add(makeButton("<- BACK",        new Color(40, 80, 40),  e -> goBack()));
        content.add(btnRow);

        JScrollPane scroll = new JScrollPane(content,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.setBounds(0, 0, pw, ph);
        scroll.getVerticalScrollBar().setUnitIncrement(Game.scale(16));
        add(scroll);

        setVisible(false);
    }

    private JPanel buildKeybindRow(String actionName, String prefKey) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, Game.scale(12), 0));
        row.setOpaque(false);
        JLabel lbl = styledLabel(actionName + ":", Game.scale(14));
        lbl.setPreferredSize(new Dimension(Game.scale(120), Game.scale(28)));

        JButton btn = new JButton(keyName(currentKey(prefKey)));
        btn.setFont(pixelFont.deriveFont((float) Game.scale(13)));
        btn.setForeground(TEXT);
        btn.setBackground(new Color(40, 40, 60));
        btn.setBorder(BorderFactory.createLineBorder(ACCENT, 1));
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(Game.scale(130), Game.scale(28)));
        btn.addActionListener(e -> startRebind(prefKey, btn));
        keybindButtons.put(prefKey, btn);
        row.add(lbl);
        row.add(btn);
        return row;
    }

    private void startRebind(String prefKey, JButton btn) {
        if (awaitingRebind != null) return;
        awaitingRebind = prefKey;
        btn.setText("Press a key...");
        btn.setBackground(new Color(60, 60, 20));

        KeyEventDispatcher dispatcher = new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (e.getID() != KeyEvent.KEY_PRESSED) return false;
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_ESCAPE) {
                    cancelRebind(btn, prefKey);
                } else {
                    applyRebind(prefKey, code, btn);
                }
                KeyboardFocusManager.getCurrentKeyboardFocusManager()
                        .removeKeyEventDispatcher(this);
                return true;
            }
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(dispatcher);
    }

    private void applyRebind(String prefKey, int code, JButton btn) {
        switch (prefKey) {
            case PREF_KEY_UP      -> keyUp      = code;
            case PREF_KEY_DOWN    -> keyDown    = code;
            case PREF_KEY_LEFT    -> keyLeft    = code;
            case PREF_KEY_RIGHT   -> keyRight   = code;
            case PREF_KEY_DASH    -> keyDash    = code;
            case PREF_KEY_EXPLODE -> keyExplode = code;
        }
        btn.setText(keyName(code));
        btn.setBackground(new Color(40, 40, 60));
        awaitingRebind = null;
        savePrefs();
    }

    private void cancelRebind(JButton btn, String prefKey) {
        btn.setText(keyName(currentKey(prefKey)));
        btn.setBackground(new Color(40, 40, 60));
        awaitingRebind = null;
    }

    private int currentKey(String prefKey) {
        return switch (prefKey) {
            case PREF_KEY_UP      -> keyUp;
            case PREF_KEY_DOWN    -> keyDown;
            case PREF_KEY_LEFT    -> keyLeft;
            case PREF_KEY_RIGHT   -> keyRight;
            case PREF_KEY_DASH    -> keyDash;
            case PREF_KEY_EXPLODE -> keyExplode;
            default -> 0;
        };
    }

    private String keyName(int code) {
        if (code == KeyEvent.VK_SHIFT)   return "SHIFT";
        if (code == KeyEvent.VK_CONTROL) return "CTRL";
        if (code == KeyEvent.VK_ALT)     return "ALT";
        if (code == KeyEvent.VK_SPACE)   return "SPACE";
        String s = KeyEvent.getKeyText(code);
        return s.length() == 1 ? s.toUpperCase() : s;
    }

    private void resetDefaults() {
        keyUp      = DEFAULT_KEY_UP;
        keyDown    = DEFAULT_KEY_DOWN;
        keyLeft    = DEFAULT_KEY_LEFT;
        keyRight   = DEFAULT_KEY_RIGHT;
        keyDash    = DEFAULT_KEY_DASH;
        keyExplode = DEFAULT_KEY_EXPLODE;
        volumeSlider.setValue(50);
        showFpsBox.setSelected(false);
        refreshKeybindLabels();
        savePrefs();
        if (soundtrack != null) soundtrack.setVolume(0.5f);
    }

    private void refreshKeybindLabels() {
        if (keybindButtons.isEmpty()) return;
        updateBtn(PREF_KEY_UP,      keyUp);
        updateBtn(PREF_KEY_DOWN,    keyDown);
        updateBtn(PREF_KEY_LEFT,    keyLeft);
        updateBtn(PREF_KEY_RIGHT,   keyRight);
        updateBtn(PREF_KEY_DASH,    keyDash);
        updateBtn(PREF_KEY_EXPLODE, keyExplode);
    }

    private void updateBtn(String key, int code) {
        JButton b = keybindButtons.get(key);
        if (b != null) b.setText(keyName(code));
    }

    private void goBack() {
        savePrefs();
        setVisible(false);
        if (menuPanel != null) menuPanel.setVisible(true);
    }

    private JLabel centeredLabel(String text, int size, Color color) {
        JLabel l = new JLabel(text, JLabel.CENTER);
        l.setFont(pixelFont.deriveFont(Font.BOLD, (float) size));
        l.setForeground(color);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(pixelFont.deriveFont(Font.BOLD, (float) Game.scale(16)));
        l.setForeground(ACCENT);
        l.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 80)));
        return l;
    }

    private JLabel styledLabel(String text, int size) {
        JLabel l = new JLabel(text);
        l.setFont(pixelFont.deriveFont((float) size));
        l.setForeground(TEXT);
        return l;
    }

    private Component vgap(int h) {
        return Box.createVerticalStrut(h);
    }

    private JButton makeButton(String text, Color bg, ActionListener al) {
        JButton b = new JButton(text);
        b.setFont(pixelFont.deriveFont(Font.BOLD, (float) Game.scale(15)));
        b.setForeground(Color.WHITE);
        b.setBackground(bg);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.brighter(), 1),
                BorderFactory.createEmptyBorder(Game.scale(6), Game.scale(18), Game.scale(6), Game.scale(18))
        ));
        b.addActionListener(al);
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(bg.brighter()); }
            public void mouseExited(MouseEvent e)  { b.setBackground(bg); }
        });
        return b;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(BG);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), Game.scale(20), Game.scale(20));
        g2.setColor(ACCENT);
        g2.setStroke(new BasicStroke(Game.scale(2)));
        g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, Game.scale(20), Game.scale(20));
    }

    public int getKeyUp()      { return keyUp; }
    public int getKeyDown()    { return keyDown; }
    public int getKeyLeft()    { return keyLeft; }
    public int getKeyRight()   { return keyRight; }
    public int getKeyDash()    { return keyDash; }
    public int getKeyExplode() { return keyExplode; }
}