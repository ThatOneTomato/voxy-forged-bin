package me.cortex.voxy;

import me.cortex.voxy.client.config.VoxyNeoForgeConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

/**
 * Main mod class for Voxy on NeoForge.
 *
 * Handles config registration and config screen setup.
 * Actual initialization happens via mixins (MixinRenderSystem).
 *
 * Voxy's settings are added to Sodium's Video Settings menu via the native Sodium config API
 * ({@link me.cortex.voxy.client.config.VoxyConfigMenu}, annotated with @ConfigEntryPointForge),
 * which is also what Reese's Sodium Options renders. A NeoForge config screen is registered here as
 * a fallback for when Sodium is somehow unavailable.
 */
@Mod("voxy")
public class Voxy {

    public Voxy(IEventBus modEventBus, ModContainer container) {
        // Only register client config on client side
        if (FMLLoader.getDist() == Dist.CLIENT) {
            // Register NeoForge config
            VoxyNeoForgeConfig.register(container);

            // Register the built-in NeoForge config screen (fallback - the primary UI is Sodium's
            // video settings, populated by VoxyConfigMenu via the Sodium config API)
            container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }
    }
}
