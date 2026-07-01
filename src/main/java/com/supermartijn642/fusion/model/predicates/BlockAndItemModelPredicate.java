package com.supermartijn642.fusion.model.predicates;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.predicates.DefaultModelPredicates;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.api.model.predicates.blockstate.FusionBlockStateModelPredicateRegistry;
import com.supermartijn642.fusion.api.model.predicates.item.FusionItemModelPredicateRegistry;
import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created 14/05/2026 by SuperMartijn642
 */
public class BlockAndItemModelPredicate implements ModelPredicate {

    public static ModelPredicate create(BlockStateModelPredicate blockPredicate, ItemModelPredicate itemPredicate){
        Objects.requireNonNull(blockPredicate);
        Objects.requireNonNull(itemPredicate);
        return new BlockAndItemModelPredicate(blockPredicate, itemPredicate);
    }

    public static final Serializer<BlockAndItemModelPredicate> SERIALIZER = new Serializer<BlockAndItemModelPredicate>() {
        @Override
        public BlockAndItemModelPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("block") || !json.get("block").isJsonObject())
                throw new JsonParseException("Block-and-item predicate must have object property 'block'!");
            if(!json.has("item") || !json.get("item").isJsonObject())
                throw new JsonParseException("Block-and-item predicate must have object property 'item'!");
            BlockStateModelPredicate blockPredicate = FusionBlockStateModelPredicateRegistry.deserializeBlockStateModelPredicate(json.getAsJsonObject("block"));
            ItemModelPredicate itemPredicate = FusionItemModelPredicateRegistry.deserializeItemModelPredicate(json.getAsJsonObject("item"));
            return new BlockAndItemModelPredicate(blockPredicate, itemPredicate);
        }

        @Override
        public JsonObject serialize(BlockAndItemModelPredicate value){
            JsonObject json = new JsonObject();
            json.add("block", FusionBlockStateModelPredicateRegistry.serializeBlockStateModelPredicate(value.blockPredicate));
            json.add("item", FusionItemModelPredicateRegistry.serializeItemModelPredicate(value.itemPredicate));
            return json;
        }
    };
    private static final Map<Item,ItemStack> DEFAULT_ITEM_INSTANCES = Collections.synchronizedMap(new HashMap<>()); // TODO this is not great

    private final BlockStateModelPredicate blockPredicate;
    private final ItemModelPredicate itemPredicate;

    private BlockAndItemModelPredicate(BlockStateModelPredicate blockPredicate, ItemModelPredicate itemPredicate){
        this.blockPredicate = blockPredicate;
        this.itemPredicate = itemPredicate;
    }

    @Override
    public boolean testForBlockState(@Nullable IBlockDisplayReader level, @Nullable BlockPos pos, @Nullable BlockState state){
        return this.blockPredicate.test(level, pos, state);
    }

    @Override
    public boolean testForItem(ItemStack stack){
        return this.itemPredicate.test(stack);
    }

    @Override
    public ModelPredicate applyTransform(ModelTransform transform){
        return new BlockAndItemModelPredicate(
            this.blockPredicate.applyTransform(transform),
            this.itemPredicate
        );
    }

    @Override
    public ModelPredicate simplify(){
        BlockStateModelPredicate simplifiedBlock = this.blockPredicate.simplify();
        ItemModelPredicate simplifiedItem = this.itemPredicate.simplify();
        if(simplifiedBlock.alwaysTrue() && simplifiedItem.alwaysTrue())
            return DefaultModelPredicates.always();
        if(simplifiedBlock.alwaysFalse() && simplifiedItem.alwaysFalse())
            return DefaultModelPredicates.never();
        return new BlockAndItemModelPredicate(simplifiedBlock, simplifiedItem);
    }

    @Override
    public Serializer<? extends ModelPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(!(o instanceof BlockAndItemModelPredicate)) return false;

        BlockAndItemModelPredicate that = (BlockAndItemModelPredicate)o;
        return this.blockPredicate.equals(that.blockPredicate) && this.itemPredicate.equals(that.itemPredicate);
    }

    @Override
    public int hashCode(){
        int result = this.blockPredicate.hashCode();
        result = 31 * result + this.itemPredicate.hashCode();
        return result;
    }
}
