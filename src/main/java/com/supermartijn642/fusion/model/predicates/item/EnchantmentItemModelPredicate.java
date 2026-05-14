package com.supermartijn642.fusion.model.predicates.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class EnchantmentItemModelPredicate implements ItemModelPredicate {

    public static final Serializer<EnchantmentItemModelPredicate> SERIALIZER = new Serializer<EnchantmentItemModelPredicate>() {
        @Override
        public EnchantmentItemModelPredicate deserialize(JsonObject json) throws JsonParseException{
            // Enchantment
            if(!json.has("enchantment") || !json.get("enchantment").isJsonPrimitive() || !json.getAsJsonPrimitive("enchantment").isString())
                throw new JsonParseException("Item-predicate must have string property 'enchantment'!");
            if(!IdentifierUtil.isValidIdentifier(json.get("enchantment").getAsString()))
                throw new JsonParseException("Property 'enchantment' must be a valid identifier, not '" + json.get("enchantment").getAsString() + "'!");
            ResourceLocation enchantment = new ResourceLocation(json.get("enchantment").getAsString());
            if(!Registry.ENCHANTMENT.containsKey(enchantment))
                throw new JsonParseException("Unknown enchantment '" + enchantment + "'!");

            // Maximum level
            int maxLevel = 255;
            if(json.has("max_level")){
                if(!json.get("max_level").isJsonPrimitive() || !json.getAsJsonPrimitive("max_level").isNumber())
                    throw new JsonParseException("Property 'max_level' must be a number!");
                maxLevel = json.getAsJsonPrimitive("max_level").getAsInt();
                if(maxLevel < 0 || maxLevel > 255)
                    throw new JsonParseException("Property 'max_level' must be between 0 and 255!");
            }

            // Minimum level
            int minLevel = maxLevel == 0 ? 0 : 1;
            if(json.has("min_level")){
                if(!json.get("min_level").isJsonPrimitive() || !json.getAsJsonPrimitive("min_level").isNumber())
                    throw new JsonParseException("Property 'min_level' must be a number!");
                minLevel = json.getAsJsonPrimitive("min_level").getAsInt();
                if(minLevel < 0 || minLevel > 255)
                    throw new JsonParseException("Property 'min_level' must be between 0 and 255!");
            }

            // Validate min level <= max level
            if(minLevel > maxLevel)
                throw new JsonParseException("Property 'min_level' must be less than or equal to 'max_level'!");
            return new EnchantmentItemModelPredicate(enchantment, minLevel, maxLevel);
        }

        @Override
        public JsonObject serialize(EnchantmentItemModelPredicate value){
            JsonObject json = new JsonObject();
            json.addProperty("enchantment", value.enchantment.toString());
            if(value.maxLevel != 0 && value.minLevel != 1)
                json.addProperty("min_level", value.minLevel);
            if(value.maxLevel != 255)
                json.addProperty("max_level", value.maxLevel);
            return json;
        }
    };

    private final ResourceLocation enchantment;
    private final int minLevel, maxLevel;

    public EnchantmentItemModelPredicate(ResourceLocation enchantment, int minLevel, int maxLevel){
        if(enchantment == null)
            throw new NullPointerException("Enchantment must not be null!");
        if(minLevel < 0 || minLevel > 255)
            throw new IllegalArgumentException("Min level must be between 0 and 255!");
        if(maxLevel < 0 || maxLevel > 255)
            throw new IllegalArgumentException("Max level must be between 0 and 255!");
        if(minLevel > maxLevel)
            throw new IllegalArgumentException("Minimum level must be less than or equal to maximum level!");
        this.enchantment = enchantment;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    @Override
    public boolean test(ItemStack stack){
        if(!stack.hasTag())
            return this.minLevel == 0;
        ListNBT enchantmentsTag = stack.getItem() == Items.ENCHANTED_BOOK ? EnchantedBookItem.getEnchantments(stack) : stack.getEnchantmentTags();
        if(!enchantmentsTag.isEmpty()){
            for(int i = 0; i < enchantmentsTag.size(); i++){
                CompoundNBT tag = enchantmentsTag.getCompound(i);
                int level = MathHelper.clamp(tag.getInt("lvl"), 0, 255);
                if(level >= this.minLevel && level <= this.maxLevel && this.enchantment.equals(ResourceLocation.tryParse(tag.getString("id"))))
                    return true;
            }
        }
        return this.minLevel == 0;
    }

    @Override
    public Serializer<? extends ItemModelPredicate> getSerializer(){
        return SERIALIZER;
    }
}
