package com.supermartijn642.fusion.model.types.base;

import net.minecraft.block.BlockState;
import net.minecraft.util.BlockRenderLayer;

/**
 * Created 15/10/2024 by SuperMartijn642
 */
public interface CustomRenderTypeBakedModel {

    boolean canRenderInLayer(BlockState state, BlockRenderLayer layer);
}
