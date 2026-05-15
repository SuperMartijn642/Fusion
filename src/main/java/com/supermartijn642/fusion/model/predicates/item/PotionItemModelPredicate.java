package com.supermartijn642.fusion.model.predicates.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;

import java.util.Objects;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class PotionItemModelPredicate implements ItemModelPredicate {

    public static ItemModelPredicate create(Potion potion){
        Objects.requireNonNull(potion);
        return new PotionItemModelPredicate(potion);
    }

    public static final Serializer<PotionItemModelPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public PotionItemModelPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("potion") || !json.get("potion").isJsonPrimitive() || !json.getAsJsonPrimitive("potion").isString())
                throw new JsonParseException("Potion-predicate must have string property 'enchantment'!");
            if(!IdentifierUtil.isValidIdentifier(json.get("potion").getAsString()))
                throw new JsonParseException("Property 'enchantment' must be a valid identifier, not '" + json.get("potion").getAsString() + "'!");
            ResourceLocation potionIdentifier = new ResourceLocation(json.get("potion").getAsString());
            Potion potion = BuiltInRegistries.POTION.get(potionIdentifier);
            if(potion == null || potion == Potions.EMPTY)
                throw new JsonParseException("Unknown potion '" + potionIdentifier + "'!");
            return new PotionItemModelPredicate(potion);
        }

        @Override
        public JsonObject serialize(PotionItemModelPredicate value){
            JsonObject json = new JsonObject();
            json.addProperty("potion", BuiltInRegistries.POTION.getKey(value.potion).toString());
            return json;
        }
    };

    private final Potion potion;

    private PotionItemModelPredicate(Potion potion){
        this.potion = potion;
    }

    @Override
    public boolean test(ItemStack stack){
        return PotionUtils.getPotion(stack) == this.potion;
    }

    @Override
    public Serializer<? extends ItemModelPredicate> getSerializer(){
        return SERIALIZER;
    }
}
