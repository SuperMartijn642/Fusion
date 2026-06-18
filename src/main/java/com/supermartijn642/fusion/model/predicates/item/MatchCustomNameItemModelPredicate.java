package com.supermartijn642.fusion.model.predicates.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Created 18/06/2026 by SuperMartijn642
 */
public class MatchCustomNameItemModelPredicate implements ItemModelPredicate {

    public static ItemModelPredicate create(String matchType, String text){
        MatchType type;
        try{
            type = MatchType.valueOf(matchType.toUpperCase(Locale.ROOT));
        }catch(IllegalArgumentException e){
            throw new IllegalArgumentException("Match type must be one of " + Arrays.toString(MatchType.values()).toLowerCase(Locale.ROOT) + ", not '" + matchType + "'!");
        }
        Objects.requireNonNull(text);
        type.validate.accept(text);
        return new MatchCustomNameItemModelPredicate(type, text);
    }

    public static final Serializer<MatchCustomNameItemModelPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public MatchCustomNameItemModelPredicate deserialize(JsonObject json) throws JsonParseException{
            MatchType matchType = MatchType.EQUALS;
            if(json.has("match_type")){
                if(!json.get("match_type").isJsonPrimitive() || !json.getAsJsonPrimitive("match_type").isString())
                    throw new JsonParseException("Property 'match_type' must be a string!");
                try{
                    matchType = MatchType.valueOf(json.get("match_type").getAsString().toUpperCase(Locale.ROOT));
                }catch(IllegalArgumentException e){
                    throw new JsonParseException("Property 'match_type' must be one of " + Arrays.toString(MatchType.values()).toLowerCase(Locale.ROOT) + ", not '" + json.get("match_type").getAsString() + "'!");
                }
            }
            if(!json.has("text"))
                throw new JsonParseException("Match name predicate must have property 'text'!");
            if(!json.get("text").isJsonPrimitive() || !json.getAsJsonPrimitive("text").isString())
                throw new JsonParseException("Property 'text' must be a string!");
            String text = json.get("text").getAsString();
            matchType.validate.accept(text);
            return new MatchCustomNameItemModelPredicate(matchType, text);
        }

        @Override
        public JsonObject serialize(MatchCustomNameItemModelPredicate data){
            JsonObject json = new JsonObject();
            if(data.matchType != MatchType.EQUALS)
                json.addProperty("match_type", data.matchType.name().toLowerCase(Locale.ROOT));
            json.addProperty("text", data.text);
            return json;
        }
    };

    private final MatchType matchType;
    private final String text;
    private final Predicate<String> matcher;

    private MatchCustomNameItemModelPredicate(MatchType matchType, String text){
        this.matchType = matchType;
        this.text = text;
        this.matcher = matchType.matcher.apply(text);
    }

    @Override
    public boolean test(ItemStack stack){
        Component name = stack.getCustomName();
        return name != null && this.matcher.test(name.getString());
    }

    @Override
    public Serializer<? extends ItemModelPredicate> getSerializer(){
        return SERIALIZER;
    }

    private enum MatchType {
        EQUALS(s -> s::equals),
        STARTS_WITH(s -> s::startsWith),
        ENDS_WITH(s -> s::endsWith),
        CONTAINS(s -> s::contains),
        REGEX(
            s -> {
                try{
                    Pattern.compile(s);
                }catch(PatternSyntaxException e){
                    throw new JsonParseException("Invalid regex:\n" + e.getMessage());
                }
            },
            s -> {
                // Perhaps some mod collects model state on a separate thread, so create a new matcher every time rather than reusing one
                Pattern pattern = Pattern.compile(s);
                return candidate -> pattern.matcher(candidate).find();
            }
        );

        private final Consumer<String> validate;
        private final Function<String,Predicate<String>> matcher;

        MatchType(Consumer<String> validate, Function<String,Predicate<String>> matcher){
            this.validate = validate;
            this.matcher = matcher;
        }

        MatchType(Function<String,Predicate<String>> matcher){
            this(s -> {}, matcher);
        }
    }
}
