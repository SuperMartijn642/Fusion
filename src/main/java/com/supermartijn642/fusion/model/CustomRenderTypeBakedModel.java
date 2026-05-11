package com.supermartijn642.fusion.model;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;

/**
 * Created 15/10/2024 by SuperMartijn642
 */
public interface CustomRenderTypeBakedModel {

    boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer);
}
