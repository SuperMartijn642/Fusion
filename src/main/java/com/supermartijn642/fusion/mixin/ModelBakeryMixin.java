package com.supermartijn642.fusion.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.supermartijn642.fusion.api.model.custom.UntypedModelInstance;
import com.supermartijn642.fusion.api.util.UserErrorException;
import com.supermartijn642.fusion.model.FusionBlockModelData;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierReloadListener;
import com.supermartijn642.fusion.model.modifiers.item.ItemModelModifierReloadListener;
import com.supermartijn642.fusion.util.LoggingHelper;
import net.minecraft.client.renderer.texture.AtlasSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashSet;
import java.util.function.Function;

/**
 * Created 22/09/2024 by SuperMartijn642
 */
@Mixin(value = ModelBakery.class, priority = 1001)
public class ModelBakeryMixin {

    @Shadow
    private AtlasSet atlasSet;

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

    @WrapOperation(
        method = "bake(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/client/resources/model/ModelState;)Lnet/minecraft/client/resources/model/BakedModel;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/UnbakedModel;bake(Lnet/minecraft/client/resources/model/ModelBakery;Ljava/util/function/Function;Lnet/minecraft/client/resources/model/ModelState;Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/resources/model/BakedModel;"
        )
    )
    private BakedModel bake(UnbakedModel unbakedModel, ModelBakery self, Function<Material,TextureAtlasSprite> spriteGetter, ModelState modelState, ResourceLocation location, Operation<BakedModel> original){
        // Handle Fusion models and models with Fusion textures
        if(FusionBlockModelData.containsFusionModelsOrTextures(unbakedModel, this.atlasSet)){
            FusionBlockModelData fusionData = FusionBlockModelData.get(unbakedModel);
            if(fusionData == null){
                UntypedModelInstance model = FusionBlockModelData.getModelInstance(unbakedModel);
                fusionData = new FusionBlockModelData(location, model);
                FusionBlockModelData.gatherBlockModelMaterials(fusionData, l -> {
                    UnbakedModel m = self.unbakedCache.get(l);
                    return m == null ? self.unbakedCache.get(l) : m;
                }, new LinkedHashSet<>());
            }
            return fusionData.bake(self, spriteGetter, modelState, location);
        }
        return original.call(unbakedModel, self, spriteGetter, modelState, location);
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

    @WrapWithCondition(
        method = "getModel(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/resources/model/UnbakedModel;",
        at = @At(
            value = "INVOKE",
            target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;[Ljava/lang/Object;)V"
        ),
        expect = 1
    )
    private boolean interceptFusionErrors(Logger logger, String message, Object[] arguments){
        // Report Fusion model user errors in a more readable way
        Exception e = (Exception)arguments[2];
        if(e instanceof UserErrorException){
            LoggingHelper.logUserError(e.getCause(), "Failed to load model '%s':", arguments[0]);
            return false;
        }
        return true;
    }
}
