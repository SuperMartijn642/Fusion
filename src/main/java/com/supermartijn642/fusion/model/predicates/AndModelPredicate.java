package com.supermartijn642.fusion.model.predicates;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.DefaultModelPredicates;
import com.supermartijn642.fusion.api.model.predicates.FusionModelPredicateRegistry;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ILightReader;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created 14/05/2026 by SuperMartijn642
 */
public class AndModelPredicate implements ModelPredicate {

    public static AndModelPredicate create(ModelPredicate... predicates){
        return new AndModelPredicate(Arrays.copyOf(predicates, predicates.length));
    }

    public static final Serializer<AndModelPredicate> SERIALIZER = new Serializer<AndModelPredicate>() {
        @Override
        public AndModelPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("predicates") || !json.get("predicates").isJsonArray())
                throw new JsonParseException("And-predicate must have array property 'predicates'!");
            // Deserialize all the predicates from the 'predicates' array
            JsonArray array = json.getAsJsonArray("predicates");
            List<ModelPredicate> predicates = new ArrayList<>(array.size());
            for(JsonElement element : array){
                if(!element.isJsonObject())
                    throw new JsonParseException("Property 'predicates' must only contain objects!");
                ModelPredicate predicate = FusionModelPredicateRegistry.deserializeModelPredicate(element.getAsJsonObject());
                predicates.add(predicate);
            }
            return new AndModelPredicate(predicates.toArray(new ModelPredicate[0]));
        }

        @Override
        public JsonObject serialize(AndModelPredicate value){
            JsonObject json = new JsonObject();
            // Create an array with all the serialized predicates
            JsonArray predicatesJson = new JsonArray();
            for(ModelPredicate predicate : value.predicates)
                predicatesJson.add(FusionModelPredicateRegistry.serializeModelPredicate(predicate));
            json.add("predicates", predicatesJson);
            return json;
        }
    };

    private final ModelPredicate[] predicates;

    private AndModelPredicate(ModelPredicate[] predicates){
        this.predicates = predicates;
    }

    @Override
    public boolean testForBlockState(@Nullable ILightReader level, @Nullable BlockPos pos, @Nullable BlockState state){
        for(ModelPredicate predicate : this.predicates){
            if(!predicate.testForBlockState(level, pos, state))
                return false;
        }
        return true;
    }

    @Override
    public boolean testForItem(ItemStack stack){
        for(ModelPredicate predicate : this.predicates){
            if(!predicate.testForItem(stack))
                return false;
        }
        return true;
    }

    @Override
    public ModelPredicate simplify(){
        List<ModelPredicate> flattened = new ArrayList<>(this.predicates.length);
        for(ModelPredicate predicate : this.predicates){
            predicate = predicate.simplify();
            if(predicate.alwaysFalse())
                return DefaultModelPredicates.never();
            if(predicate instanceof AndModelPredicate)
                flattened.addAll(Arrays.asList(((AndModelPredicate)predicate).predicates));
            else if(!predicate.alwaysTrue())
                flattened.add(predicate);
        }
        if(flattened.isEmpty())
            return DefaultModelPredicates.always();
        return new AndModelPredicate(flattened.toArray(new ModelPredicate[0]));
    }

    @Override
    public Serializer<? extends ModelPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof AndModelPredicate)) return false;

        AndModelPredicate that = (AndModelPredicate)o;
        return Arrays.equals(this.predicates, that.predicates);
    }

    @Override
    public int hashCode(){
        return Arrays.hashCode(this.predicates);
    }
}
