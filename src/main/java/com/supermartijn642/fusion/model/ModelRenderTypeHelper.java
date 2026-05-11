package com.supermartijn642.fusion.model;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.BlockRenderLayer;

/**
 * Created 13/01/2025 by SuperMartijn642
 */
public class ModelRenderTypeHelper {

    /**
     * Checks whether the given block state should render in the given layer without Fusion overwriting it.
     */
    public static boolean couldBlockRenderInLayerOriginally(IBlockState state, BlockRenderLayer layer){
        return state.getBlock().getBlockLayer() == layer;
    }

    public static boolean canRenderInLayer(IBakedModel model, IBlockState state, BlockRenderLayer layer, boolean defaultValue){
        if(state == null || layer == null)
            return defaultValue;
        return model instanceof CustomRenderTypeBakedModel ?
            ((CustomRenderTypeBakedModel)model).canRenderInLayer(state, layer) :
            defaultValue;
    }
}
