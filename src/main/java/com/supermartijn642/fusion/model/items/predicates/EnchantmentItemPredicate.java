package com.supermartijn642.fusion.model.items.predicates;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class EnchantmentItemPredicate implements ItemPredicate {

    public static final Serializer<EnchantmentItemPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public EnchantmentItemPredicate deserialize(JsonObject json) throws JsonParseException{
            // Enchantment
            if(!json.has("enchantment") || !json.get("enchantment").isJsonPrimitive() || !json.getAsJsonPrimitive("enchantment").isString())
                throw new JsonParseException("Item-predicate must have string property 'enchantment'!");
            if(!IdentifierUtil.isValidIdentifier(json.get("enchantment").getAsString()))
                throw new JsonParseException("Property 'enchantment' must be a valid identifier, not '" + json.get("enchantment").getAsString() + "'!");
            ResourceLocation enchantmentIdentifier = new ResourceLocation(json.get("enchantment").getAsString());
            Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.get(enchantmentIdentifier);
            if(enchantment == null)
                throw new JsonParseException("Unknown enchantment '" + enchantmentIdentifier + "'!");

            // Maximum level
            int maxLevel = 255;
            if(json.has("max_level")){
                if(!json.get("max_level").isJsonPrimitive() || !json.getAsJsonPrimitive("max_level").isNumber())
                    throw new JsonParseException("Property 'max_level' must have be a number!");
                maxLevel = json.getAsJsonPrimitive("max_level").getAsInt();
                if(maxLevel < 0 || maxLevel > 255)
                    throw new JsonParseException("Property 'max_level' must be between 0 and 255!");
            }

            // Minimum level
            int minLevel = maxLevel == 0 ? 0 : 1;
            if(json.has("min_level")){
                if(!json.get("min_level").isJsonPrimitive() || !json.getAsJsonPrimitive("min_level").isNumber())
                    throw new JsonParseException("Property 'min_level' must have a number!");
                minLevel = json.getAsJsonPrimitive("min_level").getAsInt();
                if(minLevel < 0 || minLevel > 255)
                    throw new JsonParseException("Property 'min_level' must be between 0 and 255!");
            }

            // Validate min level <= max level
            if(minLevel > maxLevel)
                throw new JsonParseException("Property 'min_level' must be less than or equal to 'max_level'!");
            return new EnchantmentItemPredicate(enchantment, minLevel, maxLevel);
        }

        @Override
        public JsonObject serialize(EnchantmentItemPredicate value){
            JsonObject json = new JsonObject();
            json.addProperty("enchantment", BuiltInRegistries.ENCHANTMENT.getKey(value.enchantment).toString());
            if(value.maxLevel != 0 && value.minLevel != 1)
                json.addProperty("min_level", value.minLevel);
            if(value.maxLevel != 255)
                json.addProperty("max_level", value.maxLevel);
            return json;
        }
    };

    private final Enchantment enchantment;
    private final int minLevel, maxLevel;

    public EnchantmentItemPredicate(Enchantment enchantment, int minLevel, int maxLevel){
        this.enchantment = enchantment;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    @Override
    public boolean test(ItemStack stack){
        int level = stack.getEnchantments().getLevel(this.enchantment);
        return level >= this.minLevel && level <= this.maxLevel;
    }

    @Override
    public Serializer<? extends ItemPredicate> getSerializer(){
        return SERIALIZER;
    }
}
