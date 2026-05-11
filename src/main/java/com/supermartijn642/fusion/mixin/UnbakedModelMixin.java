package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.model.FusionBlockModelData;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Created 25/05/2026 by SuperMartijn642
 */
@Mixin(UnbakedModel.class)
public interface UnbakedModelMixin {

    @Inject(
        method = "bakeWithTopModelValues",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void bakeWithTopModelValues(UnbakedModel unbakedModel, ModelBaker modelBaker, ModelState modelState, CallbackInfoReturnable<BakedModel> ci){
        FusionBlockModelData data = FusionBlockModelData.get(unbakedModel);
        if(data != null)
            ci.setReturnValue(data.bakeBlockModel(modelBaker, modelState));
    }
}
