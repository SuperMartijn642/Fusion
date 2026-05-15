package com.supermartijn642.fusion.entity.model.predicates;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.Objects;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class DimensionEntityModelPredicate implements EntityModelPredicate {

    public static EntityModelPredicate create(ResourceKey<Level> dimension){
        return new DimensionEntityModelPredicate(dimension.location());
    }

    public static EntityModelPredicate create(ResourceLocation dimension){
        Objects.requireNonNull(dimension);
        return new DimensionEntityModelPredicate(dimension);
    }

    public static final Serializer<DimensionEntityModelPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public DimensionEntityModelPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("dimension") || !json.get("dimension").isJsonPrimitive() || !json.getAsJsonPrimitive("dimension").isString())
                throw new JsonParseException("Dimension-predicate must have string property 'dimension'!");
            if(!IdentifierUtil.isValidIdentifier(json.get("dimension").getAsString()))
                throw new JsonParseException("Dimension must be a valid identifier, not '" + json.get("dimension").getAsString() + "'!");
            return new DimensionEntityModelPredicate(ResourceLocation.parse(json.get("dimension").getAsString()));
        }

        @Override
        public JsonObject serialize(DimensionEntityModelPredicate value){
            JsonObject json = new JsonObject();
            json.addProperty("dimension", value.dimension.toString());
            return json;
        }
    };

    private final ResourceLocation dimension;

    private DimensionEntityModelPredicate(ResourceLocation dimension){
        this.dimension = dimension;
    }

    @Override
    public boolean test(Entity entity){
        Level level = entity.level();
        return level != null && level.dimension().location().equals(this.dimension);
    }

    @Override
    public Serializer<? extends EntityModelPredicate> getSerializer(){
        return SERIALIZER;
    }
}
