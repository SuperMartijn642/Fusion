package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.model.FusionBlockModelData;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

/**
 * Created 26/05/2026 by SuperMartijn642
 */
@Mixin(ItemModelGenerator.class)
public class ItemModelGeneratorMixin {

    @Inject(
        method = "generateBlockModel",
        at = @At("HEAD"),
        cancellable = true
    )
    private void interceptFusionModels(Function<Material,TextureAtlasSprite> textureGetter, BlockModel model, CallbackInfoReturnable<BlockModel> ci){
        // Fusion models handle item generator model baking themselves
        if(FusionBlockModelData.get(model) != null)
            ci.setReturnValue(model);
    }
}
