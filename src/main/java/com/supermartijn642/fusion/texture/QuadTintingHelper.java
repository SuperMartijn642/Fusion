package com.supermartijn642.fusion.texture;

import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
public class QuadTintingHelper {

    /**
     * @see BaseTextureData.QuadTinting
     */
    private static final BlockColor[] TINT_FUNCTIONS = new BlockColor[]{
        (state, level, pos, tintIndex) -> {
            if(level == null || pos == null)
                return GrassColor.get(0.5, 1.0);
            return BiomeColors.getAverageGrassColor(level, pos);
        },
        (state, level, pos, tintIndex) -> {
            if(level == null || pos == null)
                return FoliageColor.getDefaultColor();
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

    public static int getColor(BaseTextureData.QuadTinting tinting, BlockState state, BlockAndTintGetter level, BlockPos pos){
        BlockColor tintFunction = TINT_FUNCTIONS[tinting.ordinal()];
        return tintFunction.getColor(state, level, pos, 0) | 0xff000000;
    }
}
