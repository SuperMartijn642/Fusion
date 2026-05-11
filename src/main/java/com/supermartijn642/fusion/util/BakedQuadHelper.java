package com.supermartijn642.fusion.util;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.math.Vector3f;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * Created 22/05/2026 by SuperMartijn642
 */
public class BakedQuadHelper {

    private static final VertexFormat VERTEX_FORMAT = DefaultVertexFormat.BLOCK;
    private static final int VERTEX_SIZE = VERTEX_FORMAT.getVertexSize() / 4;
    private static final int POSITION = findComponentOffset(DefaultVertexFormat.ELEMENT_POSITION);
    private static final int COLOR = findComponentOffset(DefaultVertexFormat.ELEMENT_COLOR);
    private static final int UV = findComponentOffset(DefaultVertexFormat.ELEMENT_UV0);
    private static final int LIGHTING = findComponentOffset(DefaultVertexFormat.ELEMENT_UV2);
    private static final int NORMAL = findComponentOffset(DefaultVertexFormat.ELEMENT_NORMAL);

    private static int findComponentOffset(VertexFormatElement component){
        int elementIndex = VERTEX_FORMAT.getElements().indexOf(component);
        if(elementIndex == -1)
            throw new AssertionError("Baked quad is missing vertex component '" + component + "'!");
        return VERTEX_FORMAT.getOffset(elementIndex) / 4; // Offset is in bytes, so divide by 4
    }

    public static int[] createVertices(){
        return new int[VERTEX_SIZE * 4];
    }

    public static float getPosX(int[] vertices, int vertex){
        int offset = vertex * VERTEX_SIZE + POSITION;
        return Float.intBitsToFloat(vertices[offset]);
    }

    public static void setPosX(int[] vertices, int vertex, float x){
        int offset = vertex * VERTEX_SIZE + POSITION;
        vertices[offset] = Float.floatToIntBits(x);
    }

    public static float getPosY(int[] vertices, int vertex){
        int offset = vertex * VERTEX_SIZE + POSITION;
        return Float.intBitsToFloat(vertices[offset + 1]);
    }

    public static void setPosY(int[] vertices, int vertex, float y){
        int offset = vertex * VERTEX_SIZE + POSITION;
        vertices[offset + 1] = Float.floatToIntBits(y);
    }

    public static float getPosZ(int[] vertices, int vertex){
        int offset = vertex * VERTEX_SIZE + POSITION;
        return Float.intBitsToFloat(vertices[offset + 2]);
    }

    public static void setPosZ(int[] vertices, int vertex, float z){
        int offset = vertex * VERTEX_SIZE + POSITION;
        vertices[offset + 2] = Float.floatToIntBits(z);
    }

    public static Vector3f getPosition(int[] vertices, int vertex, @Nullable Vector3f dest){
        if(dest == null)
            dest = new Vector3f();
        int offset = vertex * VERTEX_SIZE + POSITION;
        dest.set(
            Float.intBitsToFloat(vertices[offset]),
            Float.intBitsToFloat(vertices[offset + 1]),
            Float.intBitsToFloat(vertices[offset + 2])
        );
        return dest;
    }

    public static void setPosition(int[] vertices, int vertex, Vector3f pos){
        int offset = vertex * VERTEX_SIZE + POSITION;
        vertices[offset] = Float.floatToIntBits(pos.x());
        vertices[offset + 1] = Float.floatToIntBits(pos.y());
        vertices[offset + 2] = Float.floatToIntBits(pos.z());
    }

    public static int getColor(int[] vertices, int vertex){
        int offset = vertex * VERTEX_SIZE + COLOR;
        return vertices[offset];
    }

    public static void setColor(int[] vertices, int vertex, int color){
        int offset = vertex * VERTEX_SIZE + COLOR;
        vertices[offset] = color;
    }

    public static float getU(int[] vertices, int vertex){
        int offset = vertex * VERTEX_SIZE + UV;
        return Float.intBitsToFloat(vertices[offset]);
    }

    public static void setU(int[] vertices, int vertex, float u){
        int offset = vertex * VERTEX_SIZE + UV;
        vertices[offset] = Float.floatToIntBits(u);
    }

    public static float getV(int[] vertices, int vertex){
        int offset = vertex * VERTEX_SIZE + UV;
        return Float.intBitsToFloat(vertices[offset + 1]);
    }

