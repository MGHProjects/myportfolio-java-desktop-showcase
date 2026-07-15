package dev.myportfolio.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

final class ShowcaseModelTest {
    @Test
    void emitsRepeatableEvidenceTelemetryForTheSameSeedAndClock() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-15T12:34:56Z"), ZoneOffset.UTC);
        ShowcaseModel first = new ShowcaseModel(42L, clock);
        ShowcaseModel second = new ShowcaseModel(42L, clock);

        for (int index = 0; index < 12; index++) {
            ShowcaseSnapshot snapshot = first.next();
            assertEquals(snapshot, second.next());
            assertTrue(snapshot.health() >= 94 && snapshot.health() <= 99);
            assertTrue(snapshot.confidence() >= 58 && snapshot.confidence() <= 96);
            assertEquals("12:34:56", snapshot.time());
        }
    }
}
