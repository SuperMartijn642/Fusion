package com.supermartijn642.fusion.model.types.connecting;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.supermartijn642.fusion.api.predicate.ConnectionDirection;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.TransformationMatrix;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ILightReader;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class SurroundingBlockData {

    public static SurroundingBlockData create(ILightReader level, BlockPos pos, TransformationMatrix rotation, Map<ResourceLocation,ConnectionPredicate> predicates){
        TransformationMatrix inverseRotation = rotation.inverse();
        Matrix4f rotationMatrix = inverseRotation == null ? TransformationMatrix.identity().getMatrix() : rotation.getMatrix();
        Matrix4f inverseRotationMatrix = inverseRotation == null ? TransformationMatrix.identity().getMatrix() : inverseRotation.getMatrix();
        // Collect all surrounding blocks
        BlockState[][][] states = new BlockState[3][3][3];
        BlockPos.Mutable statePos = new BlockPos.Mutable();
        for(int i = 0; i < 27; i++){
            statePos.set(pos.getX() + i % 3 - 1, pos.getY() + i / 3 % 3 - 1, pos.getZ() + i / 9 % 3 - 1);
            states[i % 3][i / 3 % 3][i / 9 % 3] = level.getBlockState(statePos);
        }
        // Test all the predicates
        ImmutableMap.Builder<ResourceLocation,Map<Direction,SideConnections>> connectionsBuilder = ImmutableMap.builder();
        for(ResourceLocation sprite : predicates.keySet()){
            Map<Direction,SideConnections> spriteConnections = new EnumMap<>(Direction.class);
            for(Direction side : Direction.values())
                spriteConnections.put(side, getConnections(side, rotationMatrix, inverseRotationMatrix, states, predicates.get(sprite), level, pos));
            connectionsBuilder.put(sprite, spriteConnections);
        }
        return new SurroundingBlockData(connectionsBuilder.build());
    }

    private static SideConnections getConnections(Direction side, Matrix4f rotation, Matrix4f inverseRotation, BlockState[][][] states, ConnectionPredicate predicate, ILightReader level, BlockPos pos){
        Direction originalSide = Direction.rotate(inverseRotation, side);
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
        left = Direction.rotate(rotation, left);
        right = Direction.rotate(rotation, right);
        up = Direction.rotate(rotation, up);
        down = Direction.rotate(rotation, down);

        BlockState self = states[1][1][1];
        boolean connectTop = shouldConnect(states, side, originalSide, self, up.getStepX(), up.getStepY(), up.getStepZ(), ConnectionDirection.TOP, predicate, level, pos);
        boolean connectTopRight = shouldConnect(states, side, originalSide, self, up.getStepX() + right.getStepX(), up.getStepY() + right.getStepY(), up.getStepZ() + right.getStepZ(), ConnectionDirection.TOP_RIGHT, predicate, level, pos);
        boolean connectRight = shouldConnect(states, side, originalSide, self, right.getStepX(), right.getStepY(), right.getStepZ(), ConnectionDirection.RIGHT, predicate, level, pos);
        boolean connectBottomRight = shouldConnect(states, side, originalSide, self, down.getStepX() + right.getStepX(), down.getStepY() + right.getStepY(), down.getStepZ() + right.getStepZ(), ConnectionDirection.BOTTOM_RIGHT, predicate, level, pos);
        boolean connectBottom = shouldConnect(states, side, originalSide, self, down.getStepX(), down.getStepY(), down.getStepZ(), ConnectionDirection.BOTTOM, predicate, level, pos);
        boolean connectBottomLeft = shouldConnect(states, side, originalSide, self, down.getStepX() + left.getStepX(), down.getStepY() + left.getStepY(), down.getStepZ() + left.getStepZ(), ConnectionDirection.BOTTOM_LEFT, predicate, level, pos);
        boolean connectLeft = shouldConnect(states, side, originalSide, self, left.getStepX(), left.getStepY(), left.getStepZ(), ConnectionDirection.LEFT, predicate, level, pos);
        boolean connectTopLeft = shouldConnect(states, side, originalSide, self, up.getStepX() + left.getStepX(), up.getStepY() + left.getStepY(), up.getStepZ() + left.getStepZ(), ConnectionDirection.TOP_LEFT, predicate, level, pos);
        return new SideConnections(side, connectTop, connectTopRight, connectRight, connectBottomRight, connectBottom, connectBottomLeft, connectLeft, connectTopLeft);
    }

    private static boolean shouldConnect(BlockState[][][] states, Direction side, Direction originalSide, BlockState self, int neighborX, int neighborY, int neighborZ, ConnectionDirection direction, ConnectionPredicate predicate, ILightReader level, BlockPos pos){
        BlockState otherState = states[neighborX + 1][neighborY + 1][neighborZ + 1];
        BlockState stateInFront = states[neighborX + 1 + side.getStepX()][neighborY + 1 + side.getStepY()][neighborZ + 1 + side.getStepZ()];
        return predicate.shouldConnect(level, pos, originalSide, self, otherState, stateInFront, direction);
    }

    private final Map<ResourceLocation,Map<Direction,SideConnections>> connections;
    private final int hashCode;

    private SurroundingBlockData(Map<ResourceLocation,Map<Direction,SideConnections>> connections){
        this.connections = connections;
        // Calculate the hashcode once and store it
        this.hashCode = this.connections.hashCode();
    }

    public SideConnections getConnections(ResourceLocation sprite, Direction side){
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

        public final Direction side;
        public final boolean top;
        public final boolean topRight;
        public final boolean right;
        public final boolean bottomRight;
        public final boolean bottom;
        public final boolean bottomLeft;
        public final boolean left;
        public final boolean topLeft;
        public final int hash;

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
