package io.github.lunatech.chatitem.utility;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class Util {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final Random random = new Random();

    // In-memory read-only Set containing pre-compiled vanilla item and block sprite paths
    private static final Set<String> vanillaSprites = new HashSet<>();

    static {
        try (InputStream is = Util.class.getResourceAsStream("/minecraft-sprites.txt")) {
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.isBlank()) {
                            vanillaSprites.add(line.trim());
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Safe fallback if resources fail to read at class loading time
        }
    }

    /**
     * Generates a random string of specified length using alphanumeric characters
     *
     * @param length the desired length of the random string
     * @return a random string of the specified length
     */
    public static String randomString(int length) {
        char[] buffer = new char[length];
        for (int i = 0; i < length; i++) {
            buffer[i] = CHARACTERS.charAt(random.nextInt(CHARACTERS.length()));
        }
        return new String(buffer);
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
     * Checks if a material should pull its flat 2D sprite icon from the minecraft:items atlas.
     * Captures blocks that use 2D items (Chests, Heavy Cores, Heads) and non-block 3D entities (Shields).
     *
     * @param material the material type
     * @return true if the item uses a 2D item icon sprite in the items atlas
     */
    public static boolean isItemSpriteBlock(org.bukkit.Material material) {
        if (material == null || material.isAir()) {
            return false;
        }

        // 1. CRITICAL: Shields, Tools, Swords, Apples are NOT blocks, but they MUST use the items atlas!
        if (!material.isBlock()) {
            return true;
        }

        // 2. Query public Tag API from Bukkit/Paper (super fast, zero reflection)
        try {
            if (org.bukkit.Tag.DOORS.isTagged(material)
                || org.bukkit.Tag.BEDS.isTagged(material)
                || org.bukkit.Tag.SIGNS.isTagged(material)
                || org.bukkit.Tag.ITEMS_BANNERS.isTagged(material)
                || org.bukkit.Tag.SHULKER_BOXES.isTagged(material)) {
                return true;
            }
        } catch (Throwable ignored) {
            // Ignore and fall back
        }

        // 3. Suffix fallback for other specific blocks (chests, heads, utility blocks, campfire, heavy core, etc.)
        String name = material.name();
        return name.endsWith("CHEST")
            || name.endsWith("SHULKER_BOX")
            || name.endsWith("BANNER")
            || name.endsWith("SIGN")
            || name.endsWith("HEAD")
            || name.endsWith("SKULL")
            || (name.endsWith("DOOR") && !name.endsWith("TRAPDOOR"))
            || name.endsWith("BED")
            || name.endsWith("CAMPFIRE")
            || name.endsWith("POT")
            || name.equals("HEAVY_CORE")
            || name.equals("BREWING_STAND")
            || name.equals("CAULDRON")
            || name.equals("BELL");
    }

    /**
     * Container class representing the atlas namespace and resource path for a given material icon.
     */
    public static class SpriteMapping {
        public final String atlas;
        public final String sprite;

        public SpriteMapping(String atlas, String sprite) {
            this.atlas = atlas;
            this.sprite = sprite;
        }
    }

    /**
     * Dynamically resolves the correct sprite atlas namespace and path for any Bukkit material.
     * Maps using the pre-compiled vanilla textures file for 100% precision.
     *
     * @param material the Material to map
     * @return a SpriteMapping defining atlas and path
     */
    public static SpriteMapping getSpriteMapping(org.bukkit.Material material) {
        String cleanName = material.name().toLowerCase();

        // 1. Perform static O(1) checks against the compiled vanilla sprites resource database
        String itemPath = "item/" + cleanName;
        if (vanillaSprites.contains(itemPath)) {
            return new SpriteMapping("minecraft:items", itemPath);
        }

        String blockPath = "block/" + cleanName;
        if (vanillaSprites.contains(blockPath)) {
            return new SpriteMapping("minecraft:blocks", blockPath);
        }

        // 2. Dynamic Fallback: Standard Items / Blocks that use 2D Item Sprites
        if (isItemSpriteBlock(material)) {
            String spritePath;

            // Handle Shields specifically
            if (material == org.bukkit.Material.SHIELD) {
                return new SpriteMapping("minecraft:items", "item/shield_base");
            }

            // Handle Animated/Entity Blocks that drop the "item/" prefix entirely
            if (cleanName.endsWith("chest")
                || cleanName.endsWith("shulker_box")
                || cleanName.endsWith("head")
                || cleanName.endsWith("skull")
                || cleanName.equals("heavy_core")) {
                spritePath = cleanName; // e.g. "heavy_core" or "ender_chest"
            } else {
                spritePath = "item/" + cleanName; // e.g. "item/apple", "item/iron_sword"
            }

            return new SpriteMapping("minecraft:items", spritePath);
        }

        // 3. Dynamic Fallback: Voxel Blocks that have 3D models in hand (Fences, Stairs, Stone)
        // These pull standard 2D flat textures out of the blocks atlas instead
        String fallbackBlockPath;
        if (cleanName.endsWith("_fence")) {
            if (cleanName.contains("nether_brick")) {
                fallbackBlockPath = "block/nether_bricks";
            } else {
                fallbackBlockPath = "block/" + cleanName.replace("_fence", "_planks");
            }
        } else if (cleanName.endsWith("_stairs")) {
            String base = cleanName.replace("_stairs", "");
            if (base.equals("oak") || base.equals("spruce") || base.equals("birch") || base.equals("jungle")
                || base.equals("acacia") || base.equals("dark_oak") || base.equals("crimson") || base.equals("warped")
                || base.equals("mangrove") || base.equals("cherry") || base.equals("bamboo")) {
                fallbackBlockPath = "block/" + base + "_planks";
            } else if (base.endsWith("brick")) {
                fallbackBlockPath = "block/" + base + "s";
            } else if (base.equals("purpur")) {
                fallbackBlockPath = "block/purpur_block";
            } else if (base.equals("quartz")) {
                fallbackBlockPath = "block/quartz_block";
            } else {
                fallbackBlockPath = "block/" + base;
            }
        } else {
            fallbackBlockPath = "block/" + cleanName;
        }

        return new SpriteMapping("minecraft:blocks", fallbackBlockPath);
    }
}
