package com.supermartijn642.fusion.util;

import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Created 22/05/2026 by SuperMartijn642
 */
public class BakedQuadHelper {

    private static final Map<VertexFormat,Offsets> VERTEX_ELEMENT_OFFSETS = new HashMap<>();
    private static final VertexFormat COMBINED_VERTEX_FORMAT = new VertexFormat()
        .addElement(DefaultVertexFormats.ELEMENT_POSITION)
        .addElement(DefaultVertexFormats.ELEMENT_COLOR)
        .addElement(DefaultVertexFormats.ELEMENT_UV0)
        .addElement(DefaultVertexFormats.ELEMENT_UV1)
        .addElement(DefaultVertexFormats.ELEMENT_NORMAL)
        .addElement(DefaultVertexFormats.ELEMENT_PADDING);

    private static Offsets getOffsets(VertexFormat format){
        return VERTEX_ELEMENT_OFFSETS.computeIfAbsent(format, Offsets::new);
    }

    private static int findComponentOffset(VertexFormat format, VertexFormatElement component, boolean required){
        int elementIndex = format.getElements().indexOf(component);
        if(elementIndex == -1){
            if(required)
                throw new AssertionError("Baked quad is missing vertex component '" + component + "'!");
            return -1;
        }
        return format.getOffset(elementIndex) / 4; // Offset is in bytes, so divide by 4
    }

    public static int[] createVertices(VertexFormat format){
        return new int[getOffsets(format).vertexSize * 4];
    }

    public static VertexFormat getCombinedVertexFormat(){
        return COMBINED_VERTEX_FORMAT;
    }

    public static float getPosX(VertexFormat format, int[] vertices, int vertex){
        Offsets offsets = getOffsets(format);
        int offset = vertex * offsets.vertexSize + offsets.position;
        return Float.intBitsToFloat(vertices[offset]);
    }

    public static void setPosX(VertexFormat format, int[] vertices, int vertex, float x){
        Offsets offsets = getOffsets(format);
        int offset = vertex * offsets.vertexSize + offsets.position;
        vertices[offset] = Float.floatToIntBits(x);
    }

    public static float getPosY(VertexFormat format, int[] vertices, int vertex){
        Offsets offsets = getOffsets(format);
        int offset = vertex * offsets.vertexSize + offsets.position;
        return Float.intBitsToFloat(vertices[offset + 1]);
    }

    public static void setPosY(VertexFormat format, int[] vertices, int vertex, float y){
        Offsets offsets = getOffsets(format);
        int offset = vertex * offsets.vertexSize + offsets.position;
        vertices[offset + 1] = Float.floatToIntBits(y);
    }

    public static float getPosZ(VertexFormat format, int[] vertices, int vertex){
        Offsets offsets = getOffsets(format);
        int offset = vertex * offsets.vertexSize + offsets.position;
        return Float.intBitsToFloat(vertices[offset + 2]);
    }

    public static void setPosZ(VertexFormat format, int[] vertices, int vertex, float z){
        Offsets offsets = getOffsets(format);
        int offset = vertex * offsets.vertexSize + offsets.position;
        vertices[offset + 2] = Float.floatToIntBits(z);
    }

    public static Vector3f getPosition(VertexFormat format, int[] vertices, int vertex, @Nullable Vector3f dest){
        if(dest == null)
            dest = new Vector3f();
        Offsets offsets = getOffsets(format);
        int offset = vertex * offsets.vertexSize + offsets.position;
        dest.set(
            Float.intBitsToFloat(vertices[offset]),
            Float.intBitsToFloat(vertices[offset + 1]),
            Float.intBitsToFloat(vertices[offset + 2])
        );
        return dest;
    }

    public static void setPosition(VertexFormat format, int[] vertices, int vertex, Vector3f pos){
        Offsets offsets = getOffsets(format);
        int offset = vertex * offsets.vertexSize + offsets.position;
        vertices[offset] = Float.floatToIntBits(pos.x());
        vertices[offset + 1] = Float.floatToIntBits(pos.y());
        vertices[offset + 2] = Float.floatToIntBits(pos.z());
    }

