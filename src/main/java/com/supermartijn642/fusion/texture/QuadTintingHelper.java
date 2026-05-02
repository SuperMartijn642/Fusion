package com.supermartijn642.fusion.texture;

import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
public class QuadTintingHelper {

    /**
     * @see com.supermartijn642.fusion.api.texture.data.BaseTextureData.QuadTinting
     */
    private static final BlockTintSource[] TINT_FUNCTIONS = new BlockTintSource[]{
            BlockTintSources.grass(),
            BlockTintSources.foliage(),
            BlockTintSources.water()
    };

    static{
        if(TINT_FUNCTIONS.length != BaseTextureData.QuadTinting.values().length)
            throw new AssertionError("Missing tinting functions!");
    }

    public static int getColor(BaseTextureData.QuadTinting tinting, BlockState state, BlockAndTintGetter level, BlockPos pos){
        BlockTintSource tintFunction = TINT_FUNCTIONS[tinting.ordinal()];
        return ARGB.opaque(tintFunction.colorInWorld(state, level, pos));
    }
}
