package me.lhawk.mobvitals;

import me.lhawk.mobvitals.util.Platform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PlatformTest {

    @AfterEach
    public void cleanup() {
        Platform.resetForTests();
    }

    @Test
    public void testPlatformDetection() {
        assertFalse(Platform.isFolia());
        Platform.resetForTests();
    }
}
