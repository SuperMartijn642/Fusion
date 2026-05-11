package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.model.FusionBlockModelData;
import net.minecraft.client.resources.model.MissingBlockModel;
import net.minecraft.client.resources.model.ModelBakery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 27/05/2026 by SuperMartijn642
 */
@Mixin(ModelBakery.class)
public class ModelBakeryMixin {

    @Inject(
        method = "bakeModels",
        at = @At("HEAD")
    )
    private void bakeMissingModel(ModelBakery.TextureGetter textureGetter, CallbackInfo ci){
        //noinspection DataFlowIssue
        ModelBakery modelBakery = (ModelBakery)(Object)this;
        ModelBakery.ModelBakerImpl modelBaker = modelBakery.new ModelBakerImpl(textureGetter, MissingBlockModel.VARIANT);
        FusionBlockModelData.missingModel = modelBaker.bake(MissingBlockModel.LOCATION, ModelTransform.identity().toModelState());
    }
}
