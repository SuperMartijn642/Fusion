package com.supermartijn642.fusion.model;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.RenderType;

/**
 * Created 15/10/2024 by SuperMartijn642
 */
public interface CustomRenderTypeBakedModel {

    boolean canRenderInLayer(BlockState state, RenderType layer);
}
