package com.supermartijn642.fusion.util;

import net.minecraft.util.math.BlockPos;

/**
 * Created 08/07/2026 by SuperMartijn642
 */
public class SeedHelper {

    public static long fromBlockPos(BlockPos pos){
        // Copied from Minecraft
        long seed = pos.getX() * 3129871L ^ pos.getZ() * 116129781L ^ pos.getY();
        seed = seed * seed * 42317861L + seed * 11L;
        return seed >> 16;
    }
}
