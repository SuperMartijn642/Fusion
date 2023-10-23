package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.model.types.connecting.ConnectingBakedModel;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.WeightedBakedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Created 01/11/2023 by SuperMartijn642
 */
@Mixin(RenderTypeLookup.class)
public class ItemBlockRenderTypesMixin {

    @Inject(
        method = "canRenderInLayer(Lnet/minecraft/block/BlockState;Lnet/minecraft/client/renderer/RenderType;)Z",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void canRenderInLayer(BlockState state, RenderType renderType, CallbackInfoReturnable<Boolean> ci){
        IBakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        if(model instanceof WeightedBakedModel)
            model = ((WeightedBakedModel)model).wrapped;
        if(model instanceof ConnectingBakedModel
            && !ConnectingBakedModel.ignoreModelRenderTypeCheck.get()
            && ((ConnectingBakedModel)model).getCustomRenderTypes().contains(renderType))
            ci.setReturnValue(true);
    }
}
