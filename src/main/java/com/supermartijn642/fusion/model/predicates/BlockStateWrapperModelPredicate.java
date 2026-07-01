package com.supermartijn642.fusion.model.predicates;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.predicates.DefaultModelPredicates;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.api.model.predicates.blockstate.FusionBlockStateModelPredicateRegistry;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
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
    public boolean testForBlockState(@Nullable IBlockDisplayReader level, @Nullable BlockPos pos, @Nullable BlockState state){
        return this.predicate.test(level, pos, state);
    }

    @Override
    public boolean testForItem(ItemStack stack){
        BlockState state = null;
        if(stack.getItem() instanceof BlockItem)
            state = ((BlockItem)stack.getItem()).getBlock().defaultBlockState();
        return this.testForBlockState(null, null, state);
    }

    @Override
    public ModelPredicate applyTransform(ModelTransform transform){
        return new BlockStateWrapperModelPredicate(this.predicate.applyTransform(transform));
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
