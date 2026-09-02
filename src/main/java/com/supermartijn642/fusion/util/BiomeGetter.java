package com.supermartijn642.fusion.util;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Created 03/09/2026 by SuperMartijn642
 */
public interface BiomeGetter {

    @Nullable
    ResourceLocation fusionGetBiome(BlockPos pos);
}
