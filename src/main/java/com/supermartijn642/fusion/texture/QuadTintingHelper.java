package com.supermartijn642.fusion.texture;

import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
public class QuadTintingHelper {

    /**
     * @see com.supermartijn642.fusion.api.texture.data.BaseTextureData.QuadTinting
     */
    private static final BlockColor[] TINT_FUNCTIONS = new BlockColor[] {
        // TODO
    };

    static {
        if(TINT_FUNCTIONS.length != BaseTextureData.QuadTinting.values().length)
            throw new AssertionError("Missing tinting functions!");
    }

    public static int getColor(BaseTextureData.QuadTinting tinting, BlockState state, BlockAndTintGetter level, BlockPos pos){
        BlockColor tintFunction = TINT_FUNCTIONS[tinting.ordinal()];
        return tintFunction.getColor(state, level, pos, 0);
    }
}
