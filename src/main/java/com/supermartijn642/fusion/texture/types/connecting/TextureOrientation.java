package com.supermartijn642.fusion.texture.types.connecting;

import com.supermartijn642.fusion.api.model.custom.quad.EmittableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionDirection;
import net.minecraft.util.EnumFacing;

import javax.vecmath.Vector3f;
import java.util.Arrays;

/**
 * Created 13/05/2026 by SuperMartijn642
 */
public enum TextureOrientation {
    NORMAL_0(false, 0), NORMAL_90(false, 1), NORMAL_180(false, 2), NORMAL_270(false, 3),
    FLIPPED_0(true, 0), FLIPPED_90(true, 1), FLIPPED_180(true, 2), FLIPPED_270(true, 3);

    /**
     * Stores world space vector point in the up and right direction of the default texture orientation for each face
     */
    private static final int[][] DEFAULT_TEXTURE_ROTATIONS_UP = new int[6][];
    private static final int[][] DEFAULT_TEXTURE_ROTATIONS_RIGHT = new int[6][];

    static{
        DEFAULT_TEXTURE_ROTATIONS_UP[EnumFacing.DOWN.ordinal()] = new int[]{0, 0, 1};
        DEFAULT_TEXTURE_ROTATIONS_RIGHT[EnumFacing.DOWN.ordinal()] = new int[]{1, 0, 0};
        DEFAULT_TEXTURE_ROTATIONS_UP[EnumFacing.UP.ordinal()] = new int[]{0, 0, -1};
        DEFAULT_TEXTURE_ROTATIONS_RIGHT[EnumFacing.UP.ordinal()] = new int[]{1, 0, 0};
        DEFAULT_TEXTURE_ROTATIONS_UP[EnumFacing.NORTH.ordinal()] = new int[]{0, 1, 0};
        DEFAULT_TEXTURE_ROTATIONS_RIGHT[EnumFacing.NORTH.ordinal()] = new int[]{-1, 0, 0};
        DEFAULT_TEXTURE_ROTATIONS_UP[EnumFacing.SOUTH.ordinal()] = new int[]{0, 1, 0};
        DEFAULT_TEXTURE_ROTATIONS_RIGHT[EnumFacing.SOUTH.ordinal()] = new int[]{1, 0, 0};
        DEFAULT_TEXTURE_ROTATIONS_UP[EnumFacing.WEST.ordinal()] = new int[]{0, 1, 0};
        DEFAULT_TEXTURE_ROTATIONS_RIGHT[EnumFacing.WEST.ordinal()] = new int[]{0, 0, 1};
        DEFAULT_TEXTURE_ROTATIONS_UP[EnumFacing.EAST.ordinal()] = new int[]{0, 1, 0};
        DEFAULT_TEXTURE_ROTATIONS_RIGHT[EnumFacing.EAST.ordinal()] = new int[]{0, 0, -1};
    }

    public static TextureOrientation of(boolean flipped, int rotations){
        return TextureOrientation.values()[flipped ? 4 + rotations : rotations];
    }

