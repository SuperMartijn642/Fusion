package com.supermartijn642.fusion.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.api.util.UserErrorException;
import com.supermartijn642.fusion.model.FusionBlockModelData;
import com.supermartijn642.fusion.model.FusionModelLoader;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierReloadListener;
import com.supermartijn642.fusion.model.modifiers.item.ItemModelModifierReloadListener;
import com.supermartijn642.fusion.util.LoggingHelper;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.SpecialBlockModelRenderer;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelDiscovery;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
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
        SpriteLoader.Preparations itemAtlas,
        ModelBakery bakery,
        Object2IntMap<BlockState> modelGroups,
        EntityModelSet entityModelSet,
        SpecialBlockModelRenderer specialBlockModelRenderer,
        Executor taskExecutor,
        CallbackInfoReturnable<CompletableFuture<?>> ci
    ){
        FusionBlockModelData.BLOCK_ITEM_ATLAS_SPRITES = Pair.of(blockAtlas, itemAtlas);
    }

    @Inject(
        method = "loadModels",
        at = @At("RETURN")
    )
    private static void releaseBlockItemSprites(
        SpriteLoader.Preparations blockAtlas,
        SpriteLoader.Preparations itemAtlas,
        ModelBakery bakery,
        Object2IntMap<BlockState> modelGroups,
        EntityModelSet entityModelSet,
        SpecialBlockModelRenderer specialBlockModelRenderer,
        Executor taskExecutor,
        CallbackInfoReturnable<CompletableFuture<?>> ci
    ){
        ci.getReturnValue().thenRun(() -> FusionBlockModelData.BLOCK_ITEM_ATLAS_SPRITES = null);
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
        method = "discoverModelDependencies(Ljava/util/Map;Lnet/minecraft/client/resources/model/BlockStateModelLoader$LoadedModels;Lnet/minecraft/client/resources/model/ClientItemInfoLoader$LoadedClientInfos;Lnet/neoforged/neoforge/client/model/standalone/StandaloneModelLoader$LoadedModels;)Lnet/minecraft/client/resources/model/ModelManager$ResolvedModels;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/ModelManager$ResolvedModels;<init>(Lnet/minecraft/client/resources/model/ResolvedModel;Ljava/util/Map;)V",
            shift = At.Shift.BEFORE
        )
    )
    private static void registerBlockModelOverlays(CallbackInfoReturnable<ModelDiscovery> ci, @Local ModelDiscovery modelDiscovery){
        modelDiscovery.addRoot(resolver -> {
            BlockModelModifierReloadListener.INSTANCE.registerModelDependencies(resolver);
            ItemModelModifierReloadListener.INSTANCE.registerModelDependencies(resolver);
        });
    }

    @Inject(
        method = "lambda$loadBlockModels$6(Ljava/util/Map$Entry;)Lcom/mojang/datafixers/util/Pair;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/neoforge/client/model/UnbakedModelParser;parse(Ljava/io/Reader;)Lnet/minecraft/client/resources/model/UnbakedModel;",
            shift = At.Shift.BEFORE
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void deserializeModel(Map.Entry<?,?> entry, CallbackInfoReturnable<?> ci, Identifier name){
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
    private static void interceptFusionErrors(Map.Entry<Identifier,?> entry, CallbackInfoReturnable<?> ci, @Local Exception e){
        // Report Fusion model user errors in a more readable way
        if(e instanceof UserErrorException || e instanceof FusionModelLoader.Marker){
            LoggingHelper.logUserError(e.getCause(), "Failed to load model '%s':", entry.getKey());
            ci.setReturnValue(null);
        }
    }
}
