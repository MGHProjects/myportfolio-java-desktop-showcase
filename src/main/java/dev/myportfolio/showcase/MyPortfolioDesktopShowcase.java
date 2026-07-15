package dev.myportfolio.showcase;

import javax.swing.BorderFactory;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.time.Clock;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

public final class MyPortfolioDesktopShowcase {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            WorkspaceFrame frame = new WorkspaceFrame(new ShowcaseModel(0x4d50524f4f464cL, Clock.systemDefaultZone()));
            frame.setVisible(true);
        });
    }

    private MyPortfolioDesktopShowcase() {
    }
}

record ShowcaseSnapshot(int health, int velocity, String grade, int confidence, String time) {
}

final class ShowcaseModel {
    private final Random random;
    private final Clock clock;
    private int tick;

    ShowcaseModel(long seed, Clock clock) {
        this.random = new Random(seed);
        this.clock = clock;
    }

    ShowcaseSnapshot next() {
        tick++;
        return new ShowcaseSnapshot(
            94 + random.nextInt(6),
            19 + (tick % 9),
            tick % 5 == 0 ? "A+" : "A",
            58 + random.nextInt(39),
            LocalTime.now(clock).format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        );
    }
}

final class WorkspaceFrame extends JFrame {
    static final String CLIPBOARD_TOKEN = "proof://myportfolio/desktop-fixture";
    private final ShowcaseModel model;
    private final MetricCard healthCard;
    private final MetricCard velocityCard;
    private final MetricCard focusCard;
    private final LineChartPanel chartPanel;
    private final JLabel clockLabel;
    private final JLabel environmentLabel;
    private final Timer liveTimer;
    private boolean expandedScale;

