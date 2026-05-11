package com.supermartijn642.fusion.model;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.util.BlockRenderLayer;

/**
 * Created 13/01/2025 by SuperMartijn642
 */
public class ModelRenderTypeHelper {

    /**
     * Checks whether the given block state should render in the given layer without Fusion overwriting it.
     */
    public static boolean couldBlockRenderInLayerOriginally(BlockState state, BlockRenderLayer layer){
        return state.getBlock().canRenderInLayer(state, layer);
    }

    public static boolean canRenderInLayer(IBakedModel model, BlockState state, BlockRenderLayer layer, boolean defaultValue){
        if(state == null || layer == null)
            return defaultValue;
        return model instanceof CustomRenderTypeBakedModel ?
            ((CustomRenderTypeBakedModel)model).canRenderInLayer(state, layer) :
            defaultValue;
    }
}
