package io.github.lunatech.chatitem.config;

import org.spongepowered.configurate.interfaces.meta.Exclude;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.Map;

@ConfigSerializable
public class CustomIconsConfig implements VersionedConfig {
    @Comment("Do not change this value!")
    public int configVersion = 1;

    @Override
    @Exclude
    public int configVersion() {
        return configVersion;
    }

    @Comment("Customize what icon to show next to specified items in chat.\n" +
             "\n" +
             "Format guidelines:\n" +
             "Each entry requires a material ID name (e.g. TNT, CHEST, ENDER_CHEST).\n" +
             "Choose from two rendering models:\n" +
             "  1. atlas (Default): Uses standard client sprite sheets (minecraft:blocks or minecraft:items).\n" +
             "     Properties: atlas, sprite.\n" +
             "  2. player: Renders a 2D player skin face icon dynamically or statically.\n" +
             "     Properties (specify one):\n" +
             "       - name: A player username (e.g., \"Steve\"), or the dynamic template \"{player}\" to automatically show the showcasing player's skin face.\n" +
             "       - uuid: A player UUID string.\n" +
             "       - value: A base64-encoded skin textures property value.\n" +
             "       - hash: The Mojang skin texture hash suffix (the end of Mojang textures URL).\n" +
             "\n" +
             "Priority rule: If the material is also listed in excluded-icons.yml, it will be ignored and remain hidden.")
    public Map<String, IconOverride> customIcons = Map.of(
        "TNT", new IconOverride("atlas", "minecraft:blocks", "block/tnt_side", null, null, null, null),
        "ANCIENT_DEBRIS", new IconOverride("atlas", "minecraft:blocks", "block/ancient_debris_side", null, null, null, null),
        "CHEST", new IconOverride("player", null, null, null, null, null, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDVjNmRjMmJiZjUxYzM2Y2ZjNzcxNDU4NWE2YTU2ODNlZjJiMTRkNDdkOGZmNzE0NjU0YTg5M2Y1ZGE2MjIifX19"),
        "ENDER_CHEST", new IconOverride("player", null, null, null, null, "a6cc486c2be1cb9dfcb2e53dd9a3e9a883bfadb27cb956f1896d602b4067", null)
    );

    @ConfigSerializable
    public static class IconOverride {
        @Comment("The object type: either \"atlas\" (default) or \"player\"")
        public String object = "atlas";

        // Atlas specific properties
        public String atlas;
        public String sprite;

        // Player specific properties
        public String name;
        public String uuid;
        public String hash;
        public String value;

        public IconOverride() {}

        public IconOverride(String object, String atlas, String sprite, String name, String uuid, String hash, String value) {
            this.object = object;
            this.atlas = atlas;
            this.sprite = sprite;
            this.name = name;
            this.uuid = uuid;
            this.hash = hash;
            this.value = value;
        }
    }
}
