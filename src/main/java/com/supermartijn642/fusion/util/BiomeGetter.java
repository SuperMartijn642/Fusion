package com.supermartijn642.fusion.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Created 03/09/2026 by SuperMartijn642
 */
public interface BiomeGetter {

    @Nullable
    Identifier fusionGetBiome(BlockPos pos);
}
