# MyPortfolio Java Desktop Showcase

A deterministic Java Swing desktop fixture built to test MyPortfolio Desktop Protocol sandboxes.

The runtime jar is dependency-free. It exercises actual desktop behavior rather than presenting a passive mock: focus, resize and DPI reporting, mouse and drag events, keyboard entry, clipboard access, modal dialogs, multiple windows, clean shutdown, and an explicit non-zero crash path.

## Run Locally

```bash
./mvnw package
java -jar target/myportfolio-java-desktop-showcase.jar
```

## Interaction Matrix

| Probe | Expected result |
| --- | --- |
| Type in the input | MDP keyboard events reach a focused Swing control |
| Click or drag the blue surface | Pointer coordinates and event count update |
| `Dialog` or `Ctrl+D` | A modal Swing dialog opens and can be dismissed |
| `New window` or `Ctrl+N` | A second focusable window opens |
| `Copy token` or `Ctrl+Shift+C` | The fixture proof URI is written to the clipboard |
| `Resize` | The frame changes size and reports dimensions and screen DPI |
| `Clean close` | The process exits through `WINDOW_CLOSING` with status 0 |
| `Crash 42` | After confirmation, the process exits with status 42 for crash capture |

## Sandbox Notes

MyPortfolio should detect this as a Java desktop app because it uses `javax.swing` and `java.awt`.

Expected sandbox profile:

- Language: Java
- Display mode: desktop
- Build: `mvn package -DskipTests -q`
- Run: `java -jar target/*.jar`

Use it to test:

- MDP/1 PNG desktop stream
- Mouse and keyboard input
- Minimize and restore from the MyPortfolio desktop toolbar
- Window switching
- Clipboard, dialog, resize, and DPI behavior
- Clean-close versus non-zero crash evidence

## Verification

`./mvnw verify` runs a repeatability test for the telemetry model and a real AWT smoke test. CI starts an Xvfb display, drives safe controls, opens the secondary window, records pointer input, and writes `target/desktop-fixture-smoke.png`. The test rejects blank or monochrome captures.

Licensed under the MIT License.
