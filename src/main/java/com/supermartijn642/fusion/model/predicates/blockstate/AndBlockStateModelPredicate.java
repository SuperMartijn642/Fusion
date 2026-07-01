package com.supermartijn642.fusion.model.predicates.blockstate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.api.model.predicates.blockstate.DefaultBlockStateModelPredicates;
import com.supermartijn642.fusion.api.model.predicates.blockstate.FusionBlockStateModelPredicateRegistry;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created 14/05/2026 by SuperMartijn642
 */
public class AndBlockStateModelPredicate implements BlockStateModelPredicate {

    public static AndBlockStateModelPredicate create(BlockStateModelPredicate... predicates){
        return new AndBlockStateModelPredicate(Arrays.copyOf(predicates, predicates.length));
    }

    public static final Serializer<AndBlockStateModelPredicate> SERIALIZER = new Serializer<AndBlockStateModelPredicate>() {
        @Override
        public AndBlockStateModelPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("predicates") || !json.get("predicates").isJsonArray())
                throw new JsonParseException("And-predicate must have array property 'predicates'!");
            // Deserialize all the predicates from the 'predicates' array
            JsonArray array = json.getAsJsonArray("predicates");
            List<BlockStateModelPredicate> predicates = new ArrayList<>(array.size());
            for(JsonElement element : array){
                if(!element.isJsonObject())
                    throw new JsonParseException("Property 'predicates' must only contain objects!");
                BlockStateModelPredicate predicate = FusionBlockStateModelPredicateRegistry.deserializeBlockStateModelPredicate(element.getAsJsonObject());
                predicates.add(predicate);
            }
            return new AndBlockStateModelPredicate(predicates.toArray(new BlockStateModelPredicate[0]));
        }

        @Override
        public JsonObject serialize(AndBlockStateModelPredicate value){
            JsonObject json = new JsonObject();
            // Create an array with all the serialized predicates
            JsonArray predicatesJson = new JsonArray();
            for(BlockStateModelPredicate predicate : value.predicates)
                predicatesJson.add(FusionBlockStateModelPredicateRegistry.serializeBlockStateModelPredicate(predicate));
            json.add("predicates", predicatesJson);
            return json;
        }
    };

    private final BlockStateModelPredicate[] predicates;

    private AndBlockStateModelPredicate(BlockStateModelPredicate[] predicates){
        this.predicates = predicates;
    }

    @Override
    public boolean test(@Nullable IBlockAccess level, @Nullable BlockPos pos, @Nullable IBlockState state){
        for(BlockStateModelPredicate predicate : this.predicates){
            if(!predicate.test(level, pos, state))
                return false;
        }
        return true;
    }

    @Override
    public BlockStateModelPredicate applyTransform(ModelTransform transform){
        return new AndBlockStateModelPredicate(
            Arrays.stream(this.predicates)
                .map(p -> p.applyTransform(transform))
                .toArray(BlockStateModelPredicate[]::new)
        );
    }

    @Override
    public BlockStateModelPredicate simplify(){
        List<BlockStateModelPredicate> flattened = new ArrayList<>(this.predicates.length);
        for(BlockStateModelPredicate predicate : this.predicates){
            predicate = predicate.simplify();
            if(predicate.alwaysFalse())
                return DefaultBlockStateModelPredicates.never();
            if(predicate instanceof AndBlockStateModelPredicate)
                flattened.addAll(Arrays.asList(((AndBlockStateModelPredicate)predicate).predicates));
            else if(!predicate.alwaysTrue())
                flattened.add(predicate);
        }
        if(flattened.isEmpty())
            return DefaultBlockStateModelPredicates.always();
        return new AndBlockStateModelPredicate(flattened.toArray(new BlockStateModelPredicate[0]));
    }

    @Override
    public Serializer<? extends BlockStateModelPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof AndBlockStateModelPredicate)) return false;

        AndBlockStateModelPredicate that = (AndBlockStateModelPredicate)o;
        return Arrays.equals(this.predicates, that.predicates);
    }

    @Override
    public int hashCode(){
        return Arrays.hashCode(this.predicates);
    }
}