    public static int getColor(VertexFormat format, int[] vertices, int vertex){
        Offsets offsets = getOffsets(format);
        int offset = vertex * offsets.vertexSize + offsets.color;
        return vertices[offset];
    }

    public static void setColor(VertexFormat format, int[] vertices, int vertex, int color){
        Offsets offsets = getOffsets(format);
        int offset = vertex * offsets.vertexSize + offsets.color;
        vertices[offset] = color;
    }

    public static float getU(VertexFormat format, int[] vertices, int vertex){
        Offsets offsets = getOffsets(format);
        int offset = vertex * offsets.vertexSize + offsets.uv;
        return Float.intBitsToFloat(vertices[offset]);
    }

    public static void setU(VertexFormat format, int[] vertices, int vertex, float u){
        Offsets offsets = getOffsets(format);
        int offset = vertex * offsets.vertexSize + offsets.uv;
        vertices[offset] = Float.floatToIntBits(u);
    }

    public static float getV(VertexFormat format, int[] vertices, int vertex){
        Offsets offsets = getOffsets(format);
        int offset = vertex * offsets.vertexSize + offsets.uv;
        return Float.intBitsToFloat(vertices[offset + 1]);
    }

    public static void setV(VertexFormat format, int[] vertices, int vertex, float v){
        Offsets offsets = getOffsets(format);
        int offset = vertex * offsets.vertexSize + offsets.uv;
        vertices[offset + 1] = Float.floatToIntBits(v);
    }

    public static float[] getUV(VertexFormat format, int[] vertices, int vertex, @Nullable float[] dest){
        if(dest == null)
            dest = new float[2];
        Offsets offsets = getOffsets(format);
        int offset = vertex * offsets.vertexSize + offsets.uv;
        dest[0] = Float.intBitsToFloat(vertices[offset]);
        dest[1] = Float.intBitsToFloat(vertices[offset + 1]);
        return dest;
    }

    public static void setUV(VertexFormat format, int[] vertices, int vertex, float[] uv){
        Offsets offsets = getOffsets(format);
        int offset = vertex * offsets.vertexSize + offsets.uv;
        vertices[offset] = Float.floatToIntBits(uv[0]);
        vertices[offset + 1] = Float.floatToIntBits(uv[1]);
    }

    public static boolean hasLighting(VertexFormat format){
        return getOffsets(format).lighting != -1;
    }

    public static int getLighting(VertexFormat format, int[] vertices, int vertex){
        Offsets offsets = getOffsets(format);
        int offset = vertex * offsets.vertexSize + offsets.lighting;
        return vertices[offset];
    }

    public static void setLighting(VertexFormat format, int[] vertices, int vertex, int lighting){
        Offsets offsets = getOffsets(format);
        int offset = vertex * offsets.vertexSize + offsets.lighting;
        vertices[offset] = lighting;
    }

    public static boolean hasNormal(VertexFormat format){
        return getOffsets(format).normal != -1;
    }

    public static float getNormalX(VertexFormat format, int[] vertices, int vertex){
        Offsets offsets = getOffsets(format);
        int offset = vertex * offsets.vertexSize + offsets.normal;
        return (vertices[offset] & 0xFF) / 127f;
    }

    public static void setNormalX(VertexFormat format, int[] vertices, int vertex, float x){
        Offsets offsets = getOffsets(format);
        int offset = vertex * offsets.vertexSize + offsets.normal;
        vertices[offset] = Float.floatToIntBits(x);
    }

    public static float getNormalY(VertexFormat format, int[] vertices, int vertex){
        Offsets offsets = getOffsets(format);
        int offset = vertex * offsets.vertexSize + offsets.normal;
        return ((vertices[offset] >> 8) & 0xFF) / 127f;
    }

