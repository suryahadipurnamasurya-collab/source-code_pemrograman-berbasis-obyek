import javax.swing.*;
import java.io.*;
import java.nio.file.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class SnakeGame extends JFrame {
    public SnakeGame() {
        this.add(new GamePanel());
        this.setTitle("Snake Game Java");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);
        this.pack();
        this.setVisible(true);
        this.setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        new SnakeGame();
    }
}

class GamePanel extends JPanel implements ActionListener {
    // Pengaturan Ukuran Layar
    static final int SCREEN_WIDTH = 600;
    static final int SCREEN_HEIGHT = 600;
    static final int UNIT_SIZE = 25; // Ukuran kotak
    static final int GAME_UNITS = (SCREEN_WIDTH * SCREEN_HEIGHT) / UNIT_SIZE;
    static final int DELAY = 75; // Kecepatan (makin kecil makin cepat)

    // Koordinat Ular dan Apel
    final int x[] = new int[GAME_UNITS];
    final int y[] = new int[GAME_UNITS];
    int bodyParts = 6;
    int applesEaten;
    int appleX, appleY;
    char direction = 'R'; // R: Right, L: Left, U: Up, D: Down
    boolean running = false;
    Timer timer;
    Random random;
    // background animation offset
    private int bgShift = 0;
    // Menu / high score / buttons
    private boolean inMenu = true;
    private int highScore = 0;
    private JButton btnRestart;
    private JButton btnLobby;
    private JButton btnStart;
    // settings UI
    private JComboBox<String> cbWallpaper;
    private JComboBox<String> cbSpeed;
    private JComboBox<String> cbSkin;
    private JSpinner spLength;
    // runtime settings
    private int gameDelay = DELAY;
    private boolean rainbowSkin = false;
    private Color headColor = Color.green;
    private Color bodyColor = new Color(45, 180, 0);

    GamePanel() {
        random = new Random();
        this.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        this.setBackground(Color.black);
        this.setFocusable(true);
        this.addKeyListener(new MyKeyAdapter());
        // don't auto-start: show menu first
        this.setLayout(null);
        createButtons();
        loadHighScore();
    }