    WorkspaceFrame(ShowcaseModel model) {
        super("MyPortfolio Desktop Showcase");
        this.model = model;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1040, 700));
        setSize(1180, 760);
        setLocationRelativeTo(null);

        RootPanel root = new RootPanel();
        root.setLayout(new BorderLayout(0, 0));
        root.add(new SidebarPanel(), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JPanel header = new JPanel(new BorderLayout(18, 0));
        header.setOpaque(false);
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel title = label("Talent Workspace", 30, Font.BOLD, Palette.INK);
        JLabel subtitle = label("Live evidence dashboard for executable portfolios", 14, Font.PLAIN, Palette.MUTED);
        heading.add(title);
        heading.add(Box.createVerticalStrut(6));
        heading.add(subtitle);
        header.add(heading, BorderLayout.CENTER);

        clockLabel = pill("Starting");
        environmentLabel = label("Detecting desktop", 11, Font.PLAIN, Palette.MUTED);
        environmentLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        JPanel runtimeStatus = new JPanel();
        runtimeStatus.setOpaque(false);
        runtimeStatus.setLayout(new BoxLayout(runtimeStatus, BoxLayout.Y_AXIS));
        runtimeStatus.add(clockLabel);
        runtimeStatus.add(Box.createVerticalStrut(5));
        runtimeStatus.add(environmentLabel);
        header.add(runtimeStatus, BorderLayout.EAST);
        content.add(header, BorderLayout.NORTH);

        JPanel metrics = new JPanel(new GridLayout(1, 3, 14, 0));
        metrics.setOpaque(false);
        healthCard = new MetricCard("Sandbox health", "98%", "stable stream");
        velocityCard = new MetricCard("Delivery pulse", "24", "changes shipped");
        focusCard = new MetricCard("Review signal", "A", "portfolio ready");
        metrics.add(healthCard);
        metrics.add(velocityCard);
        metrics.add(focusCard);

        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        center.add(metrics, BorderLayout.NORTH);

        JPanel workArea = new JPanel(new GridLayout(1, 2, 14, 0));
        workArea.setOpaque(false);
        chartPanel = new LineChartPanel();
        workArea.add(chartPanel);
        workArea.add(new ActivityPanel(this));
        center.add(workArea, BorderLayout.CENTER);

        JPanel footer = new JPanel(new GridLayout(1, 3, 14, 0));
        footer.setOpaque(false);
        footer.add(new ProjectTile("API Sandbox", "WebSocket lifecycle hardened", 92));
        footer.add(new ProjectTile("Desktop Agent", "Rust capture and XTest input", 86));
        footer.add(new ProjectTile("Talent Proof", "Executable work, not static cards", 78));
        center.add(footer, BorderLayout.SOUTH);

        content.add(center, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);

        installKeyboardActions();
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                updateEnvironmentStatus("resize");
            }
        });

        liveTimer = new Timer(900, this::updateLiveMetrics);
        liveTimer.start();
        updateLiveMetrics(null);
    }

    private void updateLiveMetrics(ActionEvent event) {
        ShowcaseSnapshot snapshot = model.next();
        healthCard.setValue(snapshot.health() + "%", snapshot.health() > 96 ? "low latency" : "stable stream");
        velocityCard.setValue(String.valueOf(snapshot.velocity()), "changes shipped");
        focusCard.setValue(snapshot.grade(), "portfolio ready");
        chartPanel.push(snapshot.confidence());
        clockLabel.setText(snapshot.time());
        updateEnvironmentStatus("live");
    }

    void showProtocolDialog() {
        JOptionPane.showMessageDialog(
            this,
            "MDP/1 dialog input confirmed. Press Escape or Enter to continue.",
            "Desktop dialog probe",
            JOptionPane.INFORMATION_MESSAGE
        );
        updateEnvironmentStatus("dialog");
    }

    void openInspectorWindow() {
        JFrame inspector = new JFrame("Evidence Inspector");
        inspector.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JTextArea details = new JTextArea(
            "Fixture window: secondary\n"
                + "Purpose: verify focus and window switching\n"
                + "Protocol: MDP/1"
        );
        details.setEditable(false);
        details.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        details.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        inspector.setContentPane(new JScrollPane(details));
        inspector.setSize(460, 260);
        inspector.setLocation(getX() + 90, getY() + 90);
        inspector.setVisible(true);
        updateEnvironmentStatus("window");
    }

    void copyFixtureToken() {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(CLIPBOARD_TOKEN), null);
        updateEnvironmentStatus("clipboard");
    }

    void toggleScale() {
        expandedScale = !expandedScale;
        setSize(expandedScale ? new Dimension(1280, 820) : new Dimension(1040, 700));
        updateEnvironmentStatus("scale");
    }

    void simulateCrash() {
        int choice = JOptionPane.showConfirmDialog(
            this,
            "Exit with status 42 to verify crash capture?",
            "Controlled crash probe",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (choice == JOptionPane.YES_OPTION) {
            System.exit(42);
        }
    }

    void closeCleanly() {
        dispatchEvent(new java.awt.event.WindowEvent(this, java.awt.event.WindowEvent.WINDOW_CLOSING));
    }

    void updateEnvironmentStatus(String event) {
        int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
        environmentLabel.setText(getWidth() + "x" + getHeight() + " at " + dpi + " DPI | " + event);
    }

    private void installKeyboardActions() {
        bindKey("dialog", KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK), this::showProtocolDialog);
        bindKey("window", KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), this::openInspectorWindow);
        bindKey(
            "clipboard",
            KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
            this::copyFixtureToken
        );
    }

    private void bindKey(String name, KeyStroke keyStroke, Runnable action) {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, name);
        getRootPane().getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                action.run();
            }
        });
    }

    @Override
    public void dispose() {
        liveTimer.stop();
        for (Window window : Window.getWindows()) {
            if (window != this && window.isDisplayable()) {
                window.dispose();
            }
        }
        super.dispose();
    }

    static JLabel label(String text, int size, int style, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", style, size));
        label.setForeground(color);
        return label;
    }

    static JLabel pill(String text) {
        JLabel label = label(text, 13, Font.BOLD, Palette.BLUE);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        label.setOpaque(true);
        label.setBackground(new Color(232, 241, 255));
        return label;
    }
}

