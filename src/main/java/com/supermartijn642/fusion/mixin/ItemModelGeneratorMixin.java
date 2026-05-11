package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.model.FusionBlockModelData;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.texture.TextureMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Created 26/05/2026 by SuperMartijn642
 */
@Mixin(ItemModelGenerator.class)
public class ItemModelGeneratorMixin {

    @Inject(
        method = "makeItemModel",
        at = @At("HEAD"),
        cancellable = true
    )
    private void interceptFusionModels(TextureMap textureMap, ModelBlock model, CallbackInfoReturnable<ModelBlock> ci){
        // Fusion models handle item generator model baking themselves
        if(model instanceof FusionBlockModelData)
            ci.setReturnValue(model);
    }
}
