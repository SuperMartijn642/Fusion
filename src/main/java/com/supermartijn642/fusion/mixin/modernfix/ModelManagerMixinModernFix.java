package com.supermartijn642.fusion.mixin.modernfix;

import com.llamalad7.mixinextras.sugar.Local;
import com.supermartijn642.fusion.api.util.UserErrorException;
import com.supermartijn642.fusion.model.FusionBlockModelData;
import com.supermartijn642.fusion.model.FusionModelLoader;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierReloadListener;
import com.supermartijn642.fusion.model.modifiers.item.ItemModelModifierReloadListener;
import com.supermartijn642.fusion.util.LoggingHelper;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.SpecialBlockModelRenderer;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Map;

/**
 * Created 19/09/2024 by SuperMartijn642
 */
@Mixin(ModelManager.class)
public class ModelManagerMixinModernFix {

    @Inject(
        method = "loadModels",
        at = @At("HEAD")
    )
    private static void captureBlockItemSprites(
        ProfilerFiller profiler,
        Map<ResourceLocation,AtlasSet.StitchResult> atlasStitchResults,
        ModelBakery bakery,
        Object2IntMap<BlockState> modelGroups,
        EntityModelSet entityModelSet,
        SpecialBlockModelRenderer specialBlockModelRenderer,
        CallbackInfoReturnable<?> ci
    ){
        FusionBlockModelData.ATLAS_STITCH_RESULTS = atlasStitchResults;
    }

    @Inject(
        method = "lambda$loadBlockModels$9(Lnet/minecraft/server/packs/resources/ResourceManager;)Ljava/util/Map;",
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
    private static void registerBlockModelOverlays(CallbackInfoReturnable<ModelDiscovery> ci){
        ModelDiscovery modelDiscovery = ci.getReturnValue();
        UnbakedModel.Resolver resolver = modelDiscovery.new ResolverImpl();
        BlockModelModifierReloadListener.INSTANCE.registerModelDependencies(resolver);
        ItemModelModifierReloadListener.INSTANCE.registerModelDependencies(resolver);
    }

    @Inject(
        method = "lambda$loadBlockModels$10(Ljava/util/Map$Entry;)Lcom/mojang/datafixers/util/Pair;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/neoforge/client/model/UnbakedModelParser;parse(Ljava/io/Reader;)Lnet/minecraft/client/resources/model/UnbakedModel;",
            shift = At.Shift.BEFORE
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void deserializeModel(Map.Entry<?,?> entry, CallbackInfoReturnable<?> ci, ResourceLocation name){
        // Store the model identifier, so the model can know its name
        FusionBlockModelData.CURRENT_MODEL.set(name);
    }

    @Inject(
        method = "lambda$loadBlockModels$10(Ljava/util/Map$Entry;)Lcom/mojang/datafixers/util/Pair;",
        at = @At(
            value = "INVOKE",
            target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private static void interceptFusionErrors(Map.Entry<ResourceLocation,?> entry, CallbackInfoReturnable<?> ci, @Local Exception e){
        // Report Fusion model user errors in a more readable way
        if(e instanceof UserErrorException || e instanceof FusionModelLoader.Marker){
            LoggingHelper.logUserError(e.getCause(), "Failed to load model '%s':", entry.getKey());
            ci.setReturnValue(null);
        }
    }
}