    public void startGame() {
        // apply settings from menu
        applySettings();

        // reset game state
        // bodyParts is set in applySettings
        applesEaten = 0;
        direction = 'R';
        // place head in center and body to the left
        x[0] = SCREEN_WIDTH / 2;
        y[0] = SCREEN_HEIGHT / 2;
        for (int i = 1; i < bodyParts; i++) {
            x[i] = x[0] - i * UNIT_SIZE;
            y[i] = y[0];
        }
        newApple();
        running = true;
        inMenu = false;
        setMenuVisible(false);
        if (timer != null) timer.stop();
        timer = new Timer(gameDelay, this);
        timer.start();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        // Enable antialiasing for smoother shapes and border
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Animated gradient background (can be overridden by wallpaper selection)
        float shift = (float) Math.sin(bgShift * 0.03);
        Color c1, c2;
        String wallpaper = (cbWallpaper != null && cbWallpaper.getSelectedItem() != null) ? cbWallpaper.getSelectedItem().toString() : "Default";
        switch (wallpaper) {
            case "Ocean" -> {
                c1 = new Color(10, 30, 60);
                c2 = new Color(0, 80, 120);
            }
            case "Sunset" -> {
                c1 = new Color(80, 10, 30);
                c2 = new Color(220, 100, 40);
            }
            case "Forest" -> {
                c1 = new Color(10, 40, 20);
                c2 = new Color(40, 120, 60);
            }
            case "Space" -> {
                c1 = new Color(5, 5, 20);
                c2 = new Color(60, 10, 80);
            }
            default -> {
                c1 = Color.getHSBColor((bgShift % 360) / 360f, 0.6f, 0.12f);
                c2 = Color.getHSBColor(((bgShift + 90) % 360) / 360f, 0.6f, 0.04f);
            }
        }
        GradientPaint gp = new GradientPaint(0, 0, c1, SCREEN_WIDTH * (0.5f + 0.5f * shift), SCREEN_HEIGHT * (0.5f - 0.5f * shift), c2, true);
        g2.setPaint(gp);
        g2.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        // Subtle moving translucent circles to make background interesting
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.06f));
        for (int i = 0; i < 8; i++) {
            int radius = 80 + (i * 15);
            int cx = (int) ((SCREEN_WIDTH / 2) + Math.cos((bgShift + i * 40) * 0.02) * (SCREEN_WIDTH / 3));
            int cy = (int) ((SCREEN_HEIGHT / 2) + Math.sin((bgShift + i * 25) * 0.015) * (SCREEN_HEIGHT / 3));
            g2.setColor(Color.getHSBColor(((bgShift + i * 30) % 360) / 360f, 0.5f, 0.2f));
            g2.fillOval(cx - radius / 2, cy - radius / 2, radius, radius);
        }
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        // If showing menu
        if (inMenu && !running) {
            // title
            g2.setColor(Color.white);
            g2.setFont(new Font("Ink Free", Font.BOLD, 60));
            FontMetrics fm = getFontMetrics(g2.getFont());
            g2.drawString("SNAKE", (SCREEN_WIDTH - fm.stringWidth("SNAKE")) / 2, SCREEN_HEIGHT / 4);

            // instructions
            g2.setFont(new Font("Ink Free", Font.PLAIN, 20));
            FontMetrics fm2 = getFontMetrics(g2.getFont());
            String instr = "Klik tombol 'Mulai' untuk bermain";
            g2.drawString(instr, (SCREEN_WIDTH - fm2.stringWidth(instr)) / 2, SCREEN_HEIGHT / 4 + 40);

            // show high score
            g2.setFont(new Font("Ink Free", Font.BOLD, 28));
            g2.drawString("High Score: " + highScore, (SCREEN_WIDTH - getFontMetrics(g2.getFont()).stringWidth("High Score: " + highScore)) / 2, SCREEN_HEIGHT / 4 + 90);
            return;
        }

        if (running) {
            // Gambar Apel (ke atas dari background)
            g2.setColor(Color.red);
            g2.fillOval(appleX, appleY, UNIT_SIZE, UNIT_SIZE);

            // Gambar Ular dengan sudut membulat
            for (int i = 0; i < bodyParts; i++) {
                if (i == 0) {
                    // Kepala dengan warna sesuai skin
                    if (rainbowSkin) {
                        headColor = Color.getHSBColor(((bgShift + i * 20) % 360) / 360f, 0.9f, 0.8f);
                    }
                    GradientPaint headPaint = new GradientPaint(x[i], y[i], headColor.brighter(), x[i] + UNIT_SIZE, y[i] + UNIT_SIZE, headColor.darker());
                    g2.setPaint(headPaint);
                } else {
                    if (rainbowSkin) {
                        g2.setColor(Color.getHSBColor(((bgShift + i * 30) % 360) / 360f, 0.85f, 0.7f));
                    } else {
                        g2.setColor(bodyColor);
                    }
                }
                // Rounded segments for a more organic look
                g2.fillRoundRect(x[i], y[i], UNIT_SIZE, UNIT_SIZE, UNIT_SIZE / 2, UNIT_SIZE / 2);
                // Slight darker outline for each segment
                g2.setColor(new Color(0, 0, 0, 80));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(x[i], y[i], UNIT_SIZE, UNIT_SIZE, UNIT_SIZE / 2, UNIT_SIZE / 2);
            }

            // Smooth rounded border around the play area
            g2.setStroke(new BasicStroke(6f));
            GradientPaint borderPaint = new GradientPaint(0, 0, new Color(200, 200, 255, 180), 0, SCREEN_HEIGHT, new Color(120, 120, 200, 180));
            g2.setPaint(borderPaint);
            int inset = 3;
            g2.drawRoundRect(inset, inset, SCREEN_WIDTH - inset * 2, SCREEN_HEIGHT - inset * 2, 40, 40);

            // Skor
            g2.setColor(Color.white);
            g2.setFont(new Font("Ink Free", Font.BOLD, 36));
            FontMetrics metrics = getFontMetrics(g2.getFont());
            g2.drawString("Score: " + applesEaten, (SCREEN_WIDTH - metrics.stringWidth("Score: " + applesEaten)) / 2, g2.getFont().getSize() + 6);
        } else {
            gameOver(g2);
        }

        // advance background animation
        bgShift += 2;
    }

    public void newApple() {
        appleX = random.nextInt((int) (SCREEN_WIDTH / UNIT_SIZE)) * UNIT_SIZE;
        appleY = random.nextInt((int) (SCREEN_HEIGHT / UNIT_SIZE)) * UNIT_SIZE;
    }

    public void move() {
        for (int i = bodyParts; i > 0; i--) {
            x[i] = x[i - 1];
            y[i] = y[i - 1];
        }

        switch (direction) {
            case 'U' -> y[0] = y[0] - UNIT_SIZE;
            case 'D' -> y[0] = y[0] + UNIT_SIZE;
            case 'L' -> x[0] = x[0] - UNIT_SIZE;
            case 'R' -> x[0] = x[0] + UNIT_SIZE;
        }
    }

    public void checkApple() {
        if ((x[0] == appleX) && (y[0] == appleY)) {
            bodyParts++;
            applesEaten++;
            newApple();
        }
    }

    public void checkCollisions() {
        // Cek jika kepala menabrak badan
        for (int i = bodyParts; i > 0; i--) {
            if ((x[0] == x[i]) && (y[0] == y[i])) {
                running = false;
            }
        }
        // Cek jika kepala menabrak dinding
        if (x[0] < 0 || x[0] >= SCREEN_WIDTH || y[0] < 0 || y[0] >= SCREEN_HEIGHT) {
            running = false;
        }

        if (!running) {
            timer.stop();
            onGameOver();
        }
    }

    private void onGameOver() {
        inMenu = false;
        // update high score
        if (applesEaten > highScore) {
            highScore = applesEaten;
            saveHighScore();
        }
        // show buttons
        btnRestart.setVisible(true);
        btnLobby.setVisible(true);
    }

    public void gameOver(Graphics g) {
        g.setColor(Color.red);
        g.setFont(new Font("Ink Free", Font.BOLD, 75));
        FontMetrics metrics = getFontMetrics(g.getFont());
        g.drawString("Game Over", (SCREEN_WIDTH - metrics.stringWidth("Game Over")) / 2, SCREEN_HEIGHT / 2);
        // show score and high score
        g.setColor(Color.white);
        g.setFont(new Font("Ink Free", Font.BOLD, 30));
        FontMetrics m2 = getFontMetrics(g.getFont());
        g.drawString("Score: " + applesEaten, (SCREEN_WIDTH - m2.stringWidth("Score: " + applesEaten)) / 2, SCREEN_HEIGHT / 2 + 40);
        g.drawString("High Score: " + highScore, (SCREEN_WIDTH - m2.stringWidth("High Score: " + highScore)) / 2, SCREEN_HEIGHT / 2 + 80);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (running) {
            move();
            checkApple();
            checkCollisions();
        }
        repaint();
    }

    public class MyKeyAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            // SPACE to start/restart from menu
            // start via button, keyboard SPACE disabled
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT -> { if (direction != 'R') direction = 'L'; }
                case KeyEvent.VK_RIGHT -> { if (direction != 'L') direction = 'R'; }
                case KeyEvent.VK_UP -> { if (direction != 'D') direction = 'U'; }
                case KeyEvent.VK_DOWN -> { if (direction != 'U') direction = 'D'; }
            }
        }
    }

    // create buttons and file helpers
    private void createButtons() {
        btnRestart = new JButton("Ulang Game");
        btnRestart.setFocusable(false);
        btnRestart.setBounds(SCREEN_WIDTH/2 - 150, SCREEN_HEIGHT/2 + 140, 140, 44);
        btnRestart.setVisible(false);
        btnRestart.addActionListener(e -> {
            startGame();
        });

        btnLobby = new JButton("Kembali Lobby");
        btnLobby.setFocusable(false);
        btnLobby.setBounds(SCREEN_WIDTH/2 + 10, SCREEN_HEIGHT/2 + 140, 140, 44);
        btnLobby.setVisible(false);
        btnLobby.addActionListener(e -> {
            // return to menu
            inMenu = true;
            running = false;
            btnRestart.setVisible(false);
            btnLobby.setVisible(false);
            setMenuVisible(true);
            repaint();
        });

        this.add(btnRestart);
        this.add(btnLobby);

        // Settings controls shown in menu
        String[] wallpapers = {"Default", "Ocean", "Sunset", "Forest", "Space"};
        cbWallpaper = new JComboBox<>(wallpapers);
        cbWallpaper.setBounds(60, SCREEN_HEIGHT/4 + 120, 140, 28);
        this.add(cbWallpaper);

        SpinnerModel model = new SpinnerNumberModel(6, 1, 10, 1);
        spLength = new JSpinner(model);
        spLength.setBounds(220, SCREEN_HEIGHT/4 + 120, 60, 28);
        this.add(spLength);

        String[] speeds = {"Easy", "Medium", "Hard"};
        cbSpeed = new JComboBox<>(speeds);
        cbSpeed.setBounds(300, SCREEN_HEIGHT/4 + 120, 100, 28);
        cbSpeed.setSelectedIndex(1);
        this.add(cbSpeed);

        String[] skins = {"Default","Merah","Kuning","Hijau","Biru","Ungu","Warna Warni"};
        cbSkin = new JComboBox<>(skins);
        cbSkin.setBounds(420, SCREEN_HEIGHT/4 + 120, 150, 28);
        this.add(cbSkin);

        setMenuVisible(true);
        // Start button in menu
        btnStart = new JButton("Mulai");
        btnStart.setFocusable(false);
        btnStart.setBounds(SCREEN_WIDTH/2 - 70, SCREEN_HEIGHT/4 + 60, 140, 44);
        btnStart.addActionListener(e -> startGame());
        this.add(btnStart);
    }

    private void setMenuVisible(boolean visible) {
        if (cbWallpaper != null) cbWallpaper.setVisible(visible);
        if (cbSpeed != null) cbSpeed.setVisible(visible);
        if (cbSkin != null) cbSkin.setVisible(visible);
        if (spLength != null) spLength.setVisible(visible);
        if (btnStart != null) btnStart.setVisible(visible);
    }

    private void applySettings() {
        // length
        int len = 6;
        try { len = (Integer) spLength.getValue(); } catch (Exception ignored) {}
        bodyParts = Math.max(1, Math.min(10, len));

        // speed
        String speed = (cbSpeed != null && cbSpeed.getSelectedItem() != null) ? cbSpeed.getSelectedItem().toString() : "Medium";
        switch (speed) {
            case "Easy" -> gameDelay = 120;
            case "Hard" -> gameDelay = 45;
            default -> gameDelay = 75; // Medium
        }

        // skin
        String skin = (cbSkin != null && cbSkin.getSelectedItem() != null) ? cbSkin.getSelectedItem().toString() : "Default";
        rainbowSkin = false;
        switch (skin) {
            case "Merah" -> { headColor = new Color(220,30,30); bodyColor = new Color(140,10,10); }
            case "Kuning" -> { headColor = new Color(240,200,20); bodyColor = new Color(180,150,10); }
            case "Hijau" -> { headColor = new Color(60,200,80); bodyColor = new Color(20,120,40); }
            case "Biru" -> { headColor = new Color(60,140,220); bodyColor = new Color(10,70,160); }
            case "Ungu" -> { headColor = new Color(180,80,200); bodyColor = new Color(120,40,160); }
            case "Warna Warni" -> { rainbowSkin = true; }
            default -> { headColor = Color.green; bodyColor = new Color(45,180,0); }
        }
    }

    private void loadHighScore() {
        Path p = Paths.get("highscore.txt");
        if (Files.exists(p)) {
            try {
                String s = new String(Files.readAllBytes(p)).trim();
                highScore = Integer.parseInt(s);
            } catch (Exception ex) {
                highScore = 0;
            }
        }
    }

    private void saveHighScore() {
        try {
            Files.write(Paths.get("highscore.txt"), Integer.toString(highScore).getBytes());
        } catch (IOException ex) {
            // ignore
        }
    }
}