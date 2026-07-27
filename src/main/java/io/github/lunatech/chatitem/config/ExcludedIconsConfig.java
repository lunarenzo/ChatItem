package io.github.lunatech.chatitem.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.List;

@ConfigSerializable
public class ExcludedIconsConfig {
    @Comment("List of item IDs (material names) or wildcard patterns whose icons should not be shown in chat.\n" +
             "You can specify exact material names (e.g. MACE, HEAVY_CORE) or wildcard suffixes (e.g. *_FENCE, *_STAIRS).")
    public List<String> excludedIcons = List.of(
        "*_FENCE",
        "*_STAIRS",
        "*_CHEST",
        "CHEST",
        "*_SHULKER_BOX",
        "SHULKER_BOX",
        "*_HEAD",
        "*_SKULL",
        "DRAGON_HEAD",
        "MACE",
        "HEAVY_CORE",
        "SHIELD"
    );
}
