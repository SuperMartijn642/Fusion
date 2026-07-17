package com.supermartijn642.fusion.util;

import net.minecraft.core.Direction;
import org.joml.Vector3f;

/**
 * Created 22/05/2026 by SuperMartijn642
 */
public class BakedQuadHelper {

    private static final ThreadLocal<Vector3f> HELPER = ThreadLocal.withInitial(Vector3f::new);

    public static Direction calculateFacing(float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2){
        Vector3f helper = HELPER.get();
        Vector3f perpendicular = helper.set(x2 - x1, y2 - y1, z2 - z1)
            .cross(x0 - x1, y0 - y1, z0 - z1)
            .normalize();
        if(!perpendicular.isFinite())
            return null;

        Direction closestDirection = null;
        float bestDotProduct = 0;
        for(Direction direction : Direction.values()){
            float dot = perpendicular.dot(direction.getUnitVec3f());
            if(dot >= 0 && dot > bestDotProduct){
                bestDotProduct = dot;
                closestDirection = direction;
            }
        }
        return closestDirection;
    }
}
