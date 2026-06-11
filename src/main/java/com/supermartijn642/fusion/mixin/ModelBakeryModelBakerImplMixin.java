package com.supermartijn642.fusion.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.supermartijn642.fusion.api.model.custom.UntypedModelInstance;
import com.supermartijn642.fusion.model.FusionBlockModelData;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Created 11/06/2026 by SuperMartijn642
 */
@Mixin(ModelBakery.ModelBakerImpl.class)
public class ModelBakeryModelBakerImplMixin {

    @WrapOperation(
        method = "bake",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/UnbakedModel;bakeWithTopModelValues(Lnet/minecraft/client/resources/model/UnbakedModel;Lnet/minecraft/client/resources/model/ModelBaker;Lnet/minecraft/client/resources/model/ModelState;)Lnet/minecraft/client/resources/model/BakedModel;"
        )
    )
    private BakedModel bake(UnbakedModel unbakedModel, ModelBaker self, ModelState modelState, Operation<BakedModel> operation, @Local ResourceLocation location){
        // Handle Fusion models and models with Fusion textures
        if(FusionBlockModelData.containsFusionModelsOrTextures(unbakedModel)){
            FusionBlockModelData fusionData = FusionBlockModelData.get(unbakedModel);
            if(fusionData == null){
                UntypedModelInstance model = FusionBlockModelData.getModelInstance(unbakedModel);
                fusionData = new FusionBlockModelData(location, model);
            }
            return fusionData.bakeBlockModel(self, modelState);
        }
        return operation.call(unbakedModel, self, modelState);
    }
}
