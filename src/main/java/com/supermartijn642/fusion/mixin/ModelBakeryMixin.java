package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.api.model.custom.UntypedModelInstance;
import com.supermartijn642.fusion.api.util.UserErrorException;
import com.supermartijn642.fusion.model.FusionBlockModelData;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierReloadListener;
import com.supermartijn642.fusion.model.modifiers.item.ItemModelModifierReloadListener;
import com.supermartijn642.fusion.util.LoggingHelper;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.SpriteMap;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.TransformationMatrix;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.function.Function;

/**
 * Created 22/09/2024 by SuperMartijn642
 */
@Mixin(ModelBakery.class)
public class ModelBakeryMixin {

    @Shadow
    private SpriteMap atlasSet;
    @Final
    @Shadow
    private Map<Triple<ResourceLocation,TransformationMatrix,Boolean>,IBakedModel> bakedCache;

    @Inject(
        method = "loadTopLevel",
        at = @At("HEAD")
    )
    private void registerBlockModelOverlays(ModelResourceLocation location, CallbackInfo ci){
        if(location == ModelBakery.MISSING_MODEL_LOCATION){
            //noinspection DataFlowIssue
            ModelBakery modelBakery = (ModelBakery)(Object)this;
            BlockModelModifierReloadListener.INSTANCE.registerModelDependencies(modelBakery);
            ItemModelModifierReloadListener.INSTANCE.registerModelDependencies(modelBakery);
        }
    }

    @Inject(
        method = "getBakedModel(Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/client/renderer/model/IModelTransform;Ljava/util/function/Function;)Lnet/minecraft/client/renderer/model/IBakedModel;",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/client/renderer/model/ModelBakery;getModel(Lnet/minecraft/util/ResourceLocation;)Lnet/minecraft/client/renderer/model/IUnbakedModel;",
            shift = At.Shift.AFTER
        ),
        cancellable = true
    )
    private void bake(ResourceLocation location, IModelTransform modelState, Function<RenderMaterial,TextureAtlasSprite> spriteGetter, CallbackInfoReturnable<IBakedModel> ci){
        //noinspection DataFlowIssue
        ModelBakery modelBakery = (ModelBakery)(Object)this;
        IUnbakedModel unbakedModel = modelBakery.unbakedCache.get(location);
        if(unbakedModel == null)
            return;
        // Handle Fusion models and models with Fusion textures
        if(FusionBlockModelData.containsFusionModelsOrTextures(unbakedModel, this.atlasSet)){
            FusionBlockModelData fusionData = FusionBlockModelData.get(unbakedModel);
            if(fusionData == null){
                UntypedModelInstance model = FusionBlockModelData.getModelInstance(unbakedModel);
                fusionData = new FusionBlockModelData(location, model);
                FusionBlockModelData.gatherBlockModelMaterials(fusionData, l -> {
                    IUnbakedModel m = modelBakery.unbakedCache.get(l);
                    return m == null ? modelBakery.unbakedCache.get(l) : m;
                }, new LinkedHashSet<>());
            }
            IBakedModel baked = fusionData.bake(modelBakery, spriteGetter, modelState, location);
            // Add baked model to the cache
            Triple<ResourceLocation,TransformationMatrix,Boolean> key = Triple.of(location, modelState.getRotation(), modelState.isUvLocked());
            this.bakedCache.put(key, baked);
            ci.setReturnValue(baked);
        }
    }

    @Inject(
        method = "loadBlockModel",
        at = @At("HEAD")
    )
    private void storeBlockModelName(ResourceLocation identifier, CallbackInfoReturnable<?> ci){
        FusionBlockModelData.CURRENT_MODEL.set(identifier);
    }

    @Inject(
        method = "loadBlockModel",
        at = @At("RETURN")
    )
    private void clearBlockModelName(ResourceLocation identifier, CallbackInfoReturnable<?> ci){
        FusionBlockModelData.CURRENT_MODEL.remove();
    }

    @Redirect(
        method = "lambda$uploadTextures$12(Lnet/minecraft/util/ResourceLocation;)V",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/Exception;printStackTrace()V"
        )
    )
    private void interceptFusionErrors(Exception e){
        if(e instanceof UserErrorException)
            return;
        //noinspection CallToPrintStackTrace
        e.printStackTrace();
    }

    @Redirect(
        method = "lambda$uploadTextures$12(Lnet/minecraft/util/ResourceLocation;)V",
        at = @At(
            value = "INVOKE",
            target = "Lorg/apache/logging/log4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"
        )
    )
    private void interceptFusionErrors(Logger logger, String message, Object identifier, Object exception){
        // Report Fusion model user errors in a more readable way
        Exception e = (Exception)exception;
        if(e instanceof UserErrorException){
            LoggingHelper.logUserError(e.getCause(), "Failed to load model '%s':", identifier);
            return;
        }
        logger.warn(message, identifier, exception);
    }
}
