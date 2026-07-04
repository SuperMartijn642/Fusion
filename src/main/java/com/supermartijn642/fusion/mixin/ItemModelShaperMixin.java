package com.supermartijn642.fusion.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.supermartijn642.fusion.model.modifiers.item.ItemModelModifierBakedModel;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Created 03/07/2026 by SuperMartijn642
 */
@Mixin(ItemModelShaper.class)
public class ItemModelShaperMixin {

    @ModifyReturnValue(
        method = "getItemModel",
        at = @At("RETURN")
    )
    public BakedModel getItemModel(BakedModel model, ItemStack stack){
        if(model instanceof ItemModelModifierBakedModel)
            return ((ItemModelModifierBakedModel)model).preselectModel(stack);
        return model;
    }
}
