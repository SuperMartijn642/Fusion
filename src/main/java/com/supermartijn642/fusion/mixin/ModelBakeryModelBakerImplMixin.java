package com.supermartijn642.fusion.mixin;

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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

/**
 * Created 11/06/2026 by SuperMartijn642
 */
@Mixin(ModelBakery.ModelBakerImpl.class)
public class ModelBakeryModelBakerImplMixin {

    @Final
    @Shadow
    private ModelBakery this$0;

    @Inject(
        method = "bake(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/client/resources/model/ModelState;Ljava/util/function/Function;)Lnet/minecraft/client/resources/model/BakedModel;",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/client/resources/model/ModelBakery$ModelBakerImpl;getModel(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/resources/model/UnbakedModel;",
            shift = At.Shift.AFTER
        ),
        cancellable = true
    )
    private void bake(ResourceLocation location, ModelState modelState, Function<Material,TextureAtlasSprite> spriteGetter, CallbackInfoReturnable<BakedModel> ci){
        UnbakedModel unbakedModel = this.this$0.unbakedCache.get(location);
        if(unbakedModel == null)
            return;
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
            BakedModel baked = fusionData.bake((ModelBaker)this, spriteGetter, modelState, location);
            // Add baked model to the cache
            ModelBakery.BakedCacheKey key = new ModelBakery.BakedCacheKey(location, modelState.getRotation(), modelState.isUvLocked());
            this.this$0.bakedCache.put(key, baked);
            ci.setReturnValue(baked);
        }
    }
}
