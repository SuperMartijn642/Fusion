package com.supermartijn642.fusion.model.modifiers.block;

import com.supermartijn642.fusion.util.SeedHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Created 08/07/2026 by SuperMartijn642
 */
public interface RandomOffsetFunction {

    RandomOffsetFunction NONE = (original, pos, output) -> output.set(0, 0, 0);
    RandomOffsetFunction MATCH_BLOCK = (original, pos, output) -> output.set((float)original.x, (float)original.y, (float)original.z);

    static RandomOffsetFunction xyz(@Nullable Long seed, float minXOffset, float maxXOffset, float minYOffset, float maxYOffset, float minZOffset, float maxZOffset){
        return new XYZ(seed, minXOffset, maxXOffset, minYOffset, maxYOffset, minZOffset, maxZOffset);
    }

    void getOffset(Vector3d original, BlockPos pos, Vector3f output);

    class XYZ implements RandomOffsetFunction {

        private final Long seed;
        private final float minXOffset, maxXOffset, minYOffset, maxYOffset, minZOffset, maxZOffset;
        private final boolean hasXOffset, hasYOffset, hasZOffset;
        private final float xRange, yRange, zRange;

        public XYZ(Long seed, float minXOffset, float maxXOffset, float minYOffset, float maxYOffset, float minZOffset, float maxZOffset){
            this.seed = seed;
            this.minXOffset = minXOffset;
            this.maxXOffset = maxXOffset;
            this.minYOffset = minYOffset;
            this.maxYOffset = maxYOffset;
            this.minZOffset = minZOffset;
            this.maxZOffset = maxZOffset;
            this.hasXOffset = minXOffset != 0 || maxXOffset != 0;
            this.xRange = maxXOffset - minXOffset;
            this.hasYOffset = minYOffset != 0 || maxYOffset != 0;
            this.yRange = maxYOffset - minYOffset;
            this.hasZOffset = minZOffset != 0 || maxZOffset != 0;
            this.zRange = maxZOffset - minZOffset;
        }

        @Override
        public void getOffset(Vector3d original, BlockPos pos, Vector3f output){
            long s = SeedHelper.fromBlockPos(pos);
            if(this.seed != null)
                s = s * this.seed ^ s;
            float x = this.hasXOffset ? (float)(s & 15L) / 15 * this.xRange + this.minXOffset : 0;
            float y = this.hasYOffset ? (float)((s >> 4) & 15L) / 15 * this.yRange + this.minYOffset : 0;
            float z = this.hasZOffset ? (float)((s >> 8) & 15L) / 15 * this.zRange + this.minZOffset : 0;
            output.set(x, y, z);
        }

        @Override
        public final boolean equals(Object o){
            if(!(o instanceof XYZ)) return false;

            XYZ other = (XYZ)o;
            return Float.compare(this.minXOffset, other.minXOffset) == 0 && Float.compare(this.maxXOffset, other.maxXOffset) == 0 && Float.compare(this.minYOffset, other.minYOffset) == 0 && Float.compare(this.maxYOffset, other.maxYOffset) == 0 && Float.compare(this.minZOffset, other.minZOffset) == 0 && Float.compare(this.maxZOffset, other.maxZOffset) == 0 && Objects.equals(this.seed, other.seed);
        }

        @Override
        public int hashCode(){
            int result = Objects.hashCode(this.seed);
            result = 31 * result + Float.hashCode(this.minXOffset);
            result = 31 * result + Float.hashCode(this.maxXOffset);
            result = 31 * result + Float.hashCode(this.minYOffset);
            result = 31 * result + Float.hashCode(this.maxYOffset);
            result = 31 * result + Float.hashCode(this.minZOffset);
            result = 31 * result + Float.hashCode(this.maxZOffset);
            return result;
        }
    }
}
