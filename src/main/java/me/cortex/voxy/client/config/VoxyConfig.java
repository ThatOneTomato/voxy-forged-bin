package me.cortex.voxy.client.config;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.cortex.voxy.common.Logger;
import me.cortex.voxy.common.util.cpu.CpuLayout;
import me.cortex.voxy.commonImpl.VoxyCommon;
import net.neoforged.fml.loading.FMLPaths;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;

public class VoxyConfig {
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .excludeFieldsWithModifiers(Modifier.PRIVATE)
            .create();

    public static VoxyConfig CONFIG = loadOrCreate();

    public boolean enabled = true;
    public boolean enableRendering = true;
    public boolean ingestEnabled = true;
    public int sectionRenderDistance = 16;
    public int serviceThreads = (int) Math.max(CpuLayout.getCoreCount()/1.5, 1);
    public float subDivisionSize = 64;
    public boolean useEnvironmentalFog = true;
    public boolean dontUseSodiumBuilderThreads = true;

    // Runtime toggle for the LOD colour/brightness fix, so it can be compared against raw brightness live.
    // Toggled in-game via "/voxy colorfix" (not exposed in the config screen). true = brightness fix applied
    // (no-shader ~0.955, shader ~1.025), false = raw brightness (1.0, no fix). Read live by MDICSectionRenderer
    // and uploaded to the LOD shaders as uColorFix.
    public boolean colorFix = true;

    // Runtime toggle for the world-curvature fix (seamless start at the vanilla edge + camera-relative, smooth
    // tracking). Toggled in-game via "/voxy curvefix". true = fixed curve, false = original curve (measured from
    // the section origin, so it cuts at the boundary and snaps every 32 blocks). Uploaded to the shader as uCurveFix.
    public boolean curveFix = true;

    // LOD boundary buffer: extra INWARD shrink of each vanilla chunk's occlusion box, on top of the
    // 1-block margin outline.vsh already adds to match Sodium's render test. 0 is correct now that the
    // outermost-ring cull (MixinRenderSectionManager) + exact occlusion alignment (ChunkBoundRenderer)
    // handle the vanilla<->LOD boundary; any overlap (>0) only makes flat water z-fight at the seam.
    // Range: 0-4 blocks.
    public int lodBoundaryBuffer = 0;

    // World curvature: simulates standing on a spherical planet
    // 0 = disabled (flat world)
    // 1 = real Earth curvature (6371km radius)
    // Higher values = more extreme curvature (smaller planet effect)
    // Range: 0, or 50-5000 (values 1-49 are invalid and auto-corrected to 50)
    // Inspired by Distant Horizons' earth curvature feature
    public int earthCurveRatio = 0;

    // Central-band LOD culling: width (% of the monitor's pixel width) of a screen-centred, full-height
    // column that keeps rendering vanilla chunks. Everything to the LEFT/RIGHT of that column renders as
    // Voxy LOD instead of vanilla. The column is sized off the MONITOR width, so at 100% the band is at
    // least as wide as the game window and nothing changes (default). Lower values shrink the vanilla
    // column and turn the side "wings" into LOD - handy for ultrawide setups. Range: 30-100 (100 = off).
    public int lodCenterWidthPct = 100;

    // Distant generation: background-generate chunks beyond the vanilla render distance on the
    // integrated server (singleplayer/LAN host) and ingest them into the LOD store. Chunks are
    // generated only to the LIGHT status (blocks+biomes+light, no entities/ticking) in expanding
    // rings around the player, throttled by server MSPT and the ingest backlog.
    public boolean distantGenEnabled = true;
    // Radius in chunks around the player to generate. 96 chunks = 1536 blocks.
    public int distantGenRadius = 96;
    // How many chunks may generate concurrently. Higher = faster but more server load.
    public int distantGenMaxInFlight = 16;
    // Only start new chunks while the server's average tick time is below this (milliseconds).
    public int distantGenMaxMspt = 45;

    private static VoxyConfig loadOrCreate() {
        if (VoxyCommon.isAvailable()) {
            var path = getConfigPath();
            if (Files.exists(path)) {
                try (FileReader reader = new FileReader(path.toFile())) {
                    var conf = GSON.fromJson(reader, VoxyConfig.class);
                    if (conf != null) {
                        conf.save();
                        return conf;
                    } else {
                        Logger.error("Failed to load voxy config, resetting");
                    }
                } catch (IOException e) {
                    Logger.error("Could not parse config", e);
                }
            }
            var config = new VoxyConfig();
            config.save();
            return config;
        } else {
            var config = new VoxyConfig();
            config.enabled = false;
            config.enableRendering = false;
            return config;
        }
    }

    public void save() {
        try {
            Files.writeString(getConfigPath(), GSON.toJson(this));
        } catch (IOException e) {
            Logger.error("Failed to write config file", e);
        }
    }

    private static Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get()
                .resolve("voxy-config.json");
    }

    public boolean isRenderingEnabled() {
        return VoxyCommon.isAvailable() && this.enabled && this.enableRendering;
    }
}
