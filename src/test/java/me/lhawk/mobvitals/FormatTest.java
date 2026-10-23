package me.lhawk.mobvitals;

import me.lhawk.mobvitals.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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

    @Test
    public void testLegacyAndTagLikeNamesAreInsertedAsText() {
        Component legacy = TextUtil.fromLegacy("§6Boss");
        assertEquals("Boss", PlainTextComponentSerializer.plainText().serialize(legacy));
        assertEquals(NamedTextColor.GOLD, legacy.children().isEmpty() ? legacy.color() : legacy.children().get(0).color());

        Component bar = TextUtil.parse("<white><bold><name></bold></white> <red>20/20</red>",
                Placeholder.component("name", TextUtil.fromLegacy("<red>Injected</red>")));
        assertEquals("<red>Injected</red> 20/20", PlainTextComponentSerializer.plainText().serialize(bar));
    }

    @Test
    public void testLenientParsingFallsBackToLegacy() {
        assertEquals("Gladiator", PlainTextComponentSerializer.plainText().serialize(TextUtil.parseLenient("<gold>Gladiator</gold>")));
        assertEquals("Gladiator", PlainTextComponentSerializer.plainText().serialize(TextUtil.parseLenient("§6Gladiator")));
    }
}
