package com.supermartijn642.fusion.predicate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.predicate.ConnectionDirection;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.predicate.SensitiveConnectionPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Created 28/04/2023 by SuperMartijn642
 */
public class IsFaceVisibleConnectionPredicate implements SensitiveConnectionPredicate {

    public static final Serializer<IsFaceVisibleConnectionPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public IsFaceVisibleConnectionPredicate deserialize(JsonObject json) throws JsonParseException{
            return new IsFaceVisibleConnectionPredicate();
        }

        @Override
        public JsonObject serialize(IsFaceVisibleConnectionPredicate value){
            return new JsonObject();
        }
    };

    public IsFaceVisibleConnectionPredicate(){
    }

    @Override
    public boolean shouldConnect(BlockGetter level, BlockPos pos, Direction side, @Nullable BlockState ownState, BlockState otherState, BlockState blockInFront, ConnectionDirection direction){
        if(!blockInFront.canOcclude())
            return true;
        if(otherState.skipRendering(blockInFront, side))
            return false;

        // Compare the occlusion shapes of otherState and blockInFront
        Block.BlockStatePairKey statePair = new Block.BlockStatePairKey(otherState, blockInFront, side);
        Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey> occlusionCache = Block.OCCLUSION_CACHE.get();
        byte b = occlusionCache.getAndMoveToFirst(statePair);
        if(b != 127)
            return b != 0;
        VoxelShape otherStateShape = otherState.getFaceOcclusionShape(level, pos, side);
        if(otherStateShape.isEmpty())
            return true;
        VoxelShape inFrontShape = blockInFront.getFaceOcclusionShape(level, pos.relative(side), side.getOpposite());
        boolean bl = Shapes.joinIsNotEmpty(otherStateShape, inFrontShape, BooleanOp.ONLY_FIRST);
        if(occlusionCache.size() == 2048)
            occlusionCache.removeLastByte();
        occlusionCache.putAndMoveToFirst(statePair, (byte)(bl ? 1 : 0));
        return bl;
    }

    @Override
    public Serializer<? extends ConnectionPredicate> getSerializer(){
        return SERIALIZER;
    }
}