    public static TextureOrientation findOrientation(QuadAccess quad){
        // First determine the texture orientation relative to the vertex indices
        // Compare the angle between directions v1 to v2 and v1 to v3, to check whether the texture is flipped
        double angle1to2 = Math.atan2(quad.v(1) - quad.v(0), quad.u(1) - quad.u(0)), angle1to3 = Math.atan2(quad.v(2) - quad.v(0), quad.u(2) - quad.u(0));
        boolean textureFlipped = (angle1to2 - angle1to3 + 4 * Math.PI) % (2 * Math.PI) < Math.PI;
        // Find the top-left-most-ish index, if we assume the uvs form a grid-aligned square this should work
        int topLeftMostIndex = 0;
        float minUV = quad.u(0) + quad.v(0);
        for(int i = 1; i < 4; i++){
            if(quad.u(i) + quad.v(i) < minUV){
                minUV = quad.u(i) + quad.v(i);
                topLeftMostIndex = i;
            }
        }
        int textureRotation = textureFlipped ? topLeftMostIndex : (4 - topLeftMostIndex) % 4;

        // Determine the vertex indices rotation relative to the block face
        Vector3f[] positions3d = {quad.position(0), quad.position(1), quad.position(2), quad.position(3)};
        // Project the 3d positions onto the plane perpendicular to the facing of the quad
        float[][] pos = new float[4][2];
        EnumFacing direction = quad.facing();
        for(int i = 0; i < 4; i++){
            if(direction == EnumFacing.DOWN){
                pos[i][0] = positions3d[i].x;
                pos[i][1] = -positions3d[i].z;
            }else if(direction == EnumFacing.UP){
                pos[i][0] = positions3d[i].x;
                pos[i][1] = positions3d[i].z;
            }else if(direction == EnumFacing.NORTH){
                pos[i][0] = -positions3d[i].x;
                pos[i][1] = -positions3d[i].y;
            }else if(direction == EnumFacing.SOUTH){
                pos[i][0] = positions3d[i].x;
                pos[i][1] = -positions3d[i].y;
            }else if(direction == EnumFacing.WEST){
                pos[i][0] = positions3d[i].z;
                pos[i][1] = -positions3d[i].y;
            }else if(direction == EnumFacing.EAST){
                pos[i][0] = -positions3d[i].z;
                pos[i][1] = -positions3d[i].y;
            }
        }
        // Compare the angle between directions v1 to v2 and v1 to v3, to check whether the texture is flipped
        angle1to2 = Math.atan2(pos[1][1] - pos[0][1], pos[1][0] - pos[0][0]);
        angle1to3 = Math.atan2(pos[2][1] - pos[0][1], pos[2][0] - pos[0][0]);
        boolean quadFlipped = (angle1to2 - angle1to3 + 4 * Math.PI) % (2 * Math.PI) < Math.PI;
        // Find the top-left-most-ish index, if we assume the uvs form an axis-aligned square this should work
        topLeftMostIndex = 0;
        for(int i = 1; i < 4; i++){
            float[] best = pos[topLeftMostIndex], current = pos[i];
            if(current[0] + current[1] < best[0] + best[1])
                topLeftMostIndex = i;
        }
        int quadRotation = textureFlipped ? topLeftMostIndex : (4 - topLeftMostIndex);

        // Combine the two, to get the in-world orientation of the texture
        boolean flipped = textureFlipped ^ quadFlipped;
        int rotation = quadFlipped ? (4 - textureRotation + quadRotation) % 4 : (textureRotation + quadRotation) % 4;
        return TextureOrientation.of(flipped, rotation);
    }

    public final boolean flipped;
    public final int rotations;
    /**
     * If {@code dir} is the in-world direction, {@code worldToTexture[dir.ordinal()]} is the texture space direction
     */
    public final ConnectionDirection[] worldToTexture;
    public final int[] vertexIndexPermutation;
    public final EmittableQuad.Transform transform, reverseTransform;

    TextureOrientation(boolean flipped, int rotations){
        this.flipped = flipped;
        this.rotations = rotations;
        this.transform = q -> {
            this.applyVertexPermutation(q);
            q.emit();
        };
        this.reverseTransform = q -> {
            this.applyInverseVertexPermutation(q);
            q.emit();
        };

        this.worldToTexture = ConnectionDirection.values();
        this.vertexIndexPermutation = new int[]{0, 3, 2, 1};
        // First apply flip
        if(flipped){
            this.worldToTexture[ConnectionDirection.TOP.ordinal()] = ConnectionDirection.LEFT;
            this.worldToTexture[ConnectionDirection.TOP_RIGHT.ordinal()] = ConnectionDirection.BOTTOM_LEFT;
            this.worldToTexture[ConnectionDirection.RIGHT.ordinal()] = ConnectionDirection.BOTTOM;
            this.worldToTexture[ConnectionDirection.LEFT.ordinal()] = ConnectionDirection.TOP;
            this.worldToTexture[ConnectionDirection.BOTTOM_LEFT.ordinal()] = ConnectionDirection.TOP_RIGHT;
            this.worldToTexture[ConnectionDirection.BOTTOM.ordinal()] = ConnectionDirection.RIGHT;
            this.vertexIndexPermutation[1] = 1;
            this.vertexIndexPermutation[3] = 3;
        }
        // Then apply rotation
        if(rotations != 0){
            ConnectionDirection[] old = Arrays.copyOf(this.worldToTexture, this.worldToTexture.length);
            for(int i = 0; i < 8; i++)
                this.worldToTexture[i] = old[(i - rotations * 2 + 8) % 8];
            int[] old2 = Arrays.copyOf(this.vertexIndexPermutation, this.vertexIndexPermutation.length);
            for(int i = 0; i < 4; i++)
                this.vertexIndexPermutation[i] = old2[(i + rotations + 4) % 4];
        }
    }

