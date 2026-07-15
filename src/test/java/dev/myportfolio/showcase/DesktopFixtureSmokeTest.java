package dev.myportfolio.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

final class DesktopFixtureSmokeTest {
    @Test
    void rendersAndExercisesSafeDesktopInputs() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Desktop smoke test requires a display");
        AtomicReference<WorkspaceFrame> frameRef = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            WorkspaceFrame frame = new WorkspaceFrame(new ShowcaseModel(
                7L,
                Clock.fixed(Instant.parse("2026-07-15T10:11:12Z"), ZoneOffset.UTC)
            ));
            frame.setLocation(20, 20);
            frame.setVisible(true);
            frameRef.set(frame);
        });

        WorkspaceFrame frame = frameRef.get();
        Robot robot = new Robot();
        robot.setAutoDelay(40);
        robot.waitForIdle();

        try {
            JButton copy = findButton(frame, "Copy token");
            JButton resize = findButton(frame, "Resize");
            JButton newWindow = findButton(frame, "New window");
            assertNotNull(copy);
            assertNotNull(resize);
            assertNotNull(newWindow);

            SwingUtilities.invokeAndWait(copy::doClick);
            Object clipboard = frame.getToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            assertEquals(WorkspaceFrame.CLIPBOARD_TOKEN, clipboard);

            int originalWidth = frame.getWidth();
            SwingUtilities.invokeAndWait(resize::doClick);
            assertTrue(frame.getWidth() != originalWidth);

            SwingUtilities.invokeAndWait(newWindow::doClick);
            assertTrue(Set.of(Window.getWindows()).stream().anyMatch(
                window -> window.isVisible() && "Evidence Inspector".equals(window.getName())
            ) || hasVisibleInspector());

            MouseProbePanel probe = findComponent(frame, MouseProbePanel.class);
            assertNotNull(probe);
            SwingUtilities.invokeAndWait(() -> probe.dispatchEvent(new MouseEvent(
                probe,
                MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(),
                0,
                24,
                24,
                1,
                false
            )));

            robot.waitForIdle();
            Rectangle bounds = new Rectangle(frame.getLocationOnScreen(), frame.getSize());
            BufferedImage screenshot = robot.createScreenCapture(bounds);
            Path target = Path.of("target", "desktop-fixture-smoke.png");
            Files.createDirectories(target.getParent());
            ImageIO.write(screenshot, "png", target.toFile());

            Set<Integer> sampledColors = new HashSet<>();
            for (int y = 0; y < screenshot.getHeight(); y += 12) {
                for (int x = 0; x < screenshot.getWidth(); x += 12) {
                    sampledColors.add(screenshot.getRGB(x, y));
                }
            }
            assertTrue(sampledColors.size() > 20, "Rendered fixture should not be blank or monochrome");
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }

    private boolean hasVisibleInspector() {
        for (Window window : Window.getWindows()) {
            if (window.isVisible() && window instanceof javax.swing.JFrame inspector
                && "Evidence Inspector".equals(inspector.getTitle())) {
                return true;
            }
        }
        return false;
    }

    private JButton findButton(Container root, String text) {
        for (Component component : root.getComponents()) {
            if (component instanceof JButton button && text.equals(button.getText())) return button;
            if (component instanceof Container container) {
                JButton nested = findButton(container, text);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private <T extends Component> T findComponent(Container root, Class<T> type) {
        for (Component component : root.getComponents()) {
            if (type.isInstance(component)) return type.cast(component);
            if (component instanceof Container container) {
                T nested = findComponent(container, type);
                if (nested != null) return nested;
            }
        }
        return null;
    }
}
