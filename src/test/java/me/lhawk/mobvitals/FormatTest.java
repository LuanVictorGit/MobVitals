package me.lhawk.mobvitals;

import me.lhawk.mobvitals.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FormatTest {

    @Test
    public void testMiniMessageParsing() {
        Component empty = TextUtil.parse("");
        assertNotNull(empty);

        Component nullComp = TextUtil.parse(null);
        assertNotNull(nullComp);

        Component comp = TextUtil.parse("<green>Zombie</green> <red>20/20 ❤</red>");
        assertNotNull(comp);
    }

    @Test
    public void testHealthBarMath() {
        int barLength = 10;
        double health = 15.0;
        double maxHealth = 20.0;
        double fraction = Math.max(0.0, Math.min(1.0, health / maxHealth));

        int filled = (int) Math.round(fraction * barLength);
        int empty = barLength - filled;

        assertEquals(8, filled);
        assertEquals(2, empty);
        assertEquals(10, filled + empty);
    }
}