final class SidebarPanel extends RoundedPanel {
    SidebarPanel() {
        super(0, new Color(17, 24, 39), new Color(23, 37, 84));
        setPreferredSize(new Dimension(260, 0));
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(26, 22, 26, 22));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        JLabel brand = WorkspaceFrame.label("MyPortfolio", 26, Font.BOLD, Color.WHITE);
        JLabel caption = WorkspaceFrame.label("Desktop Lab", 13, Font.PLAIN, new Color(191, 219, 254));
        top.add(brand);
        top.add(Box.createVerticalStrut(4));
        top.add(caption);
        top.add(Box.createVerticalStrut(30));

        top.add(nav("Overview", true));
        top.add(nav("Candidates", false));
        top.add(nav("Evidence", false));
        top.add(nav("Reviews", false));
        top.add(nav("Settings", false));
        add(top, BorderLayout.NORTH);

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        JLabel score = WorkspaceFrame.label("Sandbox Ready", 15, Font.BOLD, Color.WHITE);
        JLabel copy = WorkspaceFrame.label("<html>Minimize this app,<br>then restore it from<br>MyPortfolio Desktop.</html>", 12, Font.PLAIN, new Color(203, 213, 225));
        copy.setMaximumSize(new Dimension(190, 62));
        bottom.add(score);
        bottom.add(Box.createVerticalStrut(8));
        bottom.add(copy);
        bottom.add(Box.createVerticalStrut(18));
        JButton action = new GradientButton("Run review");
        bottom.add(action);
        add(bottom, BorderLayout.SOUTH);
    }

    private JComponent nav(String label, boolean active) {
        JLabel item = WorkspaceFrame.label(label, 14, active ? Font.BOLD : Font.PLAIN, active ? Color.WHITE : new Color(203, 213, 225));
        item.setOpaque(true);
        item.setBackground(active ? new Color(37, 99, 235) : new Color(0, 0, 0, 0));
        item.setBorder(BorderFactory.createEmptyBorder(11, 14, 11, 14));
        item.setMaximumSize(new Dimension(210, 42));
        return item;
    }
}

final class ActivityPanel extends RoundedPanel {
    ActivityPanel(WorkspaceFrame owner) {
        super(22, Color.WHITE, new Color(248, 250, 252));
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(WorkspaceFrame.label("Review stream", 20, Font.BOLD, Palette.INK), BorderLayout.WEST);
        header.add(WorkspaceFrame.pill("Live"), BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.add(new ActivityRow("Runtime detected", "Java Desktop App", Palette.GREEN));
        rows.add(new ActivityRow("Desktop protocol", "MDP/1 at 144 fps target", Palette.BLUE));
        rows.add(new ActivityRow("Input channel", "Mouse, keyboard and restore actions", Palette.PINK));
        rows.add(new ActivityRow("Candidate signal", "Executable proof available", Palette.ORANGE));
        add(rows, BorderLayout.CENTER);
        add(new DesktopProbePanel(owner), BorderLayout.SOUTH);
    }
}

final class DesktopProbePanel extends JPanel {
    DesktopProbePanel(WorkspaceFrame owner) {
        setOpaque(false);
        setLayout(new BorderLayout(0, 8));

        JTextField keyboardProbe = new JTextField("Type here, then use Ctrl+C / Ctrl+V");
        keyboardProbe.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        keyboardProbe.setToolTipText("Keyboard and clipboard input probe");
        keyboardProbe.getAccessibleContext().setAccessibleName("Keyboard input probe");
        add(keyboardProbe, BorderLayout.NORTH);

        MouseProbePanel mouseProbe = new MouseProbePanel(owner);
        add(mouseProbe, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(3, 2, 6, 6));
        actions.setOpaque(false);
        actions.add(actionButton("Dialog", owner::showProtocolDialog));
        actions.add(actionButton("New window", owner::openInspectorWindow));
        actions.add(actionButton("Copy token", owner::copyFixtureToken));
        actions.add(actionButton("Resize", owner::toggleScale));
        actions.add(actionButton("Crash 42", owner::simulateCrash));
        actions.add(actionButton("Clean close", owner::closeCleanly));
        add(actions, BorderLayout.SOUTH);
    }

