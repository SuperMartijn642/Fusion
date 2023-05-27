package com.supermartijn642.fusion.model.types.connecting;

import com.google.common.base.Objects;
import com.supermartijn642.fusion.api.predicate.ConnectionDirection;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.model.TRSRTransformation;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class SurroundingBlockData {

    public static SurroundingBlockData create(IBlockAccess level, BlockPos pos, TRSRTransformation rotation, List<ConnectionPredicate> predicates){
        TRSRTransformation inverseRotation = rotation.inverse();
        Map<EnumFacing,SideConnections> connections = new EnumMap<>(EnumFacing.class);
        for(EnumFacing side : EnumFacing.values())
            connections.put(side, getConnections(side, rotation, inverseRotation, level, pos, predicates));
        return new SurroundingBlockData(connections);
    }

    private static SideConnections getConnections(EnumFacing side, TRSRTransformation rotation, TRSRTransformation inverseRotation, IBlockAccess level, BlockPos pos, List<ConnectionPredicate> predicates){
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

        IBlockState self = level.getBlockState(pos);
        boolean connectTop = shouldConnect(level, side, originalSide, self, pos.offset(up), ConnectionDirection.TOP, predicates);
        boolean connectTopRight = shouldConnect(level, side, originalSide, self, pos.offset(up).offset(right), ConnectionDirection.TOP_RIGHT, predicates);
        boolean connectRight = shouldConnect(level, side, originalSide, self, pos.offset(right), ConnectionDirection.RIGHT, predicates);
        boolean connectBottomRight = shouldConnect(level, side, originalSide, self, pos.offset(down).offset(right), ConnectionDirection.BOTTOM_RIGHT, predicates);
        boolean connectBottom = shouldConnect(level, side, originalSide, self, pos.offset(down), ConnectionDirection.BOTTOM, predicates);
        boolean connectBottomLeft = shouldConnect(level, side, originalSide, self, pos.offset(down).offset(left), ConnectionDirection.BOTTOM_LEFT, predicates);
        boolean connectLeft = shouldConnect(level, side, originalSide, self, pos.offset(left), ConnectionDirection.LEFT, predicates);
        boolean connectTopLeft = shouldConnect(level, side, originalSide, self, pos.offset(up).offset(left), ConnectionDirection.TOP_LEFT, predicates);
        return new SideConnections(side, connectTop, connectTopRight, connectRight, connectBottomRight, connectBottom, connectBottomLeft, connectLeft, connectTopLeft);
    }

    private static boolean shouldConnect(IBlockAccess level, EnumFacing side, EnumFacing originalSide, IBlockState self, BlockPos neighborPos, ConnectionDirection direction, List<ConnectionPredicate> predicates){
        IBlockState otherState = level.getBlockState(neighborPos);
        IBlockState stateInFront = level.getBlockState(neighborPos.offset(side));
        return predicates.stream().anyMatch(predicate -> predicate.shouldConnect(originalSide, self, otherState, stateInFront, direction));
    }

    private final Map<EnumFacing,SideConnections> connections;
    private final int hashCode;

    public SurroundingBlockData(Map<EnumFacing,SideConnections> connections){
        this.connections = connections;
        // Calculate the hashcode once and store it
        this.hashCode = this.connections.hashCode();
    }

    public SideConnections getConnections(EnumFacing side){
        return this.connections.get(side);
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
            return Objects.hashCode(this.side, this.left, this.right, this.top, this.topLeft, this.topRight, this.bottom, this.bottomLeft, this.bottomRight);
        }
    }
}
