package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.model.FusionBlockModelData;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierReloadListener;
import com.supermartijn642.fusion.model.modifiers.item.ItemModelModifierReloadListener;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.ref.WeakReference;

/**
 * Created 19/09/2024 by SuperMartijn642
 */
@Mixin(ModelManager.class)
public class ModelManagerMixin {

    @Inject(
        method = "prepare",
        at = @At("RETURN")
    )
    private void captureModelBakery(ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfoReturnable<ModelBakery> ci){
        // Store model bakery reference
        FusionBlockModelData.modelBakery = new WeakReference<>(ci.getReturnValue());
    }

    @Inject(
        method = "prepare",
        at = @At("HEAD")
    )
    private void loadBlockModelOverlays(ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfoReturnable<ModelBakery> ci){
        BlockModelModifierReloadListener.INSTANCE.reload(resourceManager);
        ItemModelModifierReloadListener.INSTANCE.reload(resourceManager);
    }

    @Inject(
        method = "apply",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/ModelBakery;getBakedTopLevelModels()Ljava/util/Map;",
            shift = At.Shift.AFTER
        )
    )
    private void applyBlockModelOverlays(ModelBakery modelBakery, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci){
        BlockModelModifierReloadListener.INSTANCE.applyOverlays(modelBakery);
        ItemModelModifierReloadListener.INSTANCE.applyPredicateModels(modelBakery);
    }
}
