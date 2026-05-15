package com.supermartijn642.fusion.model.predicates;

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

/**
 * Created 14/05/2026 by SuperMartijn642
 */
public class NotModelPredicate implements ModelPredicate {

    public static ModelPredicate create(ModelPredicate predicate){
        return new NotModelPredicate(predicate);
    }

    public static final Serializer<NotModelPredicate> SERIALIZER = new Serializer<NotModelPredicate>() {
        @Override
        public NotModelPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("predicate") || !json.get("predicate").isJsonObject())
                throw new JsonParseException("Not-predicate must have object property 'predicate'!");
            // Deserialize the predicate
            ModelPredicate predicate = FusionModelPredicateRegistry.deserializeModelPredicate(json.getAsJsonObject("predicate"));
            return new NotModelPredicate(predicate);
        }

        @Override
        public JsonObject serialize(NotModelPredicate value){
            JsonObject json = new JsonObject();
            json.add("predicate", FusionModelPredicateRegistry.serializeModelPredicate(value.predicate));
            return json;
        }
    };

    private final ModelPredicate predicate;

    private NotModelPredicate(ModelPredicate predicate){
        this.predicate = predicate;
    }

    @Override
    public boolean testForBlockState(@Nullable ILightReader level, @Nullable BlockPos pos, @Nullable BlockState state){
        return !this.predicate.testForBlockState(level, pos, state);
    }

    @Override
    public boolean testForItem(ItemStack stack){
        return !this.predicate.testForItem(stack);
    }

    @Override
    public ModelPredicate simplify(){
        ModelPredicate simplified = this.predicate.simplify();
        if(simplified.alwaysTrue())
            return DefaultModelPredicates.never();
        if(simplified.alwaysFalse())
            return DefaultModelPredicates.always();
        return new NotModelPredicate(simplified);
    }

    @Override
    public Serializer<? extends ModelPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof NotModelPredicate)) return false;

        NotModelPredicate that = (NotModelPredicate)o;
        return this.predicate.equals(that.predicate);
    }

    @Override
    public int hashCode(){
        return this.predicate.hashCode();
    }
}
