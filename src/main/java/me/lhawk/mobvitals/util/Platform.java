package me.lhawk.mobvitals.util;

public final class Platform {
    private static Boolean folia;
    private static Boolean packetEvents;

    private Platform() {}

    public static boolean isFolia() {
        if (folia != null) return folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        return folia;
    }

    public static boolean hasPacketEvents() {
        if (packetEvents != null) return packetEvents;
        try {
            Class.forName("com.github.retrooper.packetevents.PacketEvents");
            packetEvents = true;
        } catch (ClassNotFoundException e) {
            packetEvents = false;
        }
        return packetEvents;
    }

    public static boolean isTextDisplaySupported() {
        if (!hasPacketEvents()) return false;
        try {
            return com.github.retrooper.packetevents.PacketEvents.getAPI()
                    .getServerManager()
                    .getVersion()
                    .isNewerThanOrEquals(com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_19_4);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void resetForTests() {
        folia = null;
        packetEvents = null;
    }
}
