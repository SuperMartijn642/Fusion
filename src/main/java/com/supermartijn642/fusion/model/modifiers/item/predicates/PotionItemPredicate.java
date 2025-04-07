package com.supermartijn642.fusion.model.modifiers.item.predicates;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.modifier.item.ItemPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.Optional;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class PotionItemPredicate implements ItemPredicate {

    public static final Serializer<PotionItemPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public PotionItemPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("potion") || !json.get("potion").isJsonPrimitive() || !json.getAsJsonPrimitive("potion").isString())
                throw new JsonParseException("Potion-predicate must have string property 'enchantment'!");
            if(!IdentifierUtil.isValidIdentifier(json.get("potion").getAsString()))
                throw new JsonParseException("Property 'enchantment' must be a valid identifier, not '" + json.get("potion").getAsString() + "'!");
            ResourceLocation potionIdentifier = new ResourceLocation(json.get("potion").getAsString());
            Optional<Holder.Reference<Potion>> potion = BuiltInRegistries.POTION.getHolder(potionIdentifier);
            if(potion.isEmpty())
                throw new JsonParseException("Unknown potion '" + potionIdentifier + "'!");
            return new PotionItemPredicate(potion.get());
        }

        @Override
        public JsonObject serialize(PotionItemPredicate value){
            JsonObject json = new JsonObject();
            json.addProperty("potion", value.potion.unwrapKey().get().location().toString());
            return json;
        }
    };

    private final Holder<Potion> potion;

    public PotionItemPredicate(Holder<Potion> potion){
        if(potion == null)
            throw new NullPointerException("Potion must not be null!");
        this.potion = potion;
    }

    @Override
    public boolean test(ItemStack stack){
        PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
        return potion != null && potion.is(this.potion);
    }

    @Override
    public Serializer<? extends ItemPredicate> getSerializer(){
        return SERIALIZER;
    }
}
