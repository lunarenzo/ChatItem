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

    @Comment("Customize what icon to show for specific items.\n" +
             "Specify the material name, the object type (\"atlas\" or \"player\"), and its properties.\n" +
             "For atlas type, specify: atlas, sprite.\n" +
             "For player type, specify either: name (can be username or {player} placeholder), uuid, hash (skin hash), or value (base64).\n" +
             "Note: If an item is listed in excluded-icons.yml, it will be ignored and remain hidden.")
    public Map<String, IconOverride> customIcons = Map.of(
        "TNT", new IconOverride("atlas", "minecraft:blocks", "block/tnt_side", null, null, null, null),
        "ANCIENT_DEBRIS", new IconOverride("atlas", "minecraft:blocks", "block/ancient_debris_side", null, null, null, null)
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
