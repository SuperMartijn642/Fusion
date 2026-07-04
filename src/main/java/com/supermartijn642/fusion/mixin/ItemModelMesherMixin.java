package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.model.modifiers.item.ItemModelModifierBakedModel;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Created 03/07/2026 by SuperMartijn642
 */
@Mixin(ItemModelMesher.class)
public class ItemModelMesherMixin {

    @Inject(
        method = "getItemModel",
        at = @At("RETURN"),
        cancellable = true
    )
    private void getItemModel(ItemStack stack, CallbackInfoReturnable<IBakedModel> ci){
        IBakedModel model = ci.getReturnValue();
        if(model instanceof ItemModelModifierBakedModel)
            ci.setReturnValue(((ItemModelModifierBakedModel)model).preselectModel(stack));
    }
}
