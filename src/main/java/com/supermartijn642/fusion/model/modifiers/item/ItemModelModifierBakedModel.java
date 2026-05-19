package com.supermartijn642.fusion.model.modifiers.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.model.WrappedBakedModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    public List<Pair<BakedModel,RenderType>> getLayerModels(ItemStack stack, boolean fabulous){
        // Collect all models
        List<Pair<BakedModel,RenderType>> models = new ArrayList<>(Math.min(10, this.appendModels.size() + 1));

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
    public BakedModel handlePerspective(ItemTransforms.TransformType transformType, PoseStack poseStack){
        super.handlePerspective(transformType, poseStack);
        return this;
    }

    record ConditionalModel(BakedModel model, @Nullable ItemModelPredicate conditions) {
    }
}
