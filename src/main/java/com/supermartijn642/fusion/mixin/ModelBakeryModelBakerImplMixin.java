package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.api.model.custom.ModelResolver;
import com.supermartijn642.fusion.api.model.custom.UntypedModelInstance;
import com.supermartijn642.fusion.model.FusionBlockModelData;
import net.minecraft.client.renderer.block.model.BlockModel;
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
        method = "bakeUncached(Lnet/minecraft/client/resources/model/UnbakedModel;Lnet/minecraft/client/resources/model/ModelState;Ljava/util/function/Function;)Lnet/minecraft/client/resources/model/BakedModel;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void bake(UnbakedModel unbakedModel, ModelState modelState, Function<Material,TextureAtlasSprite> spriteGetter, CallbackInfoReturnable<BakedModel> ci){
        if(!(unbakedModel instanceof BlockModel))
            return;
        // Handle Fusion models and models with Fusion textures
        if(FusionBlockModelData.containsFusionModelsOrTextures((BlockModel)unbakedModel)){
            FusionBlockModelData fusionData = FusionBlockModelData.get(unbakedModel);
            if(fusionData == null){
                ResourceLocation location = ResourceLocation.parse(((BlockModel)unbakedModel).name);
                UntypedModelInstance model = FusionBlockModelData.getModelInstance(unbakedModel);
                fusionData = new FusionBlockModelData(location, model);
                fusionData.resolveParents(l -> {
                    UnbakedModel m = this.this$0.unbakedCache.get(l);
                    return m == null ? this.this$0.unbakedCache.get(ModelResolver.MISSING_MODEL) : m;
                });
            }
            ci.setReturnValue(fusionData.bake((ModelBaker)this, spriteGetter, modelState));
        }
    }
}
