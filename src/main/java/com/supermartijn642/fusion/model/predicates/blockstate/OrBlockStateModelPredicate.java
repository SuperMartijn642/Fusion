package com.supermartijn642.fusion.model.predicates.blockstate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.api.model.predicates.blockstate.DefaultBlockStateModelPredicates;
import com.supermartijn642.fusion.api.model.predicates.blockstate.FusionBlockStateModelPredicateRegistry;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IEnviromentBlockReader;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created 14/05/2026 by SuperMartijn642
 */
public class OrBlockStateModelPredicate implements BlockStateModelPredicate {

    public static BlockStateModelPredicate create(BlockStateModelPredicate... predicates){
        return new OrBlockStateModelPredicate(Arrays.copyOf(predicates, predicates.length));
    }

    public static final Serializer<OrBlockStateModelPredicate> SERIALIZER = new Serializer<OrBlockStateModelPredicate>() {
        @Override
        public OrBlockStateModelPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("predicates") || !json.get("predicates").isJsonArray())
                throw new JsonParseException("Or-predicate must have array property 'predicates'!");
            // Deserialize all the predicates from the 'predicates' array
            JsonArray array = json.getAsJsonArray("predicates");
            List<BlockStateModelPredicate> predicates = new ArrayList<>(array.size());
            for(JsonElement element : array){
                if(!element.isJsonObject())
                    throw new JsonParseException("Property 'predicates' must only contain objects!");
                BlockStateModelPredicate predicate = FusionBlockStateModelPredicateRegistry.deserializeBlockStateModelPredicate(element.getAsJsonObject());
                predicates.add(predicate);
            }
            return new OrBlockStateModelPredicate(predicates.toArray(new BlockStateModelPredicate[0]));
        }

        @Override
        public JsonObject serialize(OrBlockStateModelPredicate value){
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

    private OrBlockStateModelPredicate(BlockStateModelPredicate[] predicates){
        this.predicates = predicates;
    }

    @Override
    public boolean test(@Nullable IEnviromentBlockReader level, @Nullable BlockPos pos, @Nullable BlockState state){
        for(BlockStateModelPredicate predicate : this.predicates){
            if(predicate.test(level, pos, state))
                return true;
        }
        return false;
    }

    @Override
    public BlockStateModelPredicate simplify(){
        List<BlockStateModelPredicate> flattened = new ArrayList<>(this.predicates.length);
        for(BlockStateModelPredicate predicate : this.predicates){
            predicate = predicate.simplify();
            if(predicate.alwaysTrue())
                return DefaultBlockStateModelPredicates.always();
            if(predicate instanceof OrBlockStateModelPredicate)
                flattened.addAll(Arrays.asList(((OrBlockStateModelPredicate)predicate).predicates));
            else if(!predicate.alwaysFalse())
                flattened.add(predicate);
        }
        if(flattened.isEmpty())
            return DefaultBlockStateModelPredicates.never();
        return new OrBlockStateModelPredicate(flattened.toArray(new BlockStateModelPredicate[0]));
    }

    @Override
    public Serializer<? extends BlockStateModelPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof OrBlockStateModelPredicate)) return false;

        OrBlockStateModelPredicate that = (OrBlockStateModelPredicate)o;
        return Arrays.equals(this.predicates, that.predicates);
    }

    @Override
    public int hashCode(){
        return Arrays.hashCode(this.predicates);
    }
}
