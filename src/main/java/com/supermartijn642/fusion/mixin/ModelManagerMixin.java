package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.model.FusionBlockModel;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierReloadListener;
import com.supermartijn642.fusion.model.modifiers.item.ItemModelModifierReloadListener;
import net.minecraft.client.resources.model.ModelDiscovery;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
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
public class ModelManagerMixin {

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
        BlockModelModifierReloadListener.INSTANCE.registerOverlays(resolver);
        ItemModelModifierReloadListener.INSTANCE.registerPredicateModels(resolver);
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
        FusionBlockModel.CURRENT_MODEL.set(name);
    }
}
