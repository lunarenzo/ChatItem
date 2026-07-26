package io.github.lunatech.chatitem.utility;

import java.security.SecureRandom;

public final class Util {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom random = new SecureRandom();

    /**
     * Generates a random string using alphanumeric characters
     *
     * @return a random string
     */
    public static String randomString() {
        return random.ints(generateRandomInt(1, 256), 0, CHARACTERS.length())
            .mapToObj(CHARACTERS::charAt)
            .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
            .toString();
    }

    /**
     * Generates a random string of specified length using alphanumeric characters
     *
     * @param length the desired length of the random string
     * @return a random string of the specified length
     */
    public static String randomString(int length) {
        return random.ints(length, 0, CHARACTERS.length())
            .mapToObj(CHARACTERS::charAt)
            .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
            .toString();
    }

    /**
     * Generates a random integer between lower and upper bounds (inclusive)
     *
     * @param lowerBound the minimum value (inclusive)
     * @param upperBound the maximum value (inclusive)
     * @return a random integer within the specified range
     */
    public static int generateRandomInt(int lowerBound, int upperBound) {
        return random.nextInt(lowerBound, upperBound + 1);
    }

    /**
     * Converts a raw material name (e.g. DIAMOND_SWORD or DIRT) into a friendly display name (e.g. Diamond Sword or Dirt).
     *
     * @param materialName the raw material name
     * @return the friendly display name
     */
    public static String getFriendlyMaterialName(String materialName) {
        if (materialName == null || materialName.isEmpty()) {
            return "";
        }
        String name = materialName.replace('_', ' ').toLowerCase();
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : name.toCharArray()) {
            if (c == ' ') {
                sb.append(c);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Gets the dynamic rarity color of an ItemStack.
     *
     * @param itemStack the item stack
     * @return the Adventure TextColor corresponding to the item's rarity
     */
    public static net.kyori.adventure.text.format.TextColor getItemRarityColor(org.bukkit.inventory.ItemStack itemStack) {
        if (itemStack == null) {
            return net.kyori.adventure.text.format.NamedTextColor.WHITE;
        }

        // 1. Try modern Data Component API (Minecraft 1.20.5+)
        try {
            var meta = itemStack.getItemMeta();
            if (meta != null) {
                var rarity = meta.get(io.papermc.paper.datacomponent.DataComponentTypes.RARITY);
                if (rarity != null) {
                    return switch (rarity) {
                        case COMMON -> net.kyori.adventure.text.format.NamedTextColor.WHITE;
                        case UNCOMMON -> net.kyori.adventure.text.format.NamedTextColor.YELLOW;
                        case RARE -> net.kyori.adventure.text.format.NamedTextColor.AQUA;
                        case EPIC -> net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE;
                    };
                }
            }
        } catch (Throwable ignored) {
            // Fallback if class/method doesn't exist
        }

        // 2. Try Material Rarity API
        try {
            var rarity = itemStack.getType().getItemRarity();
            if (rarity != null) {
                return switch (rarity) {
                    case COMMON -> net.kyori.adventure.text.format.NamedTextColor.WHITE;
                    case UNCOMMON -> net.kyori.adventure.text.format.NamedTextColor.YELLOW;
                    case RARE -> net.kyori.adventure.text.format.NamedTextColor.AQUA;
                    case EPIC -> net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE;
                };
            }
        } catch (Throwable ignored) {
        }

        return net.kyori.adventure.text.format.NamedTextColor.WHITE;
    }
}
