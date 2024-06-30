package io.wispforest.owo;

import io.wispforest.owo.client.screens.ScreenInternals;
import io.wispforest.owo.command.debug.OwoDebugCommands;
import io.wispforest.owo.compat.modmenu.OwoModMenuPlugin;
import io.wispforest.owo.extras.network.OwoInternalNetworking;
import io.wispforest.owo.network.OwoHandshake;
import io.wispforest.owo.ops.LootOps;
import io.wispforest.owo.text.CustomTextRegistry;
import io.wispforest.owo.text.InsertingTextContent;
import io.wispforest.owo.util.Wisdom;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;

import static io.wispforest.owo.ops.TextOps.withColor;

@Mod("owo")
public class Owo {

    /**
     * Whether oωo debug is enabled, this defaults to {@code true} in a development environment.
     * To override that behavior, add the {@code -Dowo.debug=false} java argument
     */
    public static final boolean DEBUG;
    public static final Logger LOGGER = LogManager.getLogger("owo");
    private static MinecraftServer SERVER;

    public static final Text PREFIX = Text.empty().formatted(Formatting.GRAY)
            .append(withColor("o", 0x3955e5))
            .append(withColor("ω", 0x13a6f0))
            .append(withColor("o", 0x3955e5))
            .append(Text.literal(" > ").formatted(Formatting.GRAY));

    static {
        boolean debug = !FMLLoader.isProduction();
        if (System.getProperty("owo.debug") != null) debug = Boolean.getBoolean("owo.debug");
        if (Boolean.getBoolean("owo.forceDisableDebug")) {
            LOGGER.warn("Deprecated system property 'owo.forceDisableDebug=true' was used - use 'owo.debug=false' instead");
            debug = false;
        }

        DEBUG = debug;
    }

    private final IEventBus eventBus;

    public Owo(IEventBus eventBus) {
        this.eventBus = eventBus;

        eventBus.addListener(this::onInitialize);

        eventBus.addListener((RegisterEvent event) -> {
            event.register(RegistryKeys.COMMAND_ARGUMENT_TYPE, (helper) -> {
                OwoDebugCommands.initArgumentTypes();
            });
        });

        eventBus.addListener(OwoInternalNetworking.INSTANCE::initializeNetworking);
    }

    public void onInitialize(FMLCommonSetupEvent event) {
        LootOps.registerListener();
        CustomTextRegistry.register(InsertingTextContent.TYPE, "index");
        ScreenInternals.init();

        OwoHandshake.init(this.eventBus);

        OwoModMenuPlugin.getProvidedConfigScreenFactories().forEach((s, iConfigScreenFactory) -> {
            ModList.get().getModContainerById(s).ifPresent(modContainer -> modContainer.registerExtensionPoint(IConfigScreenFactory.class, iConfigScreenFactory));
        });

        NeoForge.EVENT_BUS.addListener((ServerStartingEvent event1) -> SERVER = event1.getServer());
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event1) -> SERVER = null);

        Wisdom.spread();

        if (!DEBUG) return;

        OwoDebugCommands.register();
    }

    @ApiStatus.Internal
    public static void debugWarn(Logger logger, String message) {
        if (!DEBUG) return;
        logger.warn(message);
    }

    @ApiStatus.Internal
    public static void debugWarn(Logger logger, String message, Object... params) {
        if (!DEBUG) return;
        logger.warn(message, params);
    }

    /**
     * @return The currently active minecraft server instance. If running
     * on a physical client, this will return the integrated server while in
     * a local singleplayer world and {@code null} otherwise
     */
    public static MinecraftServer currentServer() {
        return SERVER;
    }

}