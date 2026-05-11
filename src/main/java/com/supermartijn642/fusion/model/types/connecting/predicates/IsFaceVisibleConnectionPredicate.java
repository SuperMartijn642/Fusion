package com.supermartijn642.fusion.model.types.connecting.predicates;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.types.connecting.predicates.ConnectionDirection;
import com.supermartijn642.fusion.api.model.types.connecting.predicates.ConnectionPredicate;
import com.supermartijn642.fusion.api.model.types.connecting.predicates.SensitiveConnectionPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Created 28/04/2023 by SuperMartijn642
 */
public class IsFaceVisibleConnectionPredicate implements SensitiveConnectionPredicate {

    public static final IsFaceVisibleConnectionPredicate INSTANCE = new IsFaceVisibleConnectionPredicate();
    public static final Serializer<IsFaceVisibleConnectionPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public IsFaceVisibleConnectionPredicate deserialize(JsonObject json) throws JsonParseException{
            return INSTANCE;
        }

        @Override
        public JsonObject serialize(IsFaceVisibleConnectionPredicate value){
            return new JsonObject();
        }
    };

    private IsFaceVisibleConnectionPredicate(){
    }

    @Override
    public boolean shouldConnect(BlockGetter level, BlockPos pos, Direction side, @Nullable BlockState ownState, BlockState otherState, BlockState blockInFront, ConnectionDirection direction){
        return Block.shouldRenderFace(level, pos, otherState, blockInFront, side);
    }

    @Override
    public Serializer<? extends ConnectionPredicate> getSerializer(){
        return SERIALIZER;
    }
}
