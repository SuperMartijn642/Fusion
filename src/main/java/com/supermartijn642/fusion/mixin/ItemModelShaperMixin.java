package com.supermartijn642.fusion.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.supermartijn642.fusion.model.modifiers.item.ItemModelModifierBakedModel;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
@Mixin(ItemModelShaper.class)
public class ItemModelShaperMixin {

    @ModifyReturnValue(
        method = "getItemModel(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/client/resources/model/BakedModel;",
        at = @At("RETURN")
    )
    public BakedModel resolveItemPredicatesModel(BakedModel model, ItemStack stack){
        if(model instanceof ItemModelModifierBakedModel)
            return ((ItemModelModifierBakedModel)model).forStack(stack);
        return model;
    }
}