    private JButton actionButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        button.setFocusPainted(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.getAccessibleContext().setAccessibleName(text + " desktop probe");
        button.addActionListener(event -> action.run());
        return button;
    }
}

final class MouseProbePanel extends JPanel {
    private int interactions;
    private int pointerX;
    private int pointerY;

    MouseProbePanel(WorkspaceFrame owner) {
        setOpaque(false);
        setFocusable(true);
        setToolTipText("Click, drag, or focus this surface to verify pointer input");
        getAccessibleContext().setAccessibleName("Mouse and drag input probe");
        setPreferredSize(new Dimension(280, 68));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                requestFocusInWindow();
                record(event, owner, "mouse");
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent event) {
                record(event, owner, "drag");
            }
        });
    }

    private void record(MouseEvent event, WorkspaceFrame owner, String kind) {
        interactions++;
        pointerX = event.getX();
        pointerY = event.getY();
        owner.updateEnvironmentStatus(kind);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(239, 246, 255));
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
        g2.setColor(new Color(147, 197, 253));
        g2.drawRoundRect(0, 0, Math.max(0, getWidth() - 1), Math.max(0, getHeight() - 1), 10, 10);
        g2.setColor(Palette.INK);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2.drawString("Pointer probe", 10, 21);
        g2.setColor(Palette.MUTED);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        String detail = interactions == 0
            ? "Click or drag inside this area"
            : interactions + " events at " + pointerX + "," + pointerY;
        g2.drawString(detail, 10, 43);
        g2.dispose();
    }
}

final class MetricCard extends RoundedPanel {
    private final JLabel value;
    private final JLabel helper;

    MetricCard(String title, String initialValue, String initialHelper) {
        super(20, Color.WHITE, new Color(248, 250, 252));
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        add(WorkspaceFrame.label(title, 13, Font.BOLD, Palette.MUTED), BorderLayout.NORTH);
        value = WorkspaceFrame.label(initialValue, 34, Font.BOLD, Palette.INK);
        helper = WorkspaceFrame.label(initialHelper, 12, Font.PLAIN, Palette.MUTED);
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(value);
        body.add(Box.createVerticalStrut(5));
        body.add(helper);
        add(body, BorderLayout.CENTER);
    }

    void setValue(String nextValue, String nextHelper) {
        value.setText(nextValue);
        helper.setText(nextHelper);
    }
}

final class ProjectTile extends RoundedPanel {
    ProjectTile(String title, String detail, int progress) {
        super(18, Color.WHITE, new Color(248, 250, 252));
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JLabel heading = WorkspaceFrame.label(title, 15, Font.BOLD, Palette.INK);
        JLabel body = WorkspaceFrame.label(detail, 12, Font.PLAIN, Palette.MUTED);
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(heading);
        copy.add(Box.createVerticalStrut(5));
        copy.add(body);
        add(copy, BorderLayout.CENTER);
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(progress);
        bar.setStringPainted(false);
        bar.setBorderPainted(false);
        bar.setForeground(Palette.BLUE);
        bar.setBackground(new Color(226, 232, 240));
        add(bar, BorderLayout.SOUTH);
    }
}

final class ActivityRow extends JPanel {
    ActivityRow(String title, String detail, Color accent) {
        setOpaque(false);
        setLayout(new BorderLayout(12, 0));
        setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        Dot dot = new Dot(accent);
        add(dot, BorderLayout.WEST);
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(WorkspaceFrame.label(title, 14, Font.BOLD, Palette.INK));
        copy.add(Box.createVerticalStrut(3));
        copy.add(WorkspaceFrame.label(detail, 12, Font.PLAIN, Palette.MUTED));
        add(copy, BorderLayout.CENTER);
    }
}

final class Dot extends JComponent {
    private final Color color;