    public static void setV(int[] vertices, int vertex, float v){
        int offset = vertex * VERTEX_SIZE + UV;
        vertices[offset + 1] = Float.floatToIntBits(v);
    }

    public static float[] getUV(int[] vertices, int vertex, @Nullable float[] dest){
        if(dest == null)
            dest = new float[2];
        int offset = vertex * VERTEX_SIZE + UV;
        dest[0] = Float.intBitsToFloat(vertices[offset]);
        dest[1] = Float.intBitsToFloat(vertices[offset + 1]);
        return dest;
    }

    public static void setUV(int[] vertices, int vertex, float[] uv){
        int offset = vertex * VERTEX_SIZE + UV;
        vertices[offset] = Float.floatToIntBits(uv[0]);
        vertices[offset + 1] = Float.floatToIntBits(uv[1]);
    }

    public static int getLighting(int[] vertices, int vertex){
        int offset = vertex * VERTEX_SIZE + LIGHTING;
        return vertices[offset];
    }

    public static void setLighting(int[] vertices, int vertex, int lighting){
        int offset = vertex * VERTEX_SIZE + LIGHTING;
        vertices[offset] = lighting;
    }

    public static float getNormalX(int[] vertices, int vertex){
        int offset = vertex * VERTEX_SIZE + NORMAL;
        return (vertices[offset] & 0xFF) / 127f;
    }

    public static void setNormalX(int[] vertices, int vertex, float x){
        int offset = vertex * VERTEX_SIZE + NORMAL;
        vertices[offset] = Float.floatToIntBits(x);
    }

    public static float getNormalY(int[] vertices, int vertex){
        int offset = vertex * VERTEX_SIZE + NORMAL;
        return ((vertices[offset] >> 8) & 0xFF) / 127f;
    }

    public static void setNormalY(int[] vertices, int vertex, float y){
        int offset = vertex * VERTEX_SIZE + NORMAL;
        vertices[offset] = Float.floatToIntBits(y);
    }

    public static float getNormalZ(int[] vertices, int vertex){
        int offset = vertex * VERTEX_SIZE + NORMAL;
        return ((vertices[offset] >> 16) & 0xFF) / 127f;
    }

    public static void setNormalZ(int[] vertices, int vertex, float z){
        int offset = vertex * VERTEX_SIZE + NORMAL;
        vertices[offset] = Float.floatToIntBits(z);
    }

    public static Vector3f getNormal(int[] vertices, int vertex, @Nullable Vector3f dest){
        if(dest == null)
            dest = new Vector3f();
        int offset = vertex * VERTEX_SIZE + NORMAL;
        dest.set(
            (vertices[offset] & 0xFF) / 127f,
            ((vertices[offset] >> 8) & 0xFF) / 127f,
            ((vertices[offset] >> 16) & 0xFF) / 127f
        );
        return dest;
    }

    public static void setNormal(int[] vertices, int vertex, Vector3f normal){
        int offset = vertex * VERTEX_SIZE + NORMAL;
        vertices[offset] = Float.floatToIntBits(normal.x());
        vertices[offset + 1] = Float.floatToIntBits(normal.y());
        vertices[offset + 2] = Float.floatToIntBits(normal.z());
    }

    public static Direction calculateFacing(Vector3f pos0, Vector3f pos1, Vector3f pos2){
        Vector3f v1to0 = new Vector3f(pos0.x(), pos0.y(), pos0.z());
        v1to0.sub(pos1);
        Vector3f v1to2 = new Vector3f(pos2.x(), pos2.y(), pos2.z());
        v1to2.sub(pos1);
        Vector3f perpendicular = new Vector3f(v1to2.x(), v1to2.y(), v1to2.z());
        perpendicular.cross(v1to0);
        perpendicular.normalize();
        if(Math.abs(perpendicular.x()) > Float.MAX_VALUE || Math.abs(perpendicular.y()) > Float.MAX_VALUE || Math.abs(perpendicular.z()) > Float.MAX_VALUE)
            return null;

        Direction closestDirection = null;
        float bestDotProduct = 0;
        for(Direction direction : Direction.values()){
            float dot = perpendicular.dot(direction.step());
            if(dot >= 0 && dot > bestDotProduct){
                bestDotProduct = dot;
                closestDirection = direction;
            }
        }
        return closestDirection;
    }
}
