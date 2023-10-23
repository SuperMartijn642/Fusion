package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.model.types.connecting.ConnectingBakedModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.WeightedBakedModel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Created 01/11/2023 by SuperMartijn642
 */
@Mixin(ItemBlockRenderTypes.class)
public class ItemBlockRenderTypesMixin {

    @Inject(
        method = "canRenderInLayer(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/renderer/RenderType;)Z",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void canRenderInLayer(BlockState state, RenderType renderType, CallbackInfoReturnable<Boolean> ci){
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        if(model instanceof WeightedBakedModel)
            model = ((WeightedBakedModel)model).wrapped;
        if(model instanceof ConnectingBakedModel
            && !ConnectingBakedModel.ignoreModelRenderTypeCheck.get()
            && ((ConnectingBakedModel)model).getCustomRenderTypes().contains(renderType))
            ci.setReturnValue(true);
    }
}
