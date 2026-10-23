package me.lhawk.mobvitals.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

import java.lang.reflect.Method;

public final class TextUtil {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
    private static final GsonComponentSerializer GSON_SERIALIZER = GsonComponentSerializer.gson();
    private static Method paperSendMessageMethod;
    private static boolean paperChecked = false;

    private TextUtil() {}

    public static String toJson(Component component) {
        if (component == null) return "{\"text\":\"\"}";
        return GSON_SERIALIZER.serialize(component);
    }

    public static Component parse(String miniMessageText) {
        if (miniMessageText == null || miniMessageText.isEmpty()) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(miniMessageText);
    }

    public static Component parse(String miniMessageText, TagResolver... resolvers) {
        if (miniMessageText == null || miniMessageText.isEmpty()) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(miniMessageText, resolvers);
    }

    public static Component parseLenient(String text) {
        try {
            return parse(text);
        } catch (RuntimeException e) {
            return fromLegacy(text);
        }
    }

    public static Component fromLegacy(String legacyText) {
        if (legacyText == null || legacyText.isEmpty()) {
            return Component.empty();
        }
        return LEGACY_SERIALIZER.deserialize(legacyText);
    }

    public static void sendMessage(CommandSender sender, String miniMessageText) {
        if (sender == null || miniMessageText == null || miniMessageText.isEmpty()) return;
        Component component = parse(miniMessageText);
        sendMessage(sender, component);
    }

    public static void sendMessage(CommandSender sender, Component component) {
        if (sender == null || component == null) return;
        if (!paperChecked) {
            try {
                paperSendMessageMethod = sender.getClass().getMethod("sendMessage", net.kyori.adventure.text.Component.class);
            } catch (NoSuchMethodException ignored) {
                paperSendMessageMethod = null;
            }
            paperChecked = true;
        }

        if (paperSendMessageMethod != null) {
            try {
                paperSendMessageMethod.invoke(sender, component);
                return;
            } catch (Throwable ignored) {}
        }

        String legacy = LEGACY_SERIALIZER.serialize(component);
        sender.sendMessage(legacy);
    }
}
