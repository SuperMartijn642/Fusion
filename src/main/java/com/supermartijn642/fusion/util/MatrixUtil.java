package com.supermartijn642.fusion.util;

import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;

/**
 * Created 16/06/2026 by SuperMartijn642
 */
public class MatrixUtil {

    public static Matrix4f createXRotationMatrix(float angle){
        return new Matrix4f(new Quaternion((float)Math.sin(angle / 2), 0, 0, (float)Math.cos(angle / 2)));
    }

    public static Matrix4f createYRotationMatrix(float angle){
        return new Matrix4f(new Quaternion(0, (float)Math.sin(angle / 2), 0, (float)Math.cos(angle / 2)));
    }

    public static Matrix4f createZRotationMatrix(float angle){
        return new Matrix4f(new Quaternion(0, 0, (float)Math.sin(angle / 2), (float)Math.cos(angle / 2)));
    }

    public static Matrix4f createZYXRotationMatrix(float x, float y, float z){
        Matrix4f matrix = createZRotationMatrix(z);
        matrix.multiply(createYRotationMatrix(y));
        matrix.multiply(createXRotationMatrix(x));
        return matrix;
    }
}
