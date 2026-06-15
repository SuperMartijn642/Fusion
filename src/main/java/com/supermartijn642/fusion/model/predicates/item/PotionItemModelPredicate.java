package com.supermartijn642.fusion.model.predicates.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.Objects;
import java.util.Optional;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class PotionItemModelPredicate implements ItemModelPredicate {

    public static ItemModelPredicate create(Potion potion){
        return create(BuiltInRegistries.POTION.wrapAsHolder(potion));
    }

    public static ItemModelPredicate create(Holder<Potion> potion){
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
            Identifier potionIdentifier = Identifier.parse(json.get("potion").getAsString());
            Optional<Holder.Reference<Potion>> potion = BuiltInRegistries.POTION.get(potionIdentifier);
            if(potion.isEmpty())
                throw new JsonParseException("Unknown potion '" + potionIdentifier + "'!");
            return new PotionItemModelPredicate(potion.get());
        }

        @Override
        public JsonObject serialize(PotionItemModelPredicate value){
            JsonObject json = new JsonObject();
            json.addProperty("potion", value.potion.unwrapKey().get().identifier().toString());
            return json;
        }
    };

    private final Holder<Potion> potion;

    private PotionItemModelPredicate(Holder<Potion> potion){
        this.potion = potion;
    }

    @Override
    public boolean test(ItemStack stack){
        PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
        return potion != null && potion.is(this.potion);
    }

    @Override
    public Serializer<? extends ItemModelPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(!(o instanceof PotionItemModelPredicate that)) return false;

        return Objects.equals(this.potion, that.potion);
    }

    @Override
    public int hashCode(){
        return Objects.hashCode(this.potion);
    }
}
