package com.supermartijn642.fusion.predicate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.predicate.ConnectionDirection;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.predicate.SensitiveConnectionPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nullable;

/**
 * Created 28/04/2023 by SuperMartijn642
 */
public class IsFaceVisibleConnectionPredicate implements SensitiveConnectionPredicate {

    public static final Serializer<IsFaceVisibleConnectionPredicate> SERIALIZER = new Serializer<IsFaceVisibleConnectionPredicate>() {
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
    public boolean shouldConnect(IBlockAccess level, BlockPos pos, EnumFacing side, @Nullable IBlockState ownState, IBlockState otherState, IBlockState blockInFront, ConnectionDirection direction){
        return otherState.shouldSideBeRendered(level, pos, side) && !blockInFront.doesSideBlockRendering(level, pos.offset(side), side.getOpposite());
    }

    @Override
    public Serializer<? extends ConnectionPredicate> getSerializer(){
        return SERIALIZER;
    }
}
