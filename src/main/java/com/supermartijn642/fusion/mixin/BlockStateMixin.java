package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.model.CustomRenderTypeBakedModel;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.model.IBakedModel;
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
        if(model instanceof CustomRenderTypeBakedModel && ((CustomRenderTypeBakedModel)model).canRenderInLayer(state, renderType))
            return true;
        return this.getBlockState().getBlock().canRenderInLayer(this.getBlockState(), renderType);
    }
}
