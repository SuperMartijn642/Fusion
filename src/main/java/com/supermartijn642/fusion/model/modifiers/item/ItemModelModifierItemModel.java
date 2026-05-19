package com.supermartijn642.fusion.model.modifiers.item;

import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Created 28/12/2024 by SuperMartijn642
 */
public class ItemModelModifierItemModel implements ItemModel {

    private final ItemModel original;
    private final List<ConditionalModel> defaultModelOverrides;
    private final List<List<ConditionalModel>> appendModels;

    ItemModelModifierItemModel(ItemModel original, List<ConditionalModel> defaultModelOverrides, List<List<ConditionalModel>> appendModels){
        this.original = original;
        this.defaultModelOverrides = defaultModelOverrides;
        this.appendModels = appendModels;
    }

    @Override
    public void update(ItemStackRenderState renderState, ItemStack stack, ItemModelResolver modelResolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed){
        renderState.ensureCapacity(this.appendModels.size());

        // Default model
        overrides:
        {
            for(ConditionalModel override : this.defaultModelOverrides){
                if(override.conditions == null || override.conditions.test(stack)){
                    override.model.update(renderState, stack, modelResolver, displayContext, level, owner, seed);
                    break overrides;
                }
            }
            this.original.update(renderState, stack, modelResolver, displayContext, level, owner, seed);
        }

        // Append models
        for(List<ConditionalModel> appendEntry : this.appendModels){
            // First model whose conditions are met is submitted
            for(ConditionalModel conditional : appendEntry){
                if(conditional.conditions == null || conditional.conditions.test(stack)){
                    conditional.model.update(renderState, stack, modelResolver, displayContext, level, owner, seed);
                    break;
                }
            }
        }
    }

    record ConditionalModel(ItemModel model, @Nullable ItemModelPredicate conditions) {
    }
}
