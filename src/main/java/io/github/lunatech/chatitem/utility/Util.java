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
     * Checks if a material is a block but has its 2D inventory icon sprite inside minecraft:items atlas.
     *
     * @param material the material type
     * @return true if the block uses a 2D item icon sprite in the items atlas
     */
    public static boolean isItemSpriteBlock(org.bukkit.Material material) {
        if (!material.isBlock()) {
            return false;
        }

        try {
            // Retrieve NMS representation of the block via registry lookup
            var key = net.minecraft.resources.ResourceLocation.parse(material.getKey().toString());
            var nmsBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(key);
            if (nmsBlock != null) {
                var shape = nmsBlock.getRenderShape(nmsBlock.defaultBlockState());
                // ENTITYBLOCK_ANIMATED = Chests, Shulker Boxes, Ender Chests, Bell, Conduit, etc.
                // INVISIBLE = Air, Light, structures
                if (shape == net.minecraft.world.level.block.RenderShape.ENTITYBLOCK_ANIMATED 
                    || shape == net.minecraft.world.level.block.RenderShape.INVISIBLE) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
            // Safe fallback if NMS mappings differ at runtime
        }

        // Suffix/name fallback for other blocks that render with custom 2D items
        String name = material.name();
        return name.endsWith("CHEST")
            || name.endsWith("SHULKER_BOX")
            || name.endsWith("BANNER")
            || name.endsWith("SIGN")
            || name.endsWith("HEAD")
            || name.endsWith("SKULL")
            || name.endsWith("DOOR")
            || name.endsWith("BED")
            || name.endsWith("CAMPFIRE")
            || name.endsWith("POT")
            || name.equals("HEAVY_CORE")
            || name.equals("BREWING_STAND")
            || name.equals("CAULDRON")
            || name.equals("BELL");
    }
}
