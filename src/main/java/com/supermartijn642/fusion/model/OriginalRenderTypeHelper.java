package com.supermartijn642.fusion.model;

import net.minecraft.block.BlockState;
import net.minecraft.util.BlockRenderLayer;

/**
 * Created 13/01/2025 by SuperMartijn642
 */
public class OriginalRenderTypeHelper {

    /**
     * Checks whether the given block state should render in the given layer without Fusion overwriting it.
     */
    public static boolean couldBlockRenderInLayerOriginally(BlockState state, BlockRenderLayer layer){
        return state.getBlock().canRenderInLayer(state, layer);
    }
}
