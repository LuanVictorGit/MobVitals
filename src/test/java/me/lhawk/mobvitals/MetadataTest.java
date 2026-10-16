package me.lhawk.mobvitals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MetadataTest {
    @Test
    public void testMetadataFlags() {
        byte invisibleFlag = (byte) 0x20;
        assertEquals(32, invisibleFlag);

        byte armorStandFlags = (byte) (0x01 | 0x08 | 0x10);
        assertEquals(25, armorStandFlags);
        assertTrue((armorStandFlags & 0x01) != 0);
        assertTrue((armorStandFlags & 0x08) != 0);
        assertTrue((armorStandFlags & 0x10) != 0);
        assertFalse((armorStandFlags & 0x04) != 0);
    }
}
