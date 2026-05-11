package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.model.FusionBlockModelData;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiFunction;

/**
 * Created 27/05/2026 by SuperMartijn642
 */
@Mixin(ModelBakery.class)
public class ModelBakeryMixin {

    @Inject(
        method = "bakeModels",
        at = @At("HEAD")
    )
    private void bakeMissingModel(BiFunction<ResourceLocation,Material,TextureAtlasSprite> textureGetter, CallbackInfo ci){
        //noinspection DataFlowIssue
        ModelBakery modelBakery = (ModelBakery)(Object)this;
        ModelBakery.ModelBakerImpl modelBaker = modelBakery.new ModelBakerImpl(textureGetter, ModelBakery.MISSING_MODEL_LOCATION);
        FusionBlockModelData.missingModel = Pair.of(
            modelBaker.getModel(ModelBakery.MISSING_MODEL_LOCATION),
            modelBaker.bake(ModelBakery.MISSING_MODEL_LOCATION, ModelTransform.identity().toModelState())
        );
    }
}
