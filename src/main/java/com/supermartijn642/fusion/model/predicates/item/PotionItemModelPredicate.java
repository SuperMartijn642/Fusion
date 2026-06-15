package com.supermartijn642.fusion.model.predicates.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.item.DefaultItemModelPredicates;
import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Objects;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class PotionItemModelPredicate implements ItemModelPredicate {

    public static ItemModelPredicate create(PotionType potion){
        Objects.requireNonNull(potion);
        return new PotionItemModelPredicate(potion);
    }

    public static final Serializer<PotionItemModelPredicate> SERIALIZER = new Serializer<PotionItemModelPredicate>() {
        @Override
        public PotionItemModelPredicate deserialize(JsonObject json) throws JsonParseException{
            boolean ignoreMissing = false;
            if(json.has("ignore_missing")){
                if(!json.get("ignore_missing").isJsonPrimitive() || !json.getAsJsonPrimitive("ignore_missing").isBoolean())
                    throw new JsonParseException("Property 'ignore_missing' must be a boolean!");
                ignoreMissing = json.get("ignore_missing").getAsBoolean();
            }

            if(!json.has("potion") || !json.get("potion").isJsonPrimitive() || !json.getAsJsonPrimitive("potion").isString())
                throw new JsonParseException("Potion-predicate must have string property 'enchantment'!");
            if(!IdentifierUtil.isValidIdentifier(json.get("potion").getAsString()))
                throw new JsonParseException("Property 'potion' must be a valid identifier, not '" + json.get("potion").getAsString() + "'!");
            ResourceLocation potionIdentifier = new ResourceLocation(json.get("potion").getAsString());
            PotionType potion = ForgeRegistries.POTION_TYPES.getValue(potionIdentifier);
            if(potion == null || potion == PotionTypes.EMPTY){
                if(ignoreMissing)
                    return new PotionItemModelPredicate(null);
                throw new JsonParseException("Unknown potion '" + potionIdentifier + "'!");
            }
            return new PotionItemModelPredicate(potion);
        }

        @Override
        public JsonObject serialize(PotionItemModelPredicate value){
            JsonObject json = new JsonObject();
            json.addProperty("potion", ForgeRegistries.POTION_TYPES.getKey(value.potion).toString());
            return json;
        }
    };

    private final PotionType potion;

    private PotionItemModelPredicate(PotionType potion){
        this.potion = potion;
    }

    @Override
    public boolean test(ItemStack stack){
        if(this.potion == null)
            return false;
        return PotionUtils.getPotionFromItem(stack) == this.potion;
    }

    @Override
    public ItemModelPredicate simplify(){
        return this.potion == null ? DefaultItemModelPredicates.never() : this;
    }

    @Override
    public Serializer<? extends ItemModelPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(!(o instanceof PotionItemModelPredicate)) return false;

        PotionItemModelPredicate that = (PotionItemModelPredicate)o;
        return Objects.equals(this.potion, that.potion);
    }

    @Override
    public int hashCode(){
        return Objects.hashCode(this.potion);
    }
}
