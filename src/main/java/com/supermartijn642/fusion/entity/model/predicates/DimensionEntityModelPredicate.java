package com.supermartijn642.fusion.entity.model.predicates;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class DimensionEntityModelPredicate implements EntityModelPredicate {

    public static final Serializer<DimensionEntityModelPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public DimensionEntityModelPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("dimension") || !json.get("dimension").isJsonPrimitive() || !json.getAsJsonPrimitive("dimension").isString())
                throw new JsonParseException("Dimension-predicate must have string property 'dimension'!");
            if(!IdentifierUtil.isValidIdentifier(json.get("dimension").getAsString()))
                throw new JsonParseException("Dimension must be a valid identifier, not '" + json.get("dimension").getAsString() + "'!");
            return new DimensionEntityModelPredicate(new ResourceLocation(json.get("dimension").getAsString()));
        }

        @Override
        public JsonObject serialize(DimensionEntityModelPredicate value){
            JsonObject json = new JsonObject();
            json.addProperty("dimension", value.dimension.toString());
            return json;
        }
    };

    private final ResourceLocation dimension;
    private RegistryAccess registry;
    private Holder<Level> holder;

    public DimensionEntityModelPredicate(ResourceLocation dimension){
        this.dimension = dimension;
    }

    @Override
    public boolean test(Entity entity){
        Level level = entity.level();
        if(level == null)
            return false;
        if(level.registryAccess() != this.registry){
            this.registry = level.registryAccess();
            this.holder = this.registry.registryOrThrow(Registries.DIMENSION).getHolder(this.dimension).orElse(null);
        }
        return this.holder != null && this.holder.equals(level.dimension());
    }

    @Override
    public Serializer<? extends EntityModelPredicate> getSerializer(){
        return SERIALIZER;
    }
}
