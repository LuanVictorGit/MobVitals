package me.lhawk.mobvitals;

import me.lhawk.mobvitals.indicator.VariationPopup;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class DenseReductionAndPopupTest {

    @Test
    public void testVariationPopupLifecycle() {
        int id = 1500000005;
        Component text = Component.text("-5 ❤");
        long expire = System.currentTimeMillis() + 1800L;
        VariationPopup popup = new VariationPopup(id, 42, true, null, text, expire);

        assertEquals(id, popup.getVirtualId());
        assertEquals(42, popup.getHostEntityId());
        assertTrue(popup.isDamage());
        assertEquals(text, popup.getComponent());
        assertEquals(expire, popup.getExpireTimeMillis());

        UUID viewer = UUID.randomUUID();
        assertTrue(popup.addViewer(viewer));
        assertFalse(popup.addViewer(viewer));
        assertTrue(popup.getViewers().contains(viewer));
        assertTrue(popup.removeViewer(viewer));
        assertFalse(popup.getViewers().contains(viewer));
    }

    @Test
    public void testDenseReductionFactors() {
        long baseDurationMs = 5000L;

        assertEquals(3500L, Math.max(800L, (long) (baseDurationMs * 0.70)));
        assertEquals(2500L, Math.max(800L, (long) (baseDurationMs * 0.50)));
        assertEquals(1750L, Math.max(800L, (long) (baseDurationMs * 0.35)));

        int count = 5;
        double factor5 = Math.max(0.15, 1.0 / (count + 1));
        long scaled5 = Math.max(800L, (long) (baseDurationMs * factor5));
        assertTrue(scaled5 <= 1000L);
        assertTrue(scaled5 >= 800L);
    }

    @Test
    public void testHealthDifferenceLogic() {
        double prev = 20.0;
        double current = 14.5;
        double diff = prev - current;
        assertEquals(5.5, diff, 0.001);
        assertTrue(diff > 0.0);

        double healPrev = 10.0;
        double healCurrent = 16.0;
        double healDiff = healPrev - healCurrent;
        assertEquals(-6.0, healDiff, 0.001);
        assertTrue(healDiff < 0.0);
        assertEquals(6.0, -healDiff, 0.001);

        double fatalPrev = 7.0;
        double fatalCurrent = 0.0;
        double fatalDiff = fatalPrev - fatalCurrent;
        assertEquals(7.0, fatalDiff, 0.001);
    }
}
