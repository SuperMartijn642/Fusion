package com.supermartijn642.fusion.util;

import net.minecraft.util.EnumFacing;
import org.jetbrains.annotations.Nullable;

/**
 * Created 12/05/2026 by SuperMartijn642
 */
public class CullingHelper {

    private static final EnumFacing[] CULL_DIRECTIONS = {null, EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST};

    public static EnumFacing[] cullDirections(){
        return CULL_DIRECTIONS;
    }

    public static int cullIndex(@Nullable EnumFacing cullDirection){
        return cullDirection == null ? 0 : cullDirection.ordinal() + 1;
    }

    @Nullable
    public static EnumFacing cullDirection(int cullIndex){
        return CULL_DIRECTIONS[cullIndex];
    }
}
