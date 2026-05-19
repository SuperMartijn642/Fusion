package com.supermartijn642.fusion.model.modifiers.item;

import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.model.ModelRenderTypeHelper;
import com.supermartijn642.fusion.model.WrappedBakedModel;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Direction;
import net.minecraftforge.client.model.data.IModelData;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.vecmath.Matrix4f;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, @NotNull Random random){
        // Get item stack context
        ItemStack stack = FusionClient.ITEM_STACK_RENDER_CONTEXT.get();

        // Get seed to reset random instance
        long seed = random.nextLong();
        random.setSeed(seed);

        // Collect all quads
        List<BakedQuad> quads = new ArrayList<>();

        // Default model
        overrides:
        {
            for(ConditionalModel override : this.defaultModelOverrides){
                if(override.conditions == null || override.conditions.test(stack)){
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
                if(conditional.conditions == null || conditional.conditions.test(stack)){
                    random.setSeed(seed);
                    quads.addAll(conditional.model.getQuads(state, cullDirection, random));
                    break;
                }
            }
        }
        return quads;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, Random random, IModelData modelData){
        return this.getQuads(state, cullDirection, random);
    }

    @Override
    public boolean canRenderInLayer(BlockState state, BlockRenderLayer renderType){
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

        ConditionalModel(IBakedModel model, @Nullable ItemModelPredicate conditions){
            this.model = model;
            this.conditions = conditions;
        }
    }
}
