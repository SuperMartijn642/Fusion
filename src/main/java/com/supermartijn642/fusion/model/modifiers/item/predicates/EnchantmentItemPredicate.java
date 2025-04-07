package com.supermartijn642.fusion.model.modifiers.item.predicates;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.modifier.item.ItemPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

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
            ResourceLocation enchantment = ResourceLocation.parse(json.get("enchantment").getAsString());

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
            return new EnchantmentItemPredicate(enchantment, minLevel, maxLevel);
        }

        @Override
        public JsonObject serialize(EnchantmentItemPredicate value){
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
    private RegistryAccess registry;
    private Holder<Enchantment> holder;

    public EnchantmentItemPredicate(ResourceLocation enchantment, int minLevel, int maxLevel){
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
        ItemEnchantments enchantments = stack.getEnchantments();
        if(enchantments.isEmpty())
            return this.minLevel == 0;
        if(Minecraft.getInstance().level == null)
            return this.minLevel == 0;
        if(Minecraft.getInstance().level.registryAccess() != this.registry){
            this.registry = Minecraft.getInstance().level.registryAccess();
            this.holder = this.registry.lookupOrThrow(Registries.ENCHANTMENT).get(this.enchantment).orElse(null);
        }
        if(this.holder == null)
            return this.minLevel == 0;
        int level = enchantments.getLevel(this.holder);
        return level >= this.minLevel && level <= this.maxLevel;
    }

    @Override
    public Serializer<? extends ItemPredicate> getSerializer(){
        return SERIALIZER;
    }
}
