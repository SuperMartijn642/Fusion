package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierReloadListener;
import com.supermartijn642.fusion.model.modifiers.item.ItemModelModifierReloadListener;
import net.minecraft.client.resources.model.ModelBakery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Created 28/12/2024 by SuperMartijn642
 */
@Mixin(ModelBakery.class)
public class ModelBakeryMixin {

    @Inject(
        method = "bakeModels",
        at = @At("RETURN")
    )
    private void applyBlockModelOverlays(ModelBakery.TextureGetter textureGetter, CallbackInfoReturnable<ModelBakery.BakingResult> ci){
        ModelBakery.BakingResult results = ci.getReturnValue();
        ModelBakery.ModelBakerImpl resolver = ((ModelBakery)(Object)this).new ModelBakerImpl(textureGetter, () -> "Fusion Model Modifiers");
        BlockModelModifierReloadListener.INSTANCE.applyOverlays(results, resolver);
        ItemModelModifierReloadListener.INSTANCE.applyPredicateModels(results, resolver);
    }
}
