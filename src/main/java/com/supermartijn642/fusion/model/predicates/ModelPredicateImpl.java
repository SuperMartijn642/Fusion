package com.supermartijn642.fusion.model.predicates;

import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Created 14/05/2026 by SuperMartijn642
 */
public class ModelPredicateImpl {

    public static ModelPredicate of(BlockStateModelPredicate predicate){
        return new ModelPredicate() {
            @Override
            public boolean testForBlock(@Nullable IBlockAccess level, @Nullable BlockPos pos, @Nullable IBlockState state){
                return predicate.test(level, pos, state);
            }

            @Override
            public boolean testForItem(ItemStack stack){
                IBlockState state = null;
                if(stack.getItem() instanceof ItemBlock)
                    state = ((ItemBlock)stack.getItem()).getBlock().getDefaultState();
                return this.testForBlock(null, null, state);
            }
        };
    }

    private static final Map<Item,ItemStack> DEFAULT_ITEM_INSTANCES = new HashMap<>(); // TODO this is not great

    public static ModelPredicate of(ItemModelPredicate predicate){
        return new ModelPredicate() {
            @Override
            public boolean testForBlock(@Nullable IBlockAccess level, @Nullable BlockPos pos, @Nullable IBlockState state){
                ItemStack stack = ItemStack.EMPTY;
                if(state != null)
                    stack = DEFAULT_ITEM_INSTANCES.computeIfAbsent(Item.getItemFromBlock(state.getBlock()), Item::getDefaultInstance);
                return this.testForItem(stack);
            }

            @Override
            public boolean testForItem(ItemStack stack){
                return predicate.test(stack);
            }
        };
    }

    public static ModelPredicate and(ModelPredicate... predicates){
        if(predicates.length == 0)
            return TRUE;
        return new ModelPredicate() {
            @Override
            public boolean testForBlock(@Nullable IBlockAccess level, @Nullable BlockPos pos, @Nullable IBlockState state){
                for(ModelPredicate predicate : predicates){
                    if(!predicate.testForBlock(level, pos, state))
                        return false;
                }
                return true;
            }

            @Override
            public boolean testForItem(ItemStack stack){
                for(ModelPredicate predicate : predicates){
                    if(!predicate.testForItem(stack))
                        return false;
                }
                return true;
            }
        };
    }

    public static ModelPredicate or(ModelPredicate... predicates){
        if(predicates.length == 0)
            return FALSE;
        return new ModelPredicate() {
            @Override
            public boolean testForBlock(@Nullable IBlockAccess level, @Nullable BlockPos pos, @Nullable IBlockState state){
                for(ModelPredicate predicate : predicates){
                    if(predicate.testForBlock(level, pos, state))
                        return true;
                }
                return false;
            }

            @Override
            public boolean testForItem(ItemStack stack){
                for(ModelPredicate predicate : predicates){
                    if(predicate.testForItem(stack))
                        return true;
                }
                return false;
            }
        };
    }

    public static ModelPredicate not(ModelPredicate predicate){
        if(predicate == TRUE)
            return FALSE;
        if(predicate == FALSE)
            return TRUE;
        return new ModelPredicate() {
            @Override
            public boolean testForBlock(@Nullable IBlockAccess level, @Nullable BlockPos pos, @Nullable IBlockState state){
                return !predicate.testForBlock(level, pos, state);
            }

            @Override
            public boolean testForItem(ItemStack stack){
                return !predicate.testForItem(stack);
            }
        };
    }

    private static final ModelPredicate TRUE = new ModelPredicate() {
        @Override
        public boolean testForBlock(@Nullable IBlockAccess level, @Nullable BlockPos pos, @Nullable IBlockState state){
            return true;
        }

        @Override
        public boolean testForItem(ItemStack stack){
            return true;
        }
    };
    private static final ModelPredicate FALSE = new ModelPredicate() {
        @Override
        public boolean testForBlock(@Nullable IBlockAccess level, @Nullable BlockPos pos, @Nullable IBlockState state){
            return false;
        }

        @Override
        public boolean testForItem(ItemStack stack){
            return false;
        }
    };
}
