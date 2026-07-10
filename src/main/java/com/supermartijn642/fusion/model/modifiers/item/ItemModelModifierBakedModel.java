package com.supermartijn642.fusion.model.modifiers.item;

import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.model.WrappedBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class ItemModelModifierBakedModel extends WrappedBakedModel {

    private final BakedModel original;
    private final List<ConditionalModel> defaultModelOverrides;
    private final List<List<ConditionalModel>> appendModels;

    ItemModelModifierBakedModel(BakedModel original, List<ConditionalModel> defaultModelOverrides, List<List<ConditionalModel>> appendModels){
        super(original);
        this.original = original;
        this.defaultModelOverrides = defaultModelOverrides;
        this.appendModels = appendModels;
    }

    public BakedModel preselectModel(ItemStack stack){
        for(ConditionalModel override : this.defaultModelOverrides){
            if(override.conditions == null || override.conditions.test(stack))
                return override.wrapper(this);
        }
        return this;
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context){
        // Default model
        overrides:
        {
            for(ConditionalModel override : this.defaultModelOverrides){
                if(override.conditions == null || override.conditions.test(stack)){
                    ((FabricBakedModel)override.model).emitItemQuads(stack, randomSupplier, context);
                    break overrides;
                }
            }
            ((FabricBakedModel)this.original).emitItemQuads(stack, randomSupplier, context);
        }

        // Append models
        for(List<ConditionalModel> appendEntry : this.appendModels){
            // First model whose conditions are met is submitted
            for(ConditionalModel conditional : appendEntry){
                if(conditional.conditions == null || conditional.conditions.test(stack)){
                    ((FabricBakedModel)conditional.model).emitItemQuads(stack, randomSupplier, context);
                    break;
                }
            }
        }
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, RandomSource random){
        long seed = random.nextLong();
        random.setSeed(seed);

        // Collect all quads
        List<BakedQuad> quads = new ArrayList<>();

        // Default model
        overrides:
        {
            for(ConditionalModel override : this.defaultModelOverrides){
                if(override.conditions == null || override.conditions.test(ItemStack.EMPTY)){
                    quads.addAll(override.model.getQuads(state, cullDirection, random));
                    break overrides;
                }
            }
            quads.addAll(this.original.getQuads(state, cullDirection, random));
        }

        // Append models
        for(List<ConditionalModel> appendEntry : this.appendModels){
            // First model whose conditions are met is submitted
            for(ConditionalModel conditional : appendEntry){
                if(conditional.conditions == null || conditional.conditions.test(ItemStack.EMPTY)){
                    random.setSeed(seed);
                    quads.addAll(conditional.model.getQuads(state, cullDirection, random));
                    break;
                }
            }
        }
        return quads;
    }

    @Override
    public boolean isVanillaAdapter(){
        return false;
    }

    static class ConditionalModel {
        private final BakedModel model;
        private final @Nullable ItemModelPredicate conditions;
        private SelectedDefaultModel wrapper;

        public ConditionalModel(BakedModel model, @Nullable ItemModelPredicate conditions){
            this.model = model;
            this.conditions = conditions;
        }

        public BakedModel wrapper(ItemModelModifierBakedModel parent){
            if(this.wrapper == null)
                this.wrapper = parent.new SelectedDefaultModel(this.model);
            return this.wrapper;
        }
    }

    private class SelectedDefaultModel extends WrappedBakedModel {
        private final BakedModel mainModel;

        SelectedDefaultModel(BakedModel mainModel){
            super(mainModel);
            this.mainModel = mainModel;
        }

        @Override
        public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context){
            ((FabricBakedModel)this.mainModel).emitItemQuads(stack, randomSupplier, context);

            // Append models
            for(List<ConditionalModel> appendEntry : ItemModelModifierBakedModel.this.appendModels){
                // First model whose conditions are met is submitted
                for(ConditionalModel conditional : appendEntry){
                    if(conditional.conditions == null || conditional.conditions.test(stack)){
                        ((FabricBakedModel)conditional.model).emitItemQuads(stack, randomSupplier, context);
                        break;
                    }
                }
            }
        }

        @Override
        public boolean isVanillaAdapter(){
            return false;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, RandomSource random){
            long seed = random.nextLong();
            random.setSeed(seed);

            // Collect all quads
            List<BakedQuad> quads = new ArrayList<>(this.mainModel.getQuads(state, cullDirection, random));

            // Append models
            for(List<ConditionalModel> appendEntry : ItemModelModifierBakedModel.this.appendModels){
                // First model whose conditions are met is submitted
                for(ConditionalModel conditional : appendEntry){
                    if(conditional.conditions == null || conditional.conditions.test(ItemStack.EMPTY)){
                        random.setSeed(seed);
                        quads.addAll(conditional.model.getQuads(state, cullDirection, random));
                        break;
                    }
                }
            }
            return quads;
        }
    }
}
