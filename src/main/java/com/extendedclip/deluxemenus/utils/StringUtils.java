package com.extendedclip.deluxemenus.utils;

import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StringUtils {

    private final static Pattern HEX_PATTERN = Pattern
            .compile("&(#[a-f0-9]{6})", Pattern.CASE_INSENSITIVE);

    private final static Pattern MINI_MESSAGE_TAG_PATTERN = Pattern.compile("<[^<>]+>");

    // Matches a legacy '&' or already-translated '§' code: either a 6-digit hex ("&#rrggbb")
    // or a single format/color character (e.g. "&c", "&l", "&r").
    private final static Pattern LEGACY_CODE_PATTERN = Pattern
            .compile("[&§](#[a-f0-9]{6}|[0-9a-fk-or])", Pattern.CASE_INSENSITIVE);

    private final static Map<Character, String> LEGACY_CODE_TO_TAG_NAME = Map.ofEntries(
            Map.entry('0', "black"), Map.entry('1', "dark_blue"), Map.entry('2', "dark_green"),
            Map.entry('3', "dark_aqua"), Map.entry('4', "dark_red"), Map.entry('5', "dark_purple"),
            Map.entry('6', "gold"), Map.entry('7', "gray"), Map.entry('8', "dark_gray"),
            Map.entry('9', "blue"), Map.entry('a', "green"), Map.entry('b', "aqua"),
            Map.entry('c', "red"), Map.entry('d', "light_purple"), Map.entry('e', "yellow"),
            Map.entry('f', "white"), Map.entry('k', "obfuscated"), Map.entry('l', "bold"),
            Map.entry('m', "strikethrough"), Map.entry('n', "underlined"), Map.entry('o', "italic"),
            Map.entry('r', "reset")
    );

    // Bukkit/Spigot only understands hex colors encoded as repeated "&x&r&r&g&g&b&b" section
    // sequences, not MiniMessage's/Kyori's default "&#rrggbb" shorthand. Without both hexColors()
    // and useUnusualXRepeatedCharacterHexFormat(), gradients get downsampled to the nearest of the
    // 16 legacy colors (or produce unrecognised codes) instead of rendering as an actual gradient.
    private final static LegacyComponentSerializer MINI_MESSAGE_LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    /**
     * Translates the ampersand color codes like '&7' to their section symbol counterparts like '§7'.
     * <br>
     * It also translates hex colors like '&#aaFF00' to their section symbol counterparts like '§x§a§a§F§F§0§0'.
     * <br>
     * MiniMessage tags (e.g. '&lt;red&gt;', '&lt;gradient:red:blue&gt;') are also translated if present,
     * and can be freely mixed with legacy '&' codes in the same string.
     *
     * @param input The string in which to translate the color codes.
     * @return The string with the translated colors.
     */
    @NotNull
    public static String color(@NotNull String input) {
        if (MINI_MESSAGE_TAG_PATTERN.matcher(input).find()) {
            try {
                final Component parsed = MiniMessage.miniMessage().deserialize(input);
                input = MINI_MESSAGE_LEGACY_SERIALIZER.serialize(parsed);
            } catch (final Exception ignored) {
                // Not valid MiniMessage (or a placeholder produced an invalid tag). Fall back to
                // treating the string as legacy-only rather than breaking the menu.
            }
        }

        // Hex Support for 1.16.1+
        Matcher m = HEX_PATTERN.matcher(input);
        if (VersionHelper.IS_HEX_VERSION) {
            while (m.find()) {
                input = input.replace(m.group(), ChatColor.of(m.group(1)).toString());
            }
        }

        return ChatColor.translateAlternateColorCodes('&', input);
    }

    /**
     * Like {@link #color(String)}, but returns a {@link Component} instead of collapsing the result
     * to a legacy string. This is required for MiniMessage tags that have no legacy equivalent, such
     * as {@code <sprite:...>}, which embed a non-text object (e.g. a resource pack icon) that a plain
     * '§'-coded string cannot represent.
     * <br>
     * Legacy '&' codes (and hex colors) are converted to their MiniMessage tag equivalents first, so
     * the whole string can be parsed by MiniMessage in a single pass alongside any native tags.
     *
     * @param input The string in which to translate the color codes.
     * @return The parsed component.
     */
    @NotNull
    public static Component colorComponent(@NotNull final String input) {
        try {
            final String miniMessageInput = LEGACY_CODE_PATTERN.matcher(input).replaceAll(StringUtils::legacyCodeToTag);
            return MiniMessage.miniMessage().deserialize(miniMessageInput);
        } catch (final Exception ignored) {
            // Not valid MiniMessage once legacy codes are converted to tags (e.g. a stray '<' or '>'
            // in the text that isn't part of a tag). Fall back to plain legacy coloring so the line
            // still renders, just without any sprite/object tags it may have contained.
            return MINI_MESSAGE_LEGACY_SERIALIZER.deserialize(color(input));
        }
    }

    @NotNull
    private static String legacyCodeToTag(@NotNull final MatchResult match) {
        final String code = match.group(1);
        if (code.charAt(0) == '#') {
            return "<" + code + ">";
        }

        final String tagName = LEGACY_CODE_TO_TAG_NAME.get(Character.toLowerCase(code.charAt(0)));
        return tagName == null ? match.group() : "<" + tagName + ">";
    }

    @NotNull
    public static String replacePlaceholdersAndArguments(@NotNull String input, final @Nullable Map<String, String> arguments,
                                                         final @Nullable Player player,
                                                         final boolean parsePlaceholdersInsideArguments,
                                                         final boolean parsePlaceholdersAfterArguments) {
        if (player == null) {
            return replaceArguments(input, arguments, null, parsePlaceholdersInsideArguments);
        }

        if (parsePlaceholdersAfterArguments) {
            return replacePlaceholders(replaceArguments(input, arguments, player, parsePlaceholdersInsideArguments), player);
        }

        return replaceArguments(replacePlaceholders(input, player), arguments, player, parsePlaceholdersInsideArguments);
    }

    @NotNull
    public static String replacePlaceholders(final @NotNull String input, final @NotNull Player player) {
        return PlaceholderAPI.setPlaceholders(player, input);
    }

    @NotNull
    public static String replaceArguments(@NotNull String input, final @Nullable Map<String, String> arguments,
                                          final @Nullable Player player, boolean parsePlaceholdersInsideArguments) {
        if (arguments == null || arguments.isEmpty()) {
            return input;
        }

        for (final Map.Entry<String, String> entry : arguments.entrySet()) {
            final String value = player != null && parsePlaceholdersInsideArguments
                    ? replacePlaceholders(entry.getValue(), player)
                    : entry.getValue();
            input = input.replace("{" + entry.getKey() + "}", value);
        }

        return input;
    }

    @Nullable
    public static Color parseRGBColor(@NotNull final String input) {
        final String[] parts = input.split(",");
        try {
            return Color.fromRGB(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim())
            );
        } catch (final Exception exception) {
            return null;
        }
    }
}
