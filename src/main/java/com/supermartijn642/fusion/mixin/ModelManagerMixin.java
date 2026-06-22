package com.supermartijn642.fusion.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.supermartijn642.fusion.api.util.UserErrorException;
import com.supermartijn642.fusion.model.FusionBlockModelData;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierReloadListener;
import com.supermartijn642.fusion.model.modifiers.item.ItemModelModifierReloadListener;
import com.supermartijn642.fusion.util.LoggingHelper;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.SpecialBlockModelRenderer;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.Zone;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Created 19/09/2024 by SuperMartijn642
 */
@Mixin(ModelManager.class)
public class ModelManagerMixin {

    @Inject(
        method = "loadModels",
        at = @At("HEAD")
    )
    private static void captureBlockItemSprites(
        SpriteLoader.Preparations blockAtlas,
        ModelBakery bakery,
        Object2IntMap<BlockState> modelGroups,
        EntityModelSet entityModelSet,
        SpecialBlockModelRenderer specialBlockModelRenderer,
        Executor taskExecutor,
        CallbackInfoReturnable<CompletableFuture<?>> ci
    ){
        FusionBlockModelData.blockAtlasSprites = blockAtlas;
    }

    @Inject(
        method = "lambda$loadBlockModels$5(Lnet/minecraft/server/packs/resources/ResourceManager;)Ljava/util/Map;",
        at = @At("HEAD")
    )
    private static void reloadModelModifiers(ResourceManager resourceManager, CallbackInfoReturnable<Map<?,?>> ci){
        BlockModelModifierReloadListener.INSTANCE.reload(resourceManager);
        ItemModelModifierReloadListener.INSTANCE.reload(resourceManager);
    }

    @Inject(
        method = "discoverModelDependencies",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/ModelManager$ResolvedModels;<init>(Lnet/minecraft/client/resources/model/ResolvedModel;Ljava/util/Map;)V",
            shift = At.Shift.BEFORE
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void registerBlockModelOverlays(Map<ResourceLocation,UnbakedModel> models, BlockStateModelLoader.LoadedModels loadedModels, ClientItemInfoLoader.LoadedClientInfos clientInfos, CallbackInfoReturnable<ModelDiscovery> ci, Zone zone, ModelDiscovery modelDiscovery){
        modelDiscovery.addRoot(resolver -> {
            BlockModelModifierReloadListener.INSTANCE.registerModelDependencies(resolver);
            ItemModelModifierReloadListener.INSTANCE.registerModelDependencies(resolver);
        });
    }

    @Inject(
        method = "lambda$loadBlockModels$6(Ljava/util/Map$Entry;)Lcom/mojang/datafixers/util/Pair;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/model/BlockModel;fromStream(Ljava/io/Reader;)Lnet/minecraft/client/renderer/block/model/BlockModel;",
            shift = At.Shift.BEFORE
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void deserializeModel(Map.Entry<?,?> entry, CallbackInfoReturnable<?> ci, ResourceLocation name){
        // Store the model identifier, so the model can know its name
        FusionBlockModelData.CURRENT_MODEL.set(name);
    }

    @Inject(
        method = "lambda$loadBlockModels$6(Ljava/util/Map$Entry;)Lcom/mojang/datafixers/util/Pair;",
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