    public static void setNormalY(VertexFormat format, int[] vertices, int vertex, float y){
        Offsets offsets = getOffsets(format);
        int offset = vertex * offsets.vertexSize + offsets.normal;
        vertices[offset] = Float.floatToIntBits(y);
    }

    public static float getNormalZ(VertexFormat format, int[] vertices, int vertex){
        Offsets offsets = getOffsets(format);
        int offset = vertex * offsets.vertexSize + offsets.normal;
        return ((vertices[offset] >> 16) & 0xFF) / 127f;
    }

    public static void setNormalZ(VertexFormat format, int[] vertices, int vertex, float z){
        Offsets offsets = getOffsets(format);
        int offset = vertex * offsets.vertexSize + offsets.normal;
        vertices[offset] = Float.floatToIntBits(z);
    }

    public static Vector3f getNormal(VertexFormat format, int[] vertices, int vertex, @Nullable Vector3f dest){
        if(dest == null)
            dest = new Vector3f();
        Offsets offsets = getOffsets(format);
        int offset = vertex * offsets.vertexSize + offsets.normal;
        dest.set(
            (vertices[offset] & 0xFF) / 127f,
            ((vertices[offset] >> 8) & 0xFF) / 127f,
            ((vertices[offset] >> 16) & 0xFF) / 127f
        );
        return dest;
    }

    public static void setNormal(VertexFormat format, int[] vertices, int vertex, Vector3f normal){
        Offsets offsets = getOffsets(format);
        int offset = vertex * offsets.vertexSize + offsets.normal;
        vertices[offset] = Float.floatToIntBits(normal.x());
        vertices[offset + 1] = Float.floatToIntBits(normal.y());
        vertices[offset + 2] = Float.floatToIntBits(normal.z());
    }

    public static void fillNormals(VertexFormat format, int[] vertices)
    {
        // Calculate normal
        Vector3f v1  = getPosition(format, vertices, 1, null);
        Vector3f v2 = getPosition(format, vertices, 2, null);
        Vector3f v3 = getPosition(format, vertices, 3, null);
        v3.sub(v1);
        getPosition(format, vertices, 0, v1);
        v2.sub(v1);
        v2.cross(v3);
        v2.mul(1 / (float)Math.sqrt(v2.x() * v2.x() + v2.y() * v2.y() + v2.z() * v2.z()));
        // Fill vertices
        int x = ((byte) Math.round(v2.x() * 127)) & 0xFF;
        int y = ((byte) Math.round(v2.y() * 127)) & 0xFF;
        int z = ((byte) Math.round(v2.z() * 127)) & 0xFF;
        int normal = x | (y << 8) | (z << 16);
        Offsets offsets = getOffsets(format);
        for(int i = 0; i < 4; i++){
            int offset = i * offsets.vertexSize + offsets.normal;
            vertices[offset] = normal;
        }
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
            float dot = perpendicular.dot(new Vector3f(direction.getStepX(), direction.getStepY(), direction.getStepZ()));
            if(dot >= 0 && dot > bestDotProduct){
                bestDotProduct = dot;
                closestDirection = direction;
            }
        }
        return closestDirection;
    }

    private static class Offsets {
        private final int vertexSize;
        private final int position;
        private final int color;
        private final int uv;
        private final int lighting;
        private final int normal;

        Offsets(VertexFormat format){
            this.vertexSize = format.getVertexSize() / 4;
            this.position = findComponentOffset(format, DefaultVertexFormats.ELEMENT_POSITION, true);
            this.color = findComponentOffset(format, DefaultVertexFormats.ELEMENT_COLOR, true);
            this.uv = findComponentOffset(format, DefaultVertexFormats.ELEMENT_UV0, true);
            this.lighting = findComponentOffset(format, DefaultVertexFormats.ELEMENT_UV1, false);
            this.normal = findComponentOffset(format, DefaultVertexFormats.ELEMENT_NORMAL, false);
        }
    }
}
