package com.supermartijn642.fusion.util;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Created 22/05/2026 by SuperMartijn642
 */
public class BakedQuadHelper {

    private static final VertexFormat VERTEX_FORMAT = DefaultVertexFormat.BLOCK;
    private static final int VERTEX_SIZE = VERTEX_FORMAT.getVertexSize() / 4;
    private static final int POSITION = findComponentOffset(VertexFormatElement.POSITION);
    private static final int COLOR = findComponentOffset(VertexFormatElement.COLOR);
    private static final int UV = findComponentOffset(VertexFormatElement.UV0);
    private static final int LIGHTING = findComponentOffset(VertexFormatElement.UV2);
    private static final int NORMAL = findComponentOffset(VertexFormatElement.NORMAL);

    private static int findComponentOffset(VertexFormatElement component){
        if(!VERTEX_FORMAT.contains(component))
            throw new AssertionError("Baked quad is missing vertex component '" + component + "'!");
        return VERTEX_FORMAT.getOffset(component) / 4; // Offset is in bytes, so divide by 4
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
        return dest.set(
            Float.intBitsToFloat(vertices[offset]),
            Float.intBitsToFloat(vertices[offset + 1]),
            Float.intBitsToFloat(vertices[offset + 2])
        );
    }

    public static void setPosition(int[] vertices, int vertex, Vector3fc pos){
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

    public static Vector2f getUV(int[] vertices, int vertex, @Nullable Vector2f dest){
        if(dest == null)
            dest = new Vector2f();
        int offset = vertex * VERTEX_SIZE + UV;
        return dest.set(
            Float.intBitsToFloat(vertices[offset]),
            Float.intBitsToFloat(vertices[offset + 1])
        );
    }

    public static void setUV(int[] vertices, int vertex, Vector2fc uv){
        int offset = vertex * VERTEX_SIZE + UV;
        vertices[offset] = Float.floatToIntBits(uv.x());
        vertices[offset + 1] = Float.floatToIntBits(uv.y());
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
        return dest.set(
            (vertices[offset] & 0xFF) / 127f,
            ((vertices[offset] >> 8) & 0xFF) / 127f,
            ((vertices[offset] >> 16) & 0xFF) / 127f
        );
    }

    public static void setNormal(int[] vertices, int vertex, Vector3fc normal){
        int offset = vertex * VERTEX_SIZE + NORMAL;
        vertices[offset] = Float.floatToIntBits(normal.x());
        vertices[offset + 1] = Float.floatToIntBits(normal.y());
        vertices[offset + 2] = Float.floatToIntBits(normal.z());
    }

    public static Direction calculateFacing(Vector3fc pos0, Vector3fc pos1, Vector3fc pos2){
        Vector3f v1to0 = new Vector3f(pos0).sub(pos1);
        Vector3f v1to2 = new Vector3f(pos2).sub(pos1);
        Vector3f perpendicular = new Vector3f(v1to2).cross(v1to0).normalize();
        if(!perpendicular.isFinite())
            return null;

        Direction closestDirection = null;
        float bestDotProduct = 0;
        for(Direction direction : Direction.values()){
            float dot = perpendicular.dot(direction.getStepX(), direction.getStepY(), direction.getStepZ());
            if(dot >= 0 && dot > bestDotProduct){
                bestDotProduct = dot;
                closestDirection = direction;
            }
        }
        return closestDirection;
    }
}
