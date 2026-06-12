package com.supermartijn642.fusion.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.supermartijn642.fusion.api.model.custom.ModelResolver;
import com.supermartijn642.fusion.api.model.custom.UntypedModelInstance;
import com.supermartijn642.fusion.model.FusionBlockModelData;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Function;

/**
 * Created 11/06/2026 by SuperMartijn642
 */
@Mixin(ModelBakery.ModelBakerImpl.class)
public class ModelBakeryModelBakerImplMixin {

    @Final
    @Shadow
    private ModelBakery this$0;

    @WrapOperation(
        method = "bake(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/client/resources/model/ModelState;Ljava/util/function/Function;)Lnet/minecraft/client/resources/model/BakedModel;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/UnbakedModel;bake(Lnet/minecraft/client/resources/model/ModelBaker;Ljava/util/function/Function;Lnet/minecraft/client/resources/model/ModelState;Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/resources/model/BakedModel;"
        )
    )
    private BakedModel bake(UnbakedModel unbakedModel, ModelBaker self, Function<Material,TextureAtlasSprite> spriteGetter, ModelState modelState, ResourceLocation location, Operation<BakedModel> original){
        // Handle Fusion models and models with Fusion textures
        if(FusionBlockModelData.containsFusionModelsOrTextures(unbakedModel)){
            FusionBlockModelData fusionData = FusionBlockModelData.get(unbakedModel);
            if(fusionData == null){
                UntypedModelInstance model = FusionBlockModelData.getModelInstance(unbakedModel);
                fusionData = new FusionBlockModelData(location, model);
                fusionData.resolveParents(l -> {
                    UnbakedModel m = this.this$0.unbakedCache.get(l);
                    return m == null ? this.this$0.unbakedCache.get(ModelResolver.MISSING_MODEL) : m;
                });
            }
            return fusionData.bake(self, spriteGetter, modelState, location);
        }
        return original.call(unbakedModel, self, spriteGetter, modelState, location);
    }
}
