package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.model.modifiers.item.ItemModelModifierBakedModel;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Created 03/07/2026 by SuperMartijn642
 */
@Mixin(ItemModelShaper.class)
public class ItemModelShaperMixin {

    @Inject(
        method = "getItemModel",
        at = @At("RETURN"),
        cancellable = true
    )
    private void getItemModel(ItemStack stack, CallbackInfoReturnable<BakedModel> ci){
        BakedModel model = ci.getReturnValue();
        if(model instanceof ItemModelModifierBakedModel)
            ci.setReturnValue(((ItemModelModifierBakedModel)model).preselectModel(stack));
    }
}
