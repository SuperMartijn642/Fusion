package com.supermartijn642.fusion.model.predicates;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.DefaultModelPredicates;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.api.model.predicates.blockstate.FusionBlockStateModelPredicateRegistry;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.jetbrains.annotations.Nullable;

/**
 * Created 14/05/2026 by SuperMartijn642
 */
public class BlockStateWrapperModelPredicate implements ModelPredicate {

    public static ModelPredicate create(BlockStateModelPredicate predicate){
        return new BlockStateWrapperModelPredicate(predicate);
    }

    public static final Serializer<BlockStateWrapperModelPredicate> SERIALIZER = new Serializer<BlockStateWrapperModelPredicate>() {
        @Override
        public BlockStateWrapperModelPredicate deserialize(JsonObject json) throws JsonParseException{
            return new BlockStateWrapperModelPredicate(FusionBlockStateModelPredicateRegistry.deserializeBlockStateModelPredicate(json));
        }

        @Override
        public JsonObject serialize(BlockStateWrapperModelPredicate value){
            return FusionBlockStateModelPredicateRegistry.serializeBlockStateModelPredicate(value.predicate);
        }
    };

    private final BlockStateModelPredicate predicate;

    private BlockStateWrapperModelPredicate(BlockStateModelPredicate predicate){
        this.predicate = predicate;
    }

    public BlockStateModelPredicate getPredicate(){
        return this.predicate;
    }

    @Override
    public boolean testForBlockState(@Nullable IBlockAccess level, @Nullable BlockPos pos, @Nullable IBlockState state){
        return this.predicate.test(level, pos, state);
    }

    @Override
    public boolean testForItem(ItemStack stack){
        IBlockState state = null;
        if(stack.getItem() instanceof ItemBlock)
            state = ((ItemBlock)stack.getItem()).getBlock().getDefaultState();
        return this.testForBlockState(null, null, state);
    }

    @Override
    public ModelPredicate simplify(){
        BlockStateModelPredicate simplified = this.predicate.simplify();
        if(simplified.alwaysTrue())
            return DefaultModelPredicates.always();
        if(simplified.alwaysFalse())
            return DefaultModelPredicates.never();
        return new BlockStateWrapperModelPredicate(simplified);
    }

    @Override
    public Serializer<? extends ModelPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof BlockStateWrapperModelPredicate)) return false;

        BlockStateWrapperModelPredicate that = (BlockStateWrapperModelPredicate)o;
        return this.predicate.equals(that.predicate);
    }

    @Override
    public int hashCode(){
        return this.predicate.hashCode();
    }
}
