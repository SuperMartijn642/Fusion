package com.supermartijn642.fusion.util;

import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;

import javax.vecmath.Matrix4f;

/**
 * Created 16/06/2026 by SuperMartijn642
 */
public class MatrixUtil {

    public static Matrix4f createTranslationMatrix(float x, float y, float z){
        Matrix4f matrix = new Matrix4f();
        matrix.setIdentity();
        matrix.m03 = x;
        matrix.m13 = y;
        matrix.m23 = z;
        return matrix;
    }

    public static Matrix4f createScalingMatrix(float x, float y, float z){
        Matrix4f matrix = new Matrix4f();
        matrix.setIdentity();
        matrix.m00 = x;
        matrix.m11 = y;
        matrix.m22 = z;
        return matrix;
    }

    public static Matrix4f createXRotationMatrix(float angle){
        Matrix4f matrix = new Matrix4f();
        matrix.rotX(angle);
        return matrix;
    }

    public static Matrix4f createYRotationMatrix(float angle){
        Matrix4f matrix = new Matrix4f();
        matrix.rotY(angle);
        return matrix;
    }

    public static Matrix4f createZRotationMatrix(float angle){
        Matrix4f matrix = new Matrix4f();
        matrix.rotZ(angle);
        return matrix;
    }

    public static Matrix4f createZYXRotationMatrix(float x, float y, float z){
        Matrix4f matrix = createZRotationMatrix(z);
        matrix.mul(createYRotationMatrix(y));
        matrix.mul(createXRotationMatrix(x));
        return matrix;
    }

    public static void applyRotationToVector(Quaternion rotation, Vector3f vector){
        Quaternion quaternion = new Quaternion(rotation);
        Quaternion.mul(quaternion, new Quaternion(vector.x, vector.y, vector.z, 0), quaternion);
        Quaternion negated = new Quaternion(rotation);
        negated.negate();
        Quaternion.mul(quaternion, negated, quaternion);
        vector.set(quaternion.x, quaternion.y, quaternion.z);
    }
}
