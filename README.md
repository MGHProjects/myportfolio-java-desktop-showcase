# MyPortfolio Java Desktop Showcase

A polished Java Swing desktop app built to test MyPortfolio Desktop Protocol sandboxes.

It is intentionally dependency-free: Maven builds a runnable jar and the app exercises native desktop behavior such as focus, minimize, restore, drawing, timers, panels, and input controls.

## Run Locally

```bash
mvn package
java -jar target/myportfolio-java-desktop-showcase.jar
```

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
