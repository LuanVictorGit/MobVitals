package me.lhawk.mobvitals;

import me.lhawk.mobvitals.death.DeathProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DeathProfileTest {

    @Test
    public void testDeathProfileResolution() {
        for (DeathProfile profile : DeathProfile.values()) {
            DeathProfile resolved = DeathProfile.fromName(profile.name());
            assertNotNull(resolved);
            assertEquals(profile, resolved);

            assertNotNull(profile.getAmbientParticles());
            assertTrue(profile.getAmbientParticles().length > 0);

            assertNotNull(profile.getFinaleParticles());
            assertTrue(profile.getFinaleParticles().length > 0);

            assertNotNull(profile.getAmbientSound());
            assertNotNull(profile.getFinaleSound());

            assertTrue(profile.getAmbientPitch() > 0.0f);
            assertTrue(profile.getFinalePitch() > 0.0f);
        }
    }

    @Test
    public void testInvalidProfileReturnsNull() {
        assertNull(DeathProfile.fromName("NON_EXISTENT_PROFILE"));
        assertNull(DeathProfile.fromName(null));
    }
}
