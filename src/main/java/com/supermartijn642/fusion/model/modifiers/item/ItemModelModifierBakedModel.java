package com.supermartijn642.fusion.model.modifiers.item;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.datafixers.util.Pair;
import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.model.WrappedBakedModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

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

    @Override
    public List<Pair<IBakedModel,RenderType>> getLayerModels(ItemStack stack, boolean fabulous){
        // Collect all models
        List<Pair<IBakedModel,RenderType>> models = new ArrayList<>(Math.min(10, this.appendModels.size() + 1));

        // Default model
        overrides:
        {
            for(ConditionalModel override : this.defaultModelOverrides){
                if(override.conditions == null || override.conditions.test(stack)){
                    models.addAll(override.model.getLayerModels(stack, fabulous));
                    break overrides;
                }
            }
            models.addAll(this.original.getLayerModels(stack, fabulous));
        }

        // Append models
        for(List<ConditionalModel> appendEntry : this.appendModels){
            // First model whose conditions are met is submitted
            for(ConditionalModel conditional : appendEntry){
                if(conditional.conditions == null || conditional.conditions.test(stack)){
                    models.addAll(conditional.model.getLayerModels(stack, fabulous));
                    break;
                }
            }
        }
        return models;
    }

    @Override
    public boolean isLayered(){
        return true;
    }

    @Override
    public IBakedModel handlePerspective(ItemCameraTransforms.TransformType transformType, MatrixStack poseStack){
        super.handlePerspective(transformType, poseStack);
        return this;
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
