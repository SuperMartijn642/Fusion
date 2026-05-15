package com.supermartijn642.fusion.model.predicates.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.item.ItemStack;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class CountItemModelPredicate implements ItemModelPredicate {

    public static ItemModelPredicate create(int count){
        return create(count, count);
    }

    public static ItemModelPredicate create(int min, int max){
        return new CountItemModelPredicate(Either.left(min), Either.left(max));
    }

    public static ItemModelPredicate create(int min, float maxPercentage){
        return new CountItemModelPredicate(Either.left(min), Either.right(maxPercentage));
    }

    public static ItemModelPredicate create(float minPercentage, int max){
        return new CountItemModelPredicate(Either.right(minPercentage), Either.left(max));
    }

    public static ItemModelPredicate create(float minPercentage, float maxPercentage){
        return new CountItemModelPredicate(Either.right(minPercentage), Either.right(maxPercentage));
    }

    private static ItemModelPredicate create(Either<Integer,Float> min, Either<Integer,Float> max){
        if(min.isLeft() && min.left() < 0)
            throw new IllegalArgumentException("Minimum count must be a positive number!");
        if(min.isRight() && (min.right() < 0 || min.right() > 1))
            throw new IllegalArgumentException("Minimum percentage must be between 0 and 1!");
        if(max.isLeft() && max.left() < 0)
            throw new IllegalArgumentException("Maximum count must be a positive number!");
        if(max.isRight() && (max.right() < 0 || max.right() > 1))
            throw new IllegalArgumentException("Maximum percentage must be between 0 and 1!");
        if((min.isLeft() && max.isLeft() && min.left() > max.left()) || (min.isRight() && max.isRight() && min.right() > max.right()))
            throw new IllegalArgumentException("Minimum count must be less than or equal to maximum count!");
        return new CountItemModelPredicate(min, max);
    }

    public static final Serializer<CountItemModelPredicate> SERIALIZER = new Serializer<CountItemModelPredicate>() {
        @Override
        public CountItemModelPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("min") && !json.has("min_percentage") && !json.has("max") && !json.has("max_percentage"))
                throw new JsonParseException("Count-predicate must have at least one of 'min', 'min_percentage', 'max', or 'max_percentage'!");

            // Minimum count
            if(json.has("min") && json.has("min_percentage"))
                throw new JsonParseException("Count-predicate can have only either 'min' or 'min_percentage', not both!");
            Either<Integer,Float> min = Either.left(0);
            if(json.has("min")){
                if(!json.get("min").isJsonPrimitive() || !json.getAsJsonPrimitive("min").isNumber())
                    throw new JsonParseException("Property 'min' must be a number!");
                int minValue = json.getAsJsonPrimitive("min").getAsInt();
                if(minValue < 0)
                    throw new JsonParseException("Property 'min' must be a positive number!");
                min = Either.left(minValue);
            }
            if(json.has("min_percentage")){
                if(!json.get("min_percentage").isJsonPrimitive() || !json.getAsJsonPrimitive("min_percentage").isNumber())
                    throw new JsonParseException("Property 'min_percentage' must be a number!");
                float minValue = json.getAsJsonPrimitive("min_percentage").getAsFloat();
                if(minValue < 0 || minValue > 1)
                    throw new JsonParseException("Property 'min_percentage' must be between 0 and 1!");
                min = Either.right(minValue);
            }

            // Maximum count
            if(json.has("max") && json.has("max_percentage"))
                throw new JsonParseException("Count-predicate can have only either 'max' or 'max_percentage', not both!");
            Either<Integer,Float> max = Either.right(1f);
            if(json.has("max")){
                if(!json.get("max").isJsonPrimitive() || !json.getAsJsonPrimitive("max").isNumber())
                    throw new JsonParseException("Property 'max' must be a number!");
                int maxValue = json.getAsJsonPrimitive("max").getAsInt();
                if(maxValue < 0)
                    throw new JsonParseException("Property 'max' must be a positive number!");
                max = Either.left(maxValue);
            }
            if(json.has("max_percentage")){
                if(!json.get("max_percentage").isJsonPrimitive() || !json.getAsJsonPrimitive("max_percentage").isNumber())
                    throw new JsonParseException("Property 'max_percentage' must be a number!");
                float maxValue = json.getAsJsonPrimitive("max_percentage").getAsFloat();
                if(maxValue < 0 || maxValue > 1)
                    throw new JsonParseException("Property 'max_percentage' must be between 0 and 1!");
                max = Either.right(maxValue);
            }

            // Validate min <= max
            if((min.isLeft() && max.isLeft() && min.left() > max.left()) || (min.isRight() && max.isRight() && min.right() > max.right()))
                throw new JsonParseException("Minimum count must be less than or equal to maximum count!");
            return new CountItemModelPredicate(min, max);
        }

        @Override
        public JsonObject serialize(CountItemModelPredicate value){
            JsonObject json = new JsonObject();
            if(value.isMinPercentage)
                json.addProperty("min_percentage", value.minPercentage);
            else
                json.addProperty("min", value.min);
            if(value.isMaxPercentage)
                json.addProperty("max_percentage", value.maxPercentage);
            else
                json.addProperty("max", value.max);
            return json;
        }
    };

    private final int min, max;
    private final float minPercentage, maxPercentage;
    private final boolean isMinPercentage, isMaxPercentage;

    private CountItemModelPredicate(Either<Integer,Float> min, Either<Integer,Float> max){
        this.min = min.isLeft() ? min.left() : -1;
        this.max = max.isLeft() ? max.left() : -1;
        this.minPercentage = min.isRight() ? min.right() : -1;
        this.maxPercentage = max.isRight() ? max.right() : -1;
        this.isMinPercentage = min.isRight();
        this.isMaxPercentage = max.isRight();
    }

    @Override
    public boolean test(ItemStack stack){
        int count = stack.getCount();
        return (this.isMinPercentage ? this.minPercentage * stack.getItem().getItemStackLimit(stack) <= count : this.min <= count)
            && (this.isMaxPercentage ? this.maxPercentage * stack.getItem().getItemStackLimit(stack) >= count : this.max >= count);
    }

    @Override
    public Serializer<? extends ItemModelPredicate> getSerializer(){
        return SERIALIZER;
    }
}
