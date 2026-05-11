package com.supermartijn642.fusion.model;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Created 15/10/2024 by SuperMartijn642
 */
public interface CustomRenderTypeBakedModel {

    boolean canRenderInLayer(BlockState state, RenderType layer);
}
