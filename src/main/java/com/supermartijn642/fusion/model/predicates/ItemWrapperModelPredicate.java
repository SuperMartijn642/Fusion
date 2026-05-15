package com.supermartijn642.fusion.model.predicates;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.DefaultModelPredicates;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.api.model.predicates.item.FusionItemModelPredicateRegistry;
import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created 14/05/2026 by SuperMartijn642
 */
public class ItemWrapperModelPredicate implements ModelPredicate {

    public static ModelPredicate create(ItemModelPredicate predicate){
        return new ItemWrapperModelPredicate(predicate);
    }

    public static final Serializer<ItemWrapperModelPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public ItemWrapperModelPredicate deserialize(JsonObject json) throws JsonParseException{
            return new ItemWrapperModelPredicate(FusionItemModelPredicateRegistry.deserializeItemModelPredicate(json));
        }

        @Override
        public JsonObject serialize(ItemWrapperModelPredicate value){
            return FusionItemModelPredicateRegistry.serializeItemModelPredicate(value.predicate);
        }
    };
    private static final Map<Item,ItemStack> DEFAULT_ITEM_INSTANCES = Collections.synchronizedMap(new HashMap<>()); // TODO this is not great

    private final ItemModelPredicate predicate;

    private ItemWrapperModelPredicate(ItemModelPredicate predicate){
        this.predicate = predicate;
    }

    public ItemModelPredicate getPredicate(){
        return this.predicate;
    }

    @Override
    public boolean testForBlockState(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state){
        ItemStack stack = ItemStack.EMPTY;
        if(state != null)
            stack = DEFAULT_ITEM_INSTANCES.computeIfAbsent(state.getBlock().asItem(), Item::getDefaultInstance);
        return this.testForItem(stack);
    }

    @Override
    public boolean testForItem(ItemStack stack){
        return this.predicate.test(stack);
    }

    @Override
    public ModelPredicate simplify(){
        ItemModelPredicate simplified = this.predicate.simplify();
        if(simplified.alwaysTrue())
            return DefaultModelPredicates.always();
        if(simplified.alwaysFalse())
            return DefaultModelPredicates.never();
        return new ItemWrapperModelPredicate(simplified);
    }

    @Override
    public Serializer<? extends ModelPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof ItemWrapperModelPredicate that)) return false;

        return this.predicate.equals(that.predicate);
    }

    @Override
    public int hashCode(){
        return this.predicate.hashCode();
    }
}
