package com.supermartijn642.fusion.texture;

import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.FoliageColors;
import net.minecraft.world.GrassColors;
import net.minecraft.world.ILightReader;
import net.minecraft.world.biome.BiomeColors;

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
                return GrassColors.get(0.5, 1.0);
            return BiomeColors.getAverageGrassColor(level, pos);
        },
        (state, level, pos, tintIndex) -> {
            if(level == null || pos == null)
                return FoliageColors.getDefaultColor();
            return BiomeColors.getAverageFoliageColor(level, pos);
        },
        (state, level, pos, tintIndex) -> {
            if(level == null || pos == null)
                return -1;
            return BiomeColors.getAverageWaterColor(level, pos);
        }
    };

    static{
        if(TINT_FUNCTIONS.length != BaseTextureData.QuadTinting.values().length)
            throw new AssertionError("Missing tinting functions!");
    }

    public static int getColor(BaseTextureData.QuadTinting tinting, BlockState state, ILightReader level, BlockPos pos){
        IBlockColor tintFunction = TINT_FUNCTIONS[tinting.ordinal()];
        return tintFunction.getColor(state, level, pos, 0) | 0xff000000;
    }
}
