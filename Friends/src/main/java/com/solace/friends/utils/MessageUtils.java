package com.solace.friends.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class MessageUtils {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    /**
     * Converts a string with Minecraft color codes (&e, &a, etc.) to a Component
     * @param message The message with color codes
     * @return The Component
     */
    public static Component colorize(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        return LEGACY_SERIALIZER.deserialize(message);
    }

    /**
     * Gets a message from config and replaces placeholders
     * @param configPath The config path
     * @param defaultMessage The default message if not found in config
     * @param replacements Placeholder replacements (key, value pairs)
     * @return The colorized Component
     */
    public static Component getMessage(String configPath, String defaultMessage, String... replacements) {
        // This method will be used by the plugin class
        String message = defaultMessage;
        if (replacements.length % 2 == 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        return colorize(message);
    }
}
