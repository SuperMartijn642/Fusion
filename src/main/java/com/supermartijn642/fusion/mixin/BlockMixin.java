package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.model.types.connecting.ConnectingBakedModel;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.WeightedBakedModel;
import net.minecraft.util.BlockRenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Created 01/11/2023 by SuperMartijn642
 */
@Mixin(value = Block.class)
public class BlockMixin {

    @Inject(
        method = "canRenderInLayer",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void canRenderInLayer(IBlockState state, BlockRenderLayer renderType, CallbackInfoReturnable<Boolean> ci){ // Might not work for mods that overwrite this in their block class
        IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(state);
        if(model instanceof WeightedBakedModel)
            model = ((WeightedBakedModel)model).baseModel;
        if(model instanceof ConnectingBakedModel
            && !ConnectingBakedModel.ignoreModelRenderTypeCheck.get()
            && ((ConnectingBakedModel)model).getCustomRenderTypes().contains(renderType))
            ci.setReturnValue(true);
    }
}
