package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.model.overlays.BlockModelOverlayReloadListener;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.model.ModelManager;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 19/09/2024 by SuperMartijn642
 */
@Mixin(ModelManager.class)
public class ModelManagerMixin {

    @Inject(
        method = "apply",
        at = @At("HEAD")
    )
    private void registerBlockModelOverlays(ModelBakery modelBakery, IResourceManager resourceManager, IProfiler profiler, CallbackInfo ci){
        BlockModelOverlayReloadListener.INSTANCE.registerOverlays(modelBakery);
    }

    @Inject(
        method = "apply",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/model/ModelBakery;getBakedTopLevelModels()Ljava/util/Map;",
            shift = At.Shift.AFTER
        )
    )
    private void applyBlockModelOverlays(ModelBakery modelBakery, IResourceManager resourceManager, IProfiler profiler, CallbackInfo ci){
        BlockModelOverlayReloadListener.INSTANCE.applyOverlays(modelBakery);
    }
}
