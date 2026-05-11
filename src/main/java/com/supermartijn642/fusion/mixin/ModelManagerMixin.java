package com.supermartijn642.fusion.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.supermartijn642.fusion.api.util.UserErrorException;
import com.supermartijn642.fusion.model.FusionBlockModelData;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierReloadListener;
import com.supermartijn642.fusion.model.modifiers.item.ItemModelModifierReloadListener;
import com.supermartijn642.fusion.util.LoggingHelper;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
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
        method = "lambda$loadBlockModels$8(Lnet/minecraft/server/packs/resources/ResourceManager;)Ljava/util/Map;",
        at = @At("HEAD")
    )
    private static void reloadModelModifiers(ResourceManager resourceManager, CallbackInfoReturnable<Map<?,?>> ci){
        BlockModelModifierReloadListener.INSTANCE.reload(resourceManager);
        ItemModelModifierReloadListener.INSTANCE.reload(resourceManager);
    }

    @Inject(
        method = "discoverModelDependencies",
        at = @At("RETURN")
    )
    private void registerBlockModelOverlays(CallbackInfoReturnable<ModelDiscovery> ci){
        ModelDiscovery modelDiscovery = ci.getReturnValue();
        UnbakedModel.Resolver resolver = modelDiscovery.new ResolverImpl();
        BlockModelModifierReloadListener.INSTANCE.registerOverlays(resolver, modelDiscovery);
        ItemModelModifierReloadListener.INSTANCE.registerPredicateModels(resolver, modelDiscovery);
    }

    @Inject(
        method = "loadModels",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/ModelBakery;getBakedTopLevelModels()Ljava/util/Map;",
            shift = At.Shift.AFTER
        )
    )
    private void applyBlockModelOverlays(ProfilerFiller profiler, Map<ResourceLocation,AtlasSet.StitchResult> textures, ModelBakery modelBakery, Object2IntMap<?> map, CallbackInfoReturnable<?> ci){
        BlockModelModifierReloadListener.INSTANCE.applyOverlays(modelBakery);
        ItemModelModifierReloadListener.INSTANCE.applyPredicateModels(modelBakery);
    }

    @Inject(
        method = "lambda$loadBlockModels$9(Ljava/util/Map$Entry;)Lcom/mojang/datafixers/util/Pair;",
        at = @At("HEAD")
    )
    private static void storeBlockModelName(Map.Entry<ResourceLocation,?> entry, CallbackInfoReturnable<?> ci){
        FusionBlockModelData.CURRENT_MODEL.set(entry.getKey());
    }

    @Inject(
        method = "lambda$loadBlockModels$9(Ljava/util/Map$Entry;)Lcom/mojang/datafixers/util/Pair;",
        at = @At("RETURN")
    )
    private static void clearBlockModelName(Map.Entry<ResourceLocation,?> entry, CallbackInfoReturnable<?> ci){
        FusionBlockModelData.CURRENT_MODEL.remove();
    }

    @Inject(
        method = "lambda$loadBlockModels$9(Ljava/util/Map$Entry;)Lcom/mojang/datafixers/util/Pair;",
        at = @At(
            value = "INVOKE",
            target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private static void interceptFusionErrors(Map.Entry<ResourceLocation,?> entry, CallbackInfoReturnable<?> ci, @Local Exception e){
        // Report Fusion model user errors in a more readable way
        if(e instanceof UserErrorException){
            LoggingHelper.logUserError(e.getCause(), "Failed to load model '%s':", entry.getKey());
            ci.setReturnValue(null);
        }
    }
}
