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
             "Specify the material name, the atlas namespace (minecraft:blocks or minecraft:items), and the sprite resource path.\n" +
             "Note: If an item is listed in excluded-icons.yml, it will be ignored and remain hidden.")
    public Map<String, IconOverride> customIcons = Map.of(
        "TNT", new IconOverride("minecraft:blocks", "block/tnt_side"),
        "ANCIENT_DEBRIS", new IconOverride("minecraft:blocks", "block/ancient_debris_side")
    );

    @ConfigSerializable
    public static class IconOverride {
        public String atlas;
        public String sprite;

        public IconOverride() {}

        public IconOverride(String atlas, String sprite) {
            this.atlas = atlas;
            this.sprite = sprite;
        }
    }
}
