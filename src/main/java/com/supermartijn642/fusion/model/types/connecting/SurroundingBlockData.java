package com.supermartijn642.fusion.model.types.connecting;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.supermartijn642.fusion.api.predicate.ConnectionDirection;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.model.TRSRTransformation;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class SurroundingBlockData {

    public static SurroundingBlockData create(IBlockAccess level, BlockPos pos, TRSRTransformation rotation, Map<ResourceLocation,ConnectionPredicate> predicates){
        TRSRTransformation inverseRotation = rotation.inverse();
        // Collect all surrounding blocks
        IBlockState[][][] states = new IBlockState[3][3][3];
        BlockPos.MutableBlockPos statePos = new BlockPos.MutableBlockPos();
        for(int i = 0; i < 27; i++){
            statePos.setPos(pos.getX() + i % 3 - 1, pos.getY() + i / 3 % 3 - 1, pos.getZ() + i / 9 % 3 - 1);
            states[i % 3][i / 3 % 3][i / 9 % 3] = level.getBlockState(statePos);
        }
        // Test all the predicates
        ImmutableMap.Builder<ResourceLocation,Map<EnumFacing,SideConnections>> connectionsBuilder = ImmutableMap.builder();
        for(ResourceLocation sprite : predicates.keySet()){
            Map<EnumFacing,SideConnections> spriteConnections = new EnumMap<>(EnumFacing.class);
            for(EnumFacing side : EnumFacing.values())
                spriteConnections.put(side, getConnections(side, rotation, inverseRotation, states, predicates.get(sprite), level, pos));
            connectionsBuilder.put(sprite, spriteConnections);
        }
        return new SurroundingBlockData(connectionsBuilder.build());
    }

    private static SideConnections getConnections(EnumFacing side, TRSRTransformation rotation, TRSRTransformation inverseRotation, IBlockState[][][] states, ConnectionPredicate predicate, IBlockAccess level, BlockPos pos){
        EnumFacing originalSide = inverseRotation.rotate(side);
        EnumFacing left;
        EnumFacing right;
        EnumFacing up;
        EnumFacing down;
        if(originalSide.getAxis() == EnumFacing.Axis.Y){
            left = EnumFacing.WEST;
            right = EnumFacing.EAST;
            up = originalSide == EnumFacing.UP ? EnumFacing.NORTH : EnumFacing.SOUTH;
            down = originalSide == EnumFacing.UP ? EnumFacing.SOUTH : EnumFacing.NORTH;
        }else{
            left = originalSide.rotateY();
            right = originalSide.rotateYCCW();
            up = EnumFacing.UP;
            down = EnumFacing.DOWN;
        }
        left = rotation.rotate(left);
        right = rotation.rotate(right);
        up = rotation.rotate(up);
        down = rotation.rotate(down);

        IBlockState self = states[1][1][1];
        boolean connectTop = shouldConnect(states, side, originalSide, self, up.getFrontOffsetX(), up.getFrontOffsetY(), up.getFrontOffsetZ(), ConnectionDirection.TOP, predicate, level, pos);
        boolean connectTopRight = shouldConnect(states, side, originalSide, self, up.getFrontOffsetX() + right.getFrontOffsetX(), up.getFrontOffsetY() + right.getFrontOffsetY(), up.getFrontOffsetZ() + right.getFrontOffsetZ(), ConnectionDirection.TOP_RIGHT, predicate, level, pos);
        boolean connectRight = shouldConnect(states, side, originalSide, self, right.getFrontOffsetX(), right.getFrontOffsetY(), right.getFrontOffsetZ(), ConnectionDirection.RIGHT, predicate, level, pos);
        boolean connectBottomRight = shouldConnect(states, side, originalSide, self, down.getFrontOffsetX() + right.getFrontOffsetX(), down.getFrontOffsetY() + right.getFrontOffsetY(), down.getFrontOffsetZ() + right.getFrontOffsetZ(), ConnectionDirection.BOTTOM_RIGHT, predicate, level, pos);
        boolean connectBottom = shouldConnect(states, side, originalSide, self, down.getFrontOffsetX(), down.getFrontOffsetY(), down.getFrontOffsetZ(), ConnectionDirection.BOTTOM, predicate, level, pos);
        boolean connectBottomLeft = shouldConnect(states, side, originalSide, self, down.getFrontOffsetX() + left.getFrontOffsetX(), down.getFrontOffsetY() + left.getFrontOffsetY(), down.getFrontOffsetZ() + left.getFrontOffsetZ(), ConnectionDirection.BOTTOM_LEFT, predicate, level, pos);
        boolean connectLeft = shouldConnect(states, side, originalSide, self, left.getFrontOffsetX(), left.getFrontOffsetY(), left.getFrontOffsetZ(), ConnectionDirection.LEFT, predicate, level, pos);
        boolean connectTopLeft = shouldConnect(states, side, originalSide, self, up.getFrontOffsetX() + left.getFrontOffsetX(), up.getFrontOffsetY() + left.getFrontOffsetY(), up.getFrontOffsetZ() + left.getFrontOffsetZ(), ConnectionDirection.TOP_LEFT, predicate, level, pos);
        return new SideConnections(side, connectTop, connectTopRight, connectRight, connectBottomRight, connectBottom, connectBottomLeft, connectLeft, connectTopLeft);
    }

    private static boolean shouldConnect(IBlockState[][][] states, EnumFacing side, EnumFacing originalSide, IBlockState self, int neighborX, int neighborY, int neighborZ, ConnectionDirection direction, ConnectionPredicate predicate, IBlockAccess level, BlockPos pos){
        IBlockState otherState = states[neighborX + 1][neighborY + 1][neighborZ + 1];
        IBlockState stateInFront = states[neighborX + 1 + side.getFrontOffsetX()][neighborY + 1 + side.getFrontOffsetY()][neighborZ + 1 + side.getFrontOffsetZ()];
        return predicate.shouldConnect(level, pos, originalSide, self, otherState, stateInFront, direction);
    }

    private final Map<ResourceLocation,Map<EnumFacing,SideConnections>> connections;
    private final int hashCode;

    private SurroundingBlockData(Map<ResourceLocation,Map<EnumFacing,SideConnections>> connections){
        this.connections = connections;
        // Calculate the hashcode once and store it
        this.hashCode = this.connections.hashCode();
    }

    public SideConnections getConnections(ResourceLocation sprite, EnumFacing side){
        return this.connections.getOrDefault(sprite, Collections.emptyMap()).get(side);
    }

    @Override
    public int hashCode(){
        return this.hashCode;
    }

    @Override
    public boolean equals(Object obj){
        return obj instanceof SurroundingBlockData && this.hashCode == ((SurroundingBlockData)obj).hashCode;
    }

    public static final class SideConnections {

        public final EnumFacing side;
        public final boolean top;
        public final boolean topRight;
        public final boolean right;
        public final boolean bottomRight;
        public final boolean bottom;
        public final boolean bottomLeft;
        public final boolean left;
        public final boolean topLeft;
        public final int hash;

        public SideConnections(EnumFacing side, boolean top, boolean topRight, boolean right, boolean bottomRight, boolean bottom, boolean bottomLeft, boolean left, boolean topLeft){
            this.side = side;
            this.top = top;
            this.topRight = topRight;
            this.right = right;
            this.bottomRight = bottomRight;
            this.bottom = bottom;
            this.bottomLeft = bottomLeft;
            this.left = left;
            this.topLeft = topLeft;
            this.hash = Objects.hashCode(this.top, this.topRight, this.right, this.bottomRight, this.bottom, this.bottomLeft, this.left, this.topLeft);
        }

        @Override
        public boolean equals(Object o){
            if(this == o) return true;
            if(o == null || this.getClass() != o.getClass()) return false;
            SideConnections that = (SideConnections)o;
            return this.left == that.left && this.right == that.right && this.top == that.top && this.topLeft == that.topLeft && this.topRight == that.topRight && this.bottom == that.bottom && this.bottomLeft == that.bottomLeft && this.bottomRight == that.bottomRight && this.side == that.side;
        }

        @Override
        public int hashCode(){
            return this.hash;
        }
    }
}
