package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.FusionClient;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 16/06/2026 by SuperMartijn642
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(
        method = "submitBlockDestroyAnimation",
        at = @At("HEAD")
    )
    private void submitBlockDestroyAnimationHead(CallbackInfo ci){
        FusionClient.IS_RENDERING_BREAKING_OVERLAY.set(true);
    }

    @Inject(
        method = "submitBlockDestroyAnimation",
        at = @At("HEAD")
    )
    private void submitBlockDestroyAnimationTail(CallbackInfo ci){
        FusionClient.IS_RENDERING_BREAKING_OVERLAY.remove();
    }
}
