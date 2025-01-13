package com.supermartijn642.fusion.model;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.IRegistryDelegate;

import java.util.Map;
import java.util.function.Predicate;

/**
 * Created 13/01/2025 by SuperMartijn642
 */
public class OriginalRenderTypeHelper {

    private static Map<IRegistryDelegate<Block>, Predicate<RenderType>> forgeRenderTypeChecks;

    /**
     * Checks whether the given block state should render in the given layer without Fusion overwriting it.
     */
    public static boolean couldBlockRenderInLayerOriginally(BlockState state, RenderType layer){
        Block block = state.getBlock();
        if(block instanceof LeavesBlock)
            return ItemBlockRenderTypes.renderCutout ? layer == RenderType.cutoutMipped() : layer == RenderType.solid();
        else{
            if(forgeRenderTypeChecks == null)
                forgeRenderTypeChecks = ItemBlockRenderTypes.getBlockLayerPredicatesView();
            return forgeRenderTypeChecks.get(block.delegate).test(layer);
        }
    }
}
