package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.model.types.connecting.ConnectingBakedModel;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.WeightedBakedModel;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.common.extensions.IForgeBlockState;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Created 01/11/2023 by SuperMartijn642
 */
@Mixin(value = BlockState.class)
public class BlockStateMixin implements IForgeBlockState {
    @Override
    public boolean canRenderInLayer(BlockRenderLayer renderType){ // Overwriting this is not ideal for mod compat, but can't really find a better way to add Fusion's model check
        //noinspection DataFlowIssue
        BlockState state = (BlockState)(Object)this;
        IBakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        if(model instanceof WeightedBakedModel)
            model = ((WeightedBakedModel)model).wrapped;
        if(model instanceof ConnectingBakedModel
            && !ConnectingBakedModel.ignoreModelRenderTypeCheck.get()
            && ((ConnectingBakedModel)model).getCustomRenderTypes().contains(renderType))
            return true;

        return getBlockState().getBlock().canRenderInLayer(getBlockState(), renderType);
    }
}
