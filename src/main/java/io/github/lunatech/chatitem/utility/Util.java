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

        // 3. Query NMS via reflection to dynamically identify blocks with animated/invisible inventory sprites
        try {
            Class<?> craftMagicNumbers = Class.forName("org.bukkit.craftbukkit.util.CraftMagicNumbers");
            java.lang.reflect.Method getBlockMethod = craftMagicNumbers.getDeclaredMethod("getBlock", org.bukkit.Material.class);
            Object nmsBlock = getBlockMethod.invoke(null, material);
            if (nmsBlock != null) {
                java.lang.reflect.Method defaultBlockStateMethod = nmsBlock.getClass().getMethod("defaultBlockState");
                Object defaultBlockState = defaultBlockStateMethod.invoke(nmsBlock);
                java.lang.reflect.Method getRenderShapeMethod = nmsBlock.getClass().getMethod("getRenderShape", defaultBlockState.getClass());
                Object renderShapeObj = getRenderShapeMethod.invoke(nmsBlock, defaultBlockState);
                if (renderShapeObj != null) {
                    String shapeName = ((Enum<?>) renderShapeObj).name();
                    // ENTITYBLOCK_ANIMATED = Chests, Shulker Boxes, Ender Chests, Bell, Conduit, etc.
                    // INVISIBLE = Air, Light, structures
                    if ("ENTITYBLOCK_ANIMATED".equals(shapeName) || "INVISIBLE".equals(shapeName)) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
            // Safe fallback if reflection fails due to version mismatches
        }

        // 4. Suffix fallback for other specific blocks (chests, heads, utility blocks, campfire, heavy core, etc.)
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
     * Handles pluralizations, model variants (Fences, Stairs), and bare registry names (Ender Chest, Heavy Core) cleanly.
     *
     * @param material the Material to map
     * @return a SpriteMapping defining atlas and path
     */
    public static SpriteMapping getSpriteMapping(org.bukkit.Material material) {
        String cleanName = material.name().toLowerCase();

        // CASE 1: Standard Items / Blocks that use 2D Item Sprites
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

        // CASE 2: Voxel Blocks that have 3D models in hand (Fences, Stairs, Stone)
        // These pull standard 2D flat textures out of the blocks atlas instead
        String blockPath;
        if (cleanName.endsWith("_fence")) {
            if (cleanName.contains("nether_brick")) {
                blockPath = "block/nether_bricks";
            } else {
                blockPath = "block/" + cleanName.replace("_fence", "_planks");
            }
        } else if (cleanName.endsWith("_stairs")) {
            String base = cleanName.replace("_stairs", "");
            if (base.equals("oak") || base.equals("spruce") || base.equals("birch") || base.equals("jungle")
                || base.equals("acacia") || base.equals("dark_oak") || base.equals("crimson") || base.equals("warped")
                || base.equals("mangrove") || base.equals("cherry") || base.equals("bamboo")) {
                blockPath = "block/" + base + "_planks";
            } else if (base.endsWith("brick")) {
                blockPath = "block/" + base + "s";
            } else if (base.equals("purpur")) {
                blockPath = "block/purpur_block";
            } else if (base.equals("quartz")) {
                blockPath = "block/quartz_block";
            } else {
                blockPath = "block/" + base;
            }
        } else {
            blockPath = "block/" + cleanName;
        }

        return new SpriteMapping("minecraft:blocks", blockPath);
    }
}
