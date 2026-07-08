package com.supermartijn642.fusion.model.modifiers.block;

import com.supermartijn642.fusion.util.SeedHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Objects;

/**
 * Created 08/07/2026 by SuperMartijn642
 */
public interface RandomOffsetFunction {

    RandomOffsetFunction NONE = (_, _, output) -> output.zero();
    RandomOffsetFunction MATCH_BLOCK = (original, _, output) -> output.set(original.x, original.y, original.z);

    static RandomOffsetFunction xyz(@Nullable Long seed, float minXOffset, float maxXOffset, float minYOffset, float maxYOffset, float minZOffset, float maxZOffset){
        return new XYZ(seed, minXOffset, maxXOffset, minYOffset, maxYOffset, minZOffset, maxZOffset);
    }

    void getOffset(Vec3 original, BlockPos pos, Vector3f output);

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
        public void getOffset(Vec3 original, BlockPos pos, Vector3f output){
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
            if(!(o instanceof XYZ xyz)) return false;

            return Float.compare(this.minXOffset, xyz.minXOffset) == 0 && Float.compare(this.maxXOffset, xyz.maxXOffset) == 0 && Float.compare(this.minYOffset, xyz.minYOffset) == 0 && Float.compare(this.maxYOffset, xyz.maxYOffset) == 0 && Float.compare(this.minZOffset, xyz.minZOffset) == 0 && Float.compare(this.maxZOffset, xyz.maxZOffset) == 0 && Objects.equals(this.seed, xyz.seed);
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
