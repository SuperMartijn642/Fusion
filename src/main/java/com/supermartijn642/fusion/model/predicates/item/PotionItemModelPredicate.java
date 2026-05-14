package com.supermartijn642.fusion.model.predicates.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class PotionItemModelPredicate implements ItemModelPredicate {

    public static final Serializer<PotionItemModelPredicate> SERIALIZER = new Serializer<PotionItemModelPredicate>() {
        @Override
        public PotionItemModelPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("potion") || !json.get("potion").isJsonPrimitive() || !json.getAsJsonPrimitive("potion").isString())
                throw new JsonParseException("Potion-predicate must have string property 'enchantment'!");
            if(!IdentifierUtil.isValidIdentifier(json.get("potion").getAsString()))
                throw new JsonParseException("Property 'enchantment' must be a valid identifier, not '" + json.get("potion").getAsString() + "'!");
            ResourceLocation potionIdentifier = new ResourceLocation(json.get("potion").getAsString());
            PotionType potion = ForgeRegistries.POTION_TYPES.getValue(potionIdentifier);
            if(potion == null || potion == PotionTypes.EMPTY)
                throw new JsonParseException("Unknown potion '" + potionIdentifier + "'!");
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

    public PotionItemModelPredicate(PotionType potion){
        if(potion == null)
            throw new NullPointerException("Potion must not be null!");
        this.potion = potion;
    }

    @Override
    public boolean test(ItemStack stack){
        return PotionUtils.getPotionFromItem(stack) == this.potion;
    }

    @Override
    public Serializer<? extends ItemModelPredicate> getSerializer(){
        return SERIALIZER;
    }
}
