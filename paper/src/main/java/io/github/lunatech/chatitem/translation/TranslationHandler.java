package io.github.lunatech.chatitem.translation;

import io.github.lunatech.chatitem.AbstractChatItem;
import io.github.lunatech.chatitem.Reloadable;
import io.github.lunatech.chatitem.config.ConfigHandler;
import io.github.milkdrinkers.colorparser.paper.ColorParser;
import io.github.milkdrinkers.wordweaver.Translation;
import io.github.milkdrinkers.wordweaver.config.TranslationConfig;

import java.nio.file.Path;

/**
 * A wrapper handler class for handling WordWeaver lifecycle.
 */
public class TranslationHandler implements Reloadable {
    private final ConfigHandler configHandler;

    public TranslationHandler(ConfigHandler configHandler) {
        this.configHandler = configHandler;
    }

    @Override
    public void onEnable(AbstractChatItem plugin) {
        Translation.initialize(TranslationConfig.builder() // Initialize word-weaver
            .translationDirectory(plugin.getDataPath().resolve("lang"))
            .resourcesDirectory(Path.of("lang"))
            .extractLanguages(true)
            .updateLanguages(true)
            .language(configHandler.getConfig().language)
            .defaultLanguage("en_US")
            .componentConverter(s -> ColorParser.of(s).build()) // Use color parser for components by default
            .build()
        );
    }
}
