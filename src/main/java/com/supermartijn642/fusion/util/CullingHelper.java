package com.supermartijn642.fusion.util;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * Created 12/05/2026 by SuperMartijn642
 */
public class CullingHelper {

    private static final Direction[] CULL_DIRECTIONS = {null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    public static Direction[] cullDirections(){
        return CULL_DIRECTIONS;
    }

    public static int cullIndex(@Nullable Direction cullDirection){
        return cullDirection == null ? 0 : cullDirection.ordinal() + 1;
    }

    @Nullable
    public static Direction cullDirection(int cullIndex){
        return CULL_DIRECTIONS[cullIndex];
    }
}
