package com.supermartijn642.fusion.model.predicates.blockstate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.api.model.predicates.blockstate.DefaultBlockStateModelPredicates;
import com.supermartijn642.fusion.api.model.predicates.blockstate.FusionBlockStateModelPredicateRegistry;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Created 14/05/2026 by SuperMartijn642
 */
public class NotBlockStateModelPredicate implements BlockStateModelPredicate {

    public static BlockStateModelPredicate create(BlockStateModelPredicate predicate){
        return new NotBlockStateModelPredicate(predicate);
    }

    public static final Serializer<NotBlockStateModelPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public NotBlockStateModelPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("predicate") || !json.get("predicate").isJsonObject())
                throw new JsonParseException("Not-predicate must have object property 'predicate'!");
            // Deserialize the predicate
            BlockStateModelPredicate predicate = FusionBlockStateModelPredicateRegistry.deserializeBlockStateModelPredicate(json.getAsJsonObject("predicate"));
            return new NotBlockStateModelPredicate(predicate);
        }

        @Override
        public JsonObject serialize(NotBlockStateModelPredicate value){
            JsonObject json = new JsonObject();
            json.add("predicate", FusionBlockStateModelPredicateRegistry.serializeBlockStateModelPredicate(value.predicate));
            return json;
        }
    };

    private final BlockStateModelPredicate predicate;

    private NotBlockStateModelPredicate(BlockStateModelPredicate predicate){
        this.predicate = predicate;
    }

    @Override
    public boolean test(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state){
        return !this.predicate.test(level, pos, state);
    }

    @Override
    public BlockStateModelPredicate applyTransform(ModelTransform transform){
        return new NotBlockStateModelPredicate(this.predicate.applyTransform(transform));
    }

    @Override
    public BlockStateModelPredicate simplify(){
        BlockStateModelPredicate simplified = this.predicate.simplify();
        if(simplified.alwaysTrue())
            return DefaultBlockStateModelPredicates.never();
        if(simplified.alwaysFalse())
            return DefaultBlockStateModelPredicates.always();
        return new NotBlockStateModelPredicate(simplified);
    }

    @Override
    public Serializer<? extends BlockStateModelPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof NotBlockStateModelPredicate that)) return false;

        return this.predicate.equals(that.predicate);
    }

    @Override
    public int hashCode(){
        return this.predicate.hashCode();
    }
}