    public int[] transformWorldUpVector(EnumFacing face){
        return this.transformWorldVector(DEFAULT_TEXTURE_ROTATIONS_UP[face.ordinal()], face);
    }

    public int[] transformWorldRightVector(EnumFacing face){
        return this.transformWorldVector(DEFAULT_TEXTURE_ROTATIONS_RIGHT[face.ordinal()], face);
    }

    public int[] transformWorldVector(int[] vector, EnumFacing face){ // TODO improve this
        if(!this.flipped && this.rotations == 0)
            return vector;
        int[] newVector = Arrays.copyOf(vector, vector.length);
        EnumFacing.Axis axis = face.getAxis();
        boolean positive = face.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE;
        if(this.flipped){
            if(face.getAxis() == EnumFacing.Axis.X){
                newVector[1] = positive ? vector[2] : -vector[2];
                newVector[2] = positive ? vector[1] : -vector[1];
            }
            if(face.getAxis() == EnumFacing.Axis.Y){
                newVector[0] = positive ? vector[2] : -vector[2];
                newVector[2] = positive ? vector[0] : -vector[0];
            }
            if(face.getAxis() == EnumFacing.Axis.Z){
                newVector[0] = positive ? -vector[1] : vector[1];
                newVector[1] = positive ? -vector[0] : vector[0];
            }
        }
        if(this.rotations > 0){
            if(this.rotations == 2){
                if(axis != EnumFacing.Axis.X)
                    newVector[0] = -newVector[0];
                if(axis != EnumFacing.Axis.Y)
                    newVector[1] = -newVector[1];
                if(axis != EnumFacing.Axis.Z)
                    newVector[2] = -newVector[2];
            }else{
                int oldX = newVector[0];
                int oldY = newVector[1];
                if(axis != EnumFacing.Axis.X)
                    newVector[0] = ((positive ^ this.rotations == 3 ^ this.flipped) ? 1 : -1) * (axis == EnumFacing.Axis.Y ? -newVector[2] : newVector[1]);
                if(axis != EnumFacing.Axis.Y)
                    newVector[1] = ((positive ^ this.rotations == 3 ^ this.flipped) ? 1 : -1) * (axis == EnumFacing.Axis.Z ? -oldX : newVector[2]);
                if(axis != EnumFacing.Axis.Z)
                    newVector[2] = ((positive ^ this.rotations == 3 ^ this.flipped) ? 1 : -1) * (axis == EnumFacing.Axis.X ? -oldY : oldX);
            }
        }
        return newVector;
    }

    public void applyVertexPermutation(MutableQuad quad){
        float[] x = new float[4];
        float[] y = new float[4];
        float[] z = new float[4];
        float[] u = new float[4];
        float[] v = new float[4];
        for(int i = 0; i < 4; i++){
            x[i] = quad.x(i);
            y[i] = quad.y(i);
            z[i] = quad.z(i);
            u[i] = quad.u(i);
            v[i] = quad.v(i);
        }
        for(int from = 0; from < 4; from++){
            int to = this.vertexIndexPermutation[from];
            quad.position(
                to,
                x[from],
                y[from],
                z[from]
            );
            quad.uv(
                to,
                u[from],
                v[from]
            );
        }
    }

    public void applyInverseVertexPermutation(MutableQuad quad){
        float[] x = new float[4];
        float[] y = new float[4];
        float[] z = new float[4];
        float[] u = new float[4];
        float[] v = new float[4];
        for(int i = 0; i < 4; i++){
            x[i] = quad.x(i);
            y[i] = quad.y(i);
            z[i] = quad.z(i);
            u[i] = quad.u(i);
            v[i] = quad.v(i);
        }
        for(int to = 0; to < 4; to++){
            int from = this.vertexIndexPermutation[to];
            quad.position(
                to,
                x[from],
                y[from],
                z[from]
            );
            quad.uv(
                to,
                u[from],
                v[from]
            );
        }
    }
}
