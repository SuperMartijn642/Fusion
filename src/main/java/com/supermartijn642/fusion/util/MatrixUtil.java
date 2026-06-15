package com.supermartijn642.fusion.util;

import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;

/**
 * Created 16/06/2026 by SuperMartijn642
 */
public class MatrixUtil {

    public static Matrix4f createXRotationMatrix(float angle) {
        return new Matrix4f(Quaternion.fromXYZ(angle, 0, 0));
    }

    public static Matrix4f createYRotationMatrix(float angle) {
        return new Matrix4f(Quaternion.fromXYZ(0, angle, 0));
    }

    public static Matrix4f createZRotationMatrix(float angle) {
        return new Matrix4f(Quaternion.fromXYZ(0, 0, angle));
    }

    public static Matrix4f createZYXRotationMatrix(float x, float y, float z) {
        Matrix4f matrix = createZRotationMatrix(z);
        matrix.multiply(createYRotationMatrix(y));
        matrix.multiply(createXRotationMatrix(x));
        return matrix;
    }
}
