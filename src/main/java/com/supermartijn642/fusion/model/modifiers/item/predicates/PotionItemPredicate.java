package com.supermartijn642.fusion.model.modifiers.item.predicates;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.modifier.item.ItemPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtils;
import net.minecraft.potion.Potions;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class PotionItemPredicate implements ItemPredicate {

    public static final Serializer<PotionItemPredicate> SERIALIZER = new Serializer<PotionItemPredicate>() {
        @Override
        public PotionItemPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("potion") || !json.get("potion").isJsonPrimitive() || !json.getAsJsonPrimitive("potion").isString())
                throw new JsonParseException("Potion-predicate must have string property 'enchantment'!");
            if(!IdentifierUtil.isValidIdentifier(json.get("potion").getAsString()))
                throw new JsonParseException("Property 'enchantment' must be a valid identifier, not '" + json.get("potion").getAsString() + "'!");
            ResourceLocation potionIdentifier = new ResourceLocation(json.get("potion").getAsString());
            Potion potion = Registry.POTION.get(potionIdentifier);
            if(potion == null || potion == Potions.EMPTY)
                throw new JsonParseException("Unknown potion '" + potionIdentifier + "'!");
            return new PotionItemPredicate(potion);
        }

        @Override
        public JsonObject serialize(PotionItemPredicate value){
            JsonObject json = new JsonObject();
            json.addProperty("potion", Registry.POTION.getKey(value.potion).toString());
            return json;
        }
    };

    private final Potion potion;

    public PotionItemPredicate(Potion potion){
        if(potion == null)
            throw new NullPointerException("Potion must not be null!");
        this.potion = potion;
    }

    @Override
    public boolean test(ItemStack stack){
        return PotionUtils.getPotion(stack) == this.potion;
    }

    @Override
    public Serializer<? extends ItemPredicate> getSerializer(){
        return SERIALIZER;
    }
}
