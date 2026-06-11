package com.supermartijn642.fusion.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.supermartijn642.fusion.api.model.custom.UntypedModelInstance;
import com.supermartijn642.fusion.model.FusionBlockModelData;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierReloadListener;
import com.supermartijn642.fusion.model.modifiers.item.ItemModelModifierReloadListener;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;

/**
 * Created 28/12/2024 by SuperMartijn642
 */
@Mixin(ModelBakery.class)
public class ModelBakeryMixin {

    @Final
    @Shadow
    private EntityModelSet entityModelSet;

    @WrapOperation(
        method = "lambda$bakeModels$8(Lnet/minecraft/client/resources/model/ModelBakery$TextureGetter;Ljava/util/Map;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/client/resources/model/UnbakedModel;)V",
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

    @Inject(
        method = "bakeModels",
        at = @At("RETURN")
    )
    private void applyBlockModelOverlays(ModelBakery.TextureGetter textureGetter, CallbackInfoReturnable<ModelBakery.BakingResult> ci){
        // Ignore non-vanilla model bakeries
        //noinspection ConstantValue,EqualsBetweenInconvertibleTypes
        if(!this.getClass().equals(ModelBakery.class))
            return;

        ModelBakery.BakingResult results = ci.getReturnValue();

        // Make sure model maps are mutable
        boolean blockModelsMutable = results.blockStateModels() instanceof HashMap;
        boolean itemModelsMutable = results.itemStackModels() instanceof HashMap;
        if(!blockModelsMutable || !itemModelsMutable){
            results = new ModelBakery.BakingResult(
                results.missingModel(),
                blockModelsMutable ? results.blockStateModels() : new HashMap<>(results.blockStateModels()),
                results.missingItemModel(),
                itemModelsMutable ? results.itemStackModels() : new HashMap<>(results.itemStackModels()),
                results.itemProperties(),
                results.standaloneModels()
            );
        }

        // Apply Fusion model modifier
        ModelBakery.ModelBakerImpl resolver = ((ModelBakery)(Object)this).new ModelBakerImpl(textureGetter, () -> "Fusion Model Modifiers");
        BlockModelModifierReloadListener.INSTANCE.applyModelModifiers(results, resolver);
        ItemModelModifierReloadListener.INSTANCE.applyModelModifiers(results, new ItemModel.BakingContext(
            resolver,
            this.entityModelSet,
            results.missingItemModel()
        ));
    }
}
