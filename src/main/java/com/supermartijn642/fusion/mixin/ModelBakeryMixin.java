package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.model.FusionBlockModelData;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Created 22/09/2024 by SuperMartijn642
 */
@Mixin(ModelBakery.class)
public class ModelBakeryMixin {

    @Inject(
        method = "loadModel",
        at = @At("HEAD")
    )
    private void storeBlockModelName(ResourceLocation identifier, CallbackInfoReturnable<?> ci){
        FusionBlockModelData.CURRENT_MODEL.set(identifier);
    }

    @Inject(
        method = "loadModel",
        at = @At("RETURN")
    )
    private void clearBlockModelName(ResourceLocation identifier, CallbackInfoReturnable<?> ci){
        FusionBlockModelData.CURRENT_MODEL.remove();
    }
}
