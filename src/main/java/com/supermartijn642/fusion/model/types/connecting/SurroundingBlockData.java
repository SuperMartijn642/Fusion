package com.supermartijn642.fusion.model.types.connecting;

import com.google.common.base.Objects;
import com.supermartijn642.fusion.api.predicate.ConnectionDirection;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IEnviromentBlockReader;
import net.minecraftforge.common.model.TRSRTransformation;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class SurroundingBlockData {

    public static SurroundingBlockData create(IEnviromentBlockReader level, BlockPos pos, TRSRTransformation rotation, List<ConnectionPredicate> predicates){
        TRSRTransformation inverseRotation = rotation.inverse();
        Map<Direction,SideConnections> connections = new EnumMap<>(Direction.class);
        for(Direction side : Direction.values())
            connections.put(side, getConnections(side, rotation, inverseRotation, level, pos, predicates));
        return new SurroundingBlockData(connections);
    }

    private static SideConnections getConnections(Direction side, TRSRTransformation rotation, TRSRTransformation inverseRotation, IEnviromentBlockReader level, BlockPos pos, List<ConnectionPredicate> predicates){
        Direction originalSide = inverseRotation.rotateTransform(side);
        Direction left;
        Direction right;
        Direction up;
        Direction down;
        if(originalSide.getAxis() == Direction.Axis.Y){
            left = Direction.WEST;
            right = Direction.EAST;
            up = originalSide == Direction.UP ? Direction.NORTH : Direction.SOUTH;
            down = originalSide == Direction.UP ? Direction.SOUTH : Direction.NORTH;
        }else{
            left = originalSide.getClockWise();
            right = originalSide.getCounterClockWise();
            up = Direction.UP;
            down = Direction.DOWN;
        }
        left = rotation.rotateTransform(left);
        right = rotation.rotateTransform(right);
        up = rotation.rotateTransform(up);
        down = rotation.rotateTransform(down);

        BlockState self = level.getBlockState(pos);
        boolean connectTop = shouldConnect(level, side, originalSide, self, pos.relative(up), ConnectionDirection.TOP, predicates);
        boolean connectTopRight = shouldConnect(level, side, originalSide, self, pos.relative(up).relative(right), ConnectionDirection.TOP_RIGHT, predicates);
        boolean connectRight = shouldConnect(level, side, originalSide, self, pos.relative(right), ConnectionDirection.RIGHT, predicates);
        boolean connectBottomRight = shouldConnect(level, side, originalSide, self, pos.relative(down).relative(right), ConnectionDirection.BOTTOM_RIGHT, predicates);
        boolean connectBottom = shouldConnect(level, side, originalSide, self, pos.relative(down), ConnectionDirection.BOTTOM, predicates);
        boolean connectBottomLeft = shouldConnect(level, side, originalSide, self, pos.relative(down).relative(left), ConnectionDirection.BOTTOM_LEFT, predicates);
        boolean connectLeft = shouldConnect(level, side, originalSide, self, pos.relative(left), ConnectionDirection.LEFT, predicates);
        boolean connectTopLeft = shouldConnect(level, side, originalSide, self, pos.relative(up).relative(left), ConnectionDirection.TOP_LEFT, predicates);
        return new SideConnections(side, connectTop, connectTopRight, connectRight, connectBottomRight, connectBottom, connectBottomLeft, connectLeft, connectTopLeft);
    }

    private static boolean shouldConnect(IEnviromentBlockReader level, Direction side, Direction originalSide, BlockState self, BlockPos neighborPos, ConnectionDirection direction, List<ConnectionPredicate> predicates){
        BlockState otherState = level.getBlockState(neighborPos);
        BlockState stateInFront = level.getBlockState(neighborPos.relative(side));
        return predicates.stream().anyMatch(predicate -> predicate.shouldConnect(originalSide, self, otherState, stateInFront, direction));
    }

    private final Map<Direction,SideConnections> connections;
    private final int hashCode;

    public SurroundingBlockData(Map<Direction,SideConnections> connections){
        this.connections = connections;
        // Calculate the hashcode once and store it
        this.hashCode = this.connections.hashCode();
    }

    public SideConnections getConnections(Direction side){
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

        public final Direction side;
        public final boolean top;
        public final boolean topRight;
        public final boolean right;
        public final boolean bottomRight;
        public final boolean bottom;
        public final boolean bottomLeft;
        public final boolean left;
        public final boolean topLeft;

        public SideConnections(Direction side, boolean top, boolean topRight, boolean right, boolean bottomRight, boolean bottom, boolean bottomLeft, boolean left, boolean topLeft){
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
