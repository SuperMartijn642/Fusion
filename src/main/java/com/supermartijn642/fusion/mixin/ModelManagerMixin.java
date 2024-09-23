package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.model.modifiers.BlockModelModifierReloadListener;
import com.supermartijn642.fusion.model.modifiers.item.ItemModelModifierReloadListener;
import net.minecraft.client.resources.model.AtlasSet;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * Created 19/09/2024 by SuperMartijn642
 */
@Mixin(ModelManager.class)
public class ModelManagerMixin {

    @Inject(
        method = "loadModels",
        at = @At("HEAD")
    )
    private void registerBlockModelOverlays(ProfilerFiller profiler, Map<ResourceLocation,AtlasSet.StitchResult> textures, ModelBakery modelBakery, CallbackInfoReturnable<?> ci){
        BlockModelModifierReloadListener.INSTANCE.registerOverlays(modelBakery);
        ItemModelModifierReloadListener.INSTANCE.registerPredicateModels(modelBakery);
    }

    @Inject(
        method = "loadModels",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/ModelBakery;getBakedTopLevelModels()Ljava/util/Map;",
            shift = At.Shift.AFTER,
            ordinal = 1
        )
    )
    private void applyBlockModelOverlays(ProfilerFiller profiler, Map<ResourceLocation,AtlasSet.StitchResult> textures, ModelBakery modelBakery, CallbackInfoReturnable<?> ci){
        BlockModelModifierReloadListener.INSTANCE.applyOverlays(modelBakery);
        ItemModelModifierReloadListener.INSTANCE.applyPredicateModels(modelBakery);
    }
}
