package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.FusionClient;
import net.minecraft.client.renderer.feature.BlockFeatureRenderer;
import net.minecraft.util.Unit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 22/08/2025 by SuperMartijn642
 */
@Mixin(BlockFeatureRenderer.class)
public class BlockRenderDispatcherMixin {

    @Inject(
        method = "renderBreakingBlockModelSubmits",
        at = @At("HEAD")
    )
    private void renderBreakingTextureHead(CallbackInfo ci){
        FusionClient.IS_RENDERING_BREAKING_OVERLAY.set(Unit.INSTANCE);
    }

    @Inject(
        method = "renderBreakingBlockModelSubmits",
        at = @At("TAIL")
    )
    private void renderBreakingTextureTail(CallbackInfo ci){
        FusionClient.IS_RENDERING_BREAKING_OVERLAY.remove();
    }
}
