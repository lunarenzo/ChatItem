package io.github.lunatech.chatitem.config;

import io.github.lunatech.chatitem.config.exception.ConfigValidationException;
import io.github.lunatech.chatitem.config.migration.Migration;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.interfaces.meta.Exclude;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.Map;

@ConfigSerializable
public class PluginConfig implements VersionedConfig {
    @Comment("Do not change this value!")
    public int configVersion = 1;

    @Override
    @Exclude
    public int configVersion() {
        return configVersion;
    }

    @Override
    @Exclude
    public @NotNull Map<Integer, Migration> migrations() {
        return Map.of();
    }

    @Override
    @Exclude
    public void validate() throws ConfigValidationException {
    }

    @Comment("Update Checker Settings")
    public UpdateChecker updateChecker = new UpdateChecker();

    @ConfigSerializable
    public static class UpdateChecker {
        @Comment("Should the plugin check for plugin updates on startup?")
        public boolean enabled = true;

        @Comment("Send update notifications to the console?")
        public boolean console = true;

        @Comment("Send update notifications to opped players on join?")
        public boolean op = true;
    }

    @Comment("Language, specify the language file to use, for chatitem `en_US` which will load `/lang/en_US.json`")
    public String language = "en_US";

    @Comment("Item Showcase Settings")
    public ItemShowcase itemShowcase = new ItemShowcase();

    @ConfigSerializable
    public static class ItemShowcase {
        @Comment("Cooldown in seconds between item showcases.")
        public int cooldownSeconds = 3;

        @Comment("Is a permission required to use [item] or [i]?")
        public boolean permissionRequired = true;

        @Comment("The permission node required if permissionRequired is true.")
        public String permissionNode = "chatitem.showcase.item";

        @Comment("The MiniMessage format for the item tag in chat.\n" +
                 "Placeholders:\n" +
                 "  - <name>: The unstyled item name. Inherits the color defined in this format (e.g. <aqua><name></aqua>).\n" +
                 "  - <raw_name>: The item name styled with its natural vanilla rarity color (e.g. aqua for enchanted golden apple, white for common items).\n" +
                 "  - <amount>: The item quantity suffix (e.g., ' x32' if quantity > 1, empty if quantity is 1).\n" +
                 "Examples:\n" +
                 "  - item-format: \"<white>[<aqua><name></aqua><yellow><amount></yellow>]</white>\" -> renders as [TNT x32] (brackets white, name aqua, amount yellow).\n" +
                 "  - item-format: \"<gray>[<raw_name><amount>]</gray>\" -> renders as [Chest x64] in vanilla rarity colors.\n" +
                 "  - item-format: \"\" (Default) -> renders the item's raw name directly without custom coloring or enclosing brackets.")
        public String itemFormat = "";

        @Comment("The MiniMessage format when player is holding nothing in main hand.")
        public String emptyHandFormat = "<gray><i>[Empty Hand]</i>";

        @Comment("Message sent to the player when they are on cooldown.")
        public String cooldownMessage = "<red>Please wait <cooldown>s before showcasing items again.";

        @Comment("Should the plugin display the item's sprite icon in chat next to the name? (Requires Minecraft 1.21.9+ or client supporting sprite component feature)")
        public boolean showIcon = false;

        @Comment("The MiniMessage format for the item icon. Use {atlas} and {sprite} for placeholders.")
        public String iconFormat = "<sprite:\"{atlas}\":\"{sprite}\"> ";

        @Comment("Should the player be allowed to showcase their shulker boxes?")
        public boolean shulkerPermissionRequired = true;
        public String shulkerPermissionNode = "chatitem.showcase.shulker";
        public int shulkerSnapshotTtlSeconds = 300;
        public int shulkerCacheMaxSize = 500;
    }

    @Comment("Inventory Showcase Settings\n" +
             "---------------------------\n" +
             "Allows players to showcase their current inventory state in chat via '[inv]' or '[inventory]'.\n" +
             "When clicked, viewers are presented with a read-only 54-slot chest GUI matching their profile.\n" +
             "To prevent item-theft exploits, all clicks and drags inside this GUI are cancelled.\n\n" +
             "Format Customization:\n" +
             "  The 'inventoryFormat' option supports the following placeholders:\n" +
             "    - <player_name>: The name of the player showcasing their inventory.\n" +
             "    - <player_head>: The player's 2D skin face sprite component (requires Minecraft 1.21.9+ atlas).\n" +
             "  Example:\n" +
             "    inventoryFormat: \"<white>[<player_head><green><player_name>'s Inventory</green>]</white>\"")
    public InventoryShowcase inventoryShowcase = new InventoryShowcase();

    @ConfigSerializable
    public static class InventoryShowcase {
        public boolean permissionRequired = true;
        public String permissionNode = "chatitem.showcase.inventory";
        public String inventoryFormat = "<white>[<player_head><green><player_name>'s Inventory</green>]</white>";
        public int inventorySnapshotTtlSeconds = 300;
        public int inventoryCacheMaxSize = 500;
    }

    @Comment("Ender Chest Showcase Settings\n" +
             "------------------------------\n" +
             "Allows players to showcase their ender chest contents in chat via '[echest]', '[ender]', or '[enderchest]'.\n" +
             "Placeholders:\n" +
             "  - <player_name>: The name of the player showcasing.\n" +
             "  - <player_head>: The player's 2D skin face icon.")
    public EnderChestShowcase enderChestShowcase = new EnderChestShowcase();

    @ConfigSerializable
    public static class EnderChestShowcase {
        public boolean permissionRequired = true;
        public String permissionNode = "chatitem.showcase.enderchest";
        public String enderChestFormat = "<white>[<player_head><light_purple><player_name>'s Ender Chest</light_purple>]</white>";
        public int enderChestSnapshotTtlSeconds = 300;
        public int enderChestCacheMaxSize = 500;
    }

    @Comment("Custom Messages (Adventure MiniMessage color tags supported)")
    public Messages messages = new Messages();

    @ConfigSerializable
    public static class Messages {
        @Comment("Message sent when an inventory snapshot has expired or doesn't exist.")
        public String expired = "<red>This inventory snapshot has expired or is invalid.";

        @Comment("Message sent when a player specifies an invalid snapshot format.")
        public String invalidToken = "<red>Invalid Snapshot Token structure Specified.";

        @Comment("Hover text shown when hovering over the [inv] chat tag.")
        public String invHover = "<gray>Click to view double-chest inventory window.";

        @Comment("Title of the showcase inventory GUI.")
        public String invTitle = "<dark_gray>{player}'s Inventory Snapshot";

        @Comment("Hover text shown when hovering over the [echest]/[ender] chat tag.")
        public String enderHover = "<gray>Click to view ender chest contents.";

        @Comment("Title of the showcase ender chest GUI.")
        public String enderTitle = "<dark_purple>{player}'s Ender Chest Snapshot";

        @Comment("Title of the showcase shulker box GUI.")
        public String shulkerTitle = "<light_purple>{player}'s Shulker Box Snapshot";

        @Comment("Message sent when a player attempts [shulker] without holding a shulker box.")
        public String noShulkerHeld = "<red>You must be holding a shulker box in your main hand.";
    }

    @Comment("Cache Settings")
    public CacheSettings cacheSettings = new CacheSettings();

    @ConfigSerializable
    public static class CacheSettings {
        @Comment("Time in hours before the custom skull texture cache is automatically cleared. Default is 168 (1 week).")
        public int skullCacheExpiryHours = 168;

        @Comment("Maximum number of custom skulls to keep in the cache before cleaning. Default is 1000.")
        public int skullCacheMaxSize = 1000;
    }
}
