package me.cortex.voxy.client.mixin.sodium;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.cortex.voxy.client.config.VoxyConfig;
import me.cortex.voxy.client.core.IGetVoxyRenderSystem;
import me.cortex.voxy.commonImpl.VoxyCommon;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegionManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderRegionManager.class, remap = false)
public class MixinRenderRegionManager {
    // Suppresses Sodium's per-section chunk fade-in while Voxy LOD is active. Sodium 0.8 rewrote
    // uploadResults and no longer routes the fade timestamp through Math.toIntExact, so this target
    // is absent there; require = 0 lets the mixin apply gracefully (fade simply isn't suppressed)
    // instead of crashing, and re-activates automatically if a future Sodium restores the call.
    @Redirect(method = "uploadResults(Lnet/caffeinemc/mods/sodium/client/gl/device/CommandList;Lnet/caffeinemc/mods/sodium/client/render/chunk/region/RenderRegion;Ljava/util/Collection;)V", at = @At(value = "INVOKE", target = "Ljava/lang/Math;toIntExact(J)I"), remap = false, require = 0)
    private int voxy$cancelFade(long time) {
        var vrs = ((IGetVoxyRenderSystem)(Minecraft.getInstance().levelRenderer)).getVoxyRenderSystem();
        if (vrs!=null) {
            return -2;
        } else {
            return Math.toIntExact(time);
        }
    }
}