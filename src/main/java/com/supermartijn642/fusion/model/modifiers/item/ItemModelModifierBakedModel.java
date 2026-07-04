package com.supermartijn642.fusion.model.modifiers.item;

import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.model.ModelRenderTypeHelper;
import com.supermartijn642.fusion.model.WrappedBakedModel;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.vecmath.Matrix4f;
import java.util.ArrayList;
import java.util.List;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class ItemModelModifierBakedModel extends WrappedBakedModel {

    private final IBakedModel original;
    private final List<ConditionalModel> defaultModelOverrides;
    private final List<List<ConditionalModel>> appendModels;

    ItemModelModifierBakedModel(IBakedModel original, List<ConditionalModel> defaultModelOverrides, List<List<ConditionalModel>> appendModels){
        super(original);
        this.original = original;
        this.defaultModelOverrides = defaultModelOverrides;
        this.appendModels = appendModels;
    }

    public IBakedModel preselectModel(ItemStack stack){
        for(ConditionalModel override : this.defaultModelOverrides){
            if(override.conditions == null || override.conditions.test(stack))
                return override.wrapper(this);
        }
        return this;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing cullDirection, long seed){
        // Get item stack context
        ItemStack stack = FusionClient.ITEM_STACK_RENDER_CONTEXT.get();

        // Collect all quads
        List<BakedQuad> quads = new ArrayList<>();

        // Default model
        overrides:
        {
            for(ConditionalModel override : this.defaultModelOverrides){
                if(override.conditions == null || override.conditions.test(stack)){
                    quads.addAll(override.model.getQuads(state, cullDirection, seed));
                    break overrides;
                }
            }
            quads.addAll(this.original.getQuads(state, cullDirection, seed));
        }

        // Append models
        for(List<ConditionalModel> appendEntry : this.appendModels){
            // First model whose conditions are met is submitted
            for(ConditionalModel conditional : appendEntry){
                if(conditional.conditions == null || conditional.conditions.test(stack)){
                    quads.addAll(conditional.model.getQuads(state, cullDirection, seed));
                    break;
                }
            }
        }
        return quads;
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer renderType){
        // Check whether the render type is a default one for the state
        boolean isDefaultRenderType = ModelRenderTypeHelper.couldBlockRenderInLayerOriginally(state, renderType);

        // Default model
        for(ConditionalModel override : this.defaultModelOverrides){
            if(ModelRenderTypeHelper.canRenderInLayer(override.model, state, renderType, isDefaultRenderType))
                return true;
        }
        if(ModelRenderTypeHelper.canRenderInLayer(this.original, state, renderType, isDefaultRenderType))
            return true;

        // Append models
        for(List<ConditionalModel> appendEntry : this.appendModels){
            for(ConditionalModel conditional : appendEntry){
                if(ModelRenderTypeHelper.canRenderInLayer(conditional.model, state, renderType, isDefaultRenderType))
                    return true;
            }
        }
        return false;
    }

    @Override
    public Pair<? extends IBakedModel,Matrix4f> handlePerspective(ItemCameraTransforms.TransformType transformType){
        Pair<? extends IBakedModel,Matrix4f> pair = super.handlePerspective(transformType);
        return Pair.of(this, pair.getRight());
    }

    static final class ConditionalModel {
        private final IBakedModel model;
        private final @Nullable ItemModelPredicate conditions;
        private SelectedDefaultModel wrapper;

        public ConditionalModel(IBakedModel model, @Nullable ItemModelPredicate conditions){
            this.model = model;
            this.conditions = conditions;
        }

        public IBakedModel wrapper(ItemModelModifierBakedModel parent){
            if(this.wrapper == null)
                this.wrapper = parent.new SelectedDefaultModel(this.model);
            return this.wrapper;
        }
    }

    private class SelectedDefaultModel extends WrappedBakedModel {
        private final IBakedModel mainModel;

        SelectedDefaultModel(IBakedModel mainModel){
            super(mainModel);
            this.mainModel = mainModel;
        }

        @Override
        public @NotNull List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing cullDirection, long seed){
            // Get item stack context
            ItemStack stack = FusionClient.ITEM_STACK_RENDER_CONTEXT.get();

            // Collect all quads
            List<BakedQuad> quads = new ArrayList<>();

            // Default model
            quads.addAll(this.mainModel.getQuads(state, cullDirection, seed));

            // Append models
            for(List<ConditionalModel> appendEntry : ItemModelModifierBakedModel.this.appendModels){
                // First model whose conditions are met is submitted
                for(ConditionalModel conditional : appendEntry){
                    if(conditional.conditions == null || conditional.conditions.test(stack)){
                        quads.addAll(conditional.model.getQuads(state, cullDirection, seed));
                        break;
                    }
                }
            }
            return quads;
        }

        @Override
        public boolean canRenderInLayer(IBlockState state, BlockRenderLayer renderType){
            // Check whether the render type is a default one for the state
            boolean isDefaultRenderType = ModelRenderTypeHelper.couldBlockRenderInLayerOriginally(state, renderType);

            // Default model
            if(ModelRenderTypeHelper.canRenderInLayer(this.mainModel, state, renderType, isDefaultRenderType))
                return true;

            // Append models
            for(List<ConditionalModel> appendEntry : ItemModelModifierBakedModel.this.appendModels){
                for(ConditionalModel conditional : appendEntry){
                    if(ModelRenderTypeHelper.canRenderInLayer(conditional.model, state, renderType, isDefaultRenderType))
                        return true;
                }
            }
            return false;
        }
    }
}
