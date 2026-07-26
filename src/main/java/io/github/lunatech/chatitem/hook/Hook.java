package io.github.lunatech.chatitem.hook;

import io.github.lunatech.chatitem.hook.placeholderapi.PAPIHook;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Enum of all hooks used by the plugin.
 */
public enum Hook {
    PAPI(PAPIHook.class, "PlaceholderAPI", true);

    private final @NotNull Class<? extends AbstractHook> hookClass;
    private final @Nullable String pluginName;
    private final boolean optional;
    private AbstractHook loadedHook;

    Hook(@NotNull Class<? extends AbstractHook> hookClass, @Nullable String pluginName, boolean optional) {
        this.hookClass = hookClass;
        this.pluginName = pluginName;
        this.optional = optional;
    }

    @NotNull Class<? extends AbstractHook> getHookClass() {
        return hookClass;
    }

    public @Nullable String getPluginName() {
        return pluginName;
    }

    public boolean isOptional() {
        return optional;
    }

    public AbstractHook get() {
        if (loadedHook == null)
            throw new IllegalStateException("Hook has not been loaded yet.");

        return loadedHook;
    }

    public boolean isLoaded() {
        if (loadedHook != null)
            return loadedHook.isHookLoaded();

        return false;
    }

    @ApiStatus.Internal
    void setHook(@Nullable AbstractHook hook) {
        this.loadedHook = hook;
    }

    @ApiStatus.Internal
    void clearHook() {
        this.loadedHook = null;
    }

    @ApiStatus.Internal
    static void clearHooks() {
        for (Hook hooks : values())
            hooks.clearHook();
    }

    @NotNull
    public static PAPIHook getPAPIHook() {
        return (PAPIHook) Hook.PAPI.get();
    }
}
