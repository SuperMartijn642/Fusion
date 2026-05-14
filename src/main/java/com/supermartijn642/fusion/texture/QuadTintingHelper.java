package com.supermartijn642.fusion.texture;

import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ColorizerFoliage;
import net.minecraft.world.ColorizerGrass;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeColorHelper;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
public class QuadTintingHelper {

    /**
     * @see BaseTextureData.QuadTinting
     */
    private static final IBlockColor[] TINT_FUNCTIONS = new IBlockColor[]{
        (state, level, pos, tintIndex) -> {
            if(level == null || pos == null)
                return ColorizerGrass.getGrassColor(0.5, 1.0);
            return BiomeColorHelper.getGrassColorAtPos(level, pos);
        },
        (state, level, pos, tintIndex) -> {
            if(level == null || pos == null)
                return ColorizerFoliage.getFoliageColorBasic();
            return BiomeColorHelper.getFoliageColorAtPos(level, pos);
        },
        (state, level, pos, tintIndex) -> {
            if(level == null || pos == null)
                return -1;
            return BiomeColorHelper.getWaterColorAtPos(level, pos);
        }
    };

    static{
        if(TINT_FUNCTIONS.length != BaseTextureData.QuadTinting.values().length)
            throw new AssertionError("Missing tinting functions!");
    }

    public static int getColor(BaseTextureData.QuadTinting tinting, IBlockState state, IBlockAccess level, BlockPos pos){
        IBlockColor tintFunction = TINT_FUNCTIONS[tinting.ordinal()];
        return tintFunction.colorMultiplier(state, level, pos, 0) | 0xff000000;
    }
}
