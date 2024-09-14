package com.supermartijn642.fusion.texture;

import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
public class QuadTintingHelper {

    /**
     * @see com.supermartijn642.fusion.api.texture.data.BaseTextureData.QuadTinting
     */
    private static final IBlockColor[] TINT_FUNCTIONS = new IBlockColor[]{
        // TODO
    };

    static{
        if(TINT_FUNCTIONS.length != BaseTextureData.QuadTinting.values().length)
            throw new AssertionError("Missing tinting functions!");
    }

    public static int getColor(BaseTextureData.QuadTinting tinting, IBlockState state, IBlockAccess level, BlockPos pos){
        IBlockColor tintFunction = TINT_FUNCTIONS[tinting.ordinal()];
        return tintFunction.colorMultiplier(state, level, pos, 0);
    }
}