    Dot(Color color) {
        this.color = color;
        setPreferredSize(new Dimension(14, 14));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.fillOval(2, 2, 10, 10);
        g2.dispose();
    }
}

final class LineChartPanel extends RoundedPanel {
    private final Deque<Integer> values = new ArrayDeque<>();

    LineChartPanel() {
        super(22, Color.WHITE, new Color(248, 250, 252));
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 18, 20));
        for (int i = 0; i < 24; i++) {
            values.add(55 + (i % 6) * 6);
        }
    }

    void push(int value) {
        if (values.size() >= 28) {
            values.removeFirst();
        }
        values.addLast(value);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int left = 26;
        int top = 72;
        int width = getWidth() - 52;
        int height = getHeight() - 112;

        g2.setColor(Palette.INK);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
        g2.drawString("Execution confidence", 22, 35);
        g2.setColor(Palette.MUTED);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.drawString("Live score from sandbox telemetry", 22, 55);

        g2.setColor(new Color(226, 232, 240));
        for (int i = 0; i < 4; i++) {
            int y = top + (height * i / 3);
            g2.drawLine(left, y, left + width, y);
        }

        Path2D line = new Path2D.Double();
        int index = 0;
        int count = Math.max(1, values.size() - 1);
        for (int value : values) {
            double x = left + (width * index / (double) count);
            double y = top + height - (height * (value - 40) / 65.0);
            if (index == 0) {
                line.moveTo(x, y);
            } else {
                line.lineTo(x, y);
            }
            index++;
        }

        Shape clip = g2.getClip();
        Path2D fill = (Path2D) line.clone();
        fill.lineTo(left + width, top + height);
        fill.lineTo(left, top + height);
        fill.closePath();
        g2.setPaint(new GradientPaint(0, top, new Color(37, 99, 235, 80), 0, top + height, new Color(37, 99, 235, 0)));
        g2.fill(fill);
        g2.setClip(clip);
        g2.setColor(Palette.BLUE);
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(line);

        g2.dispose();
    }
}

class RoundedPanel extends JPanel {
    private final int radius;
    private final Color start;
    private final Color end;

    RoundedPanel(int radius, Color start, Color end) {
        this.radius = radius;
        this.start = start;
        this.end = end;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Shape shape = radius <= 0
            ? new java.awt.Rectangle(0, 0, getWidth(), getHeight())
            : new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), radius, radius);
        g2.setPaint(new GradientPaint(0, 0, start, getWidth(), getHeight(), end));
        g2.fill(shape);
        if (radius > 0) {
            g2.setColor(new Color(15, 23, 42, 20));
            g2.draw(shape);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}

final class RootPanel extends JPanel {
    RootPanel() {
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setPaint(new GradientPaint(0, 0, new Color(241, 245, 249), getWidth(), getHeight(), new Color(219, 234, 254)));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(new Color(255, 255, 255, 120));
        g2.fillOval(getWidth() - 280, -120, 420, 420);
        g2.dispose();
    }
}

final class GradientButton extends JButton {
    GradientButton(String text) {
        super(text);
        setFont(new Font("Segoe UI", Font.BOLD, 13));
        setForeground(Color.WHITE);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorder(BorderFactory.createEmptyBorder(11, 18, 11, 18));
        setMaximumSize(new Dimension(210, 44));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Area shape = new Area(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 14, 14));
        g2.setPaint(new GradientPaint(0, 0, Palette.BLUE, getWidth(), getHeight(), Palette.PINK));
        g2.fill(shape);
        g2.dispose();
        super.paintComponent(g);
    }
}

final class Palette {
    static final Color INK = new Color(15, 23, 42);
    static final Color MUTED = new Color(100, 116, 139);
    static final Color BLUE = new Color(37, 99, 235);
    static final Color GREEN = new Color(16, 185, 129);
    static final Color PINK = new Color(236, 72, 153);
    static final Color ORANGE = new Color(245, 158, 11);

    private Palette() {
    }
}
