package com.supermartijn642.fusion.model.types.composite;

import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Created 15/06/2026 by SuperMartijn642
 */
public class CompositeItemModel implements ItemModel {

    private final List<ConditionalList> entries;

    public CompositeItemModel(List<ConditionalList> entries){
        this.entries = entries;
    }

    @Override
    public void update(ItemStackRenderState renderState, ItemStack stack, ItemModelResolver modelResolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed){
        renderState.appendModelIdentityElement(this);
        renderState.ensureCapacity(this.entries.size());
        for(ConditionalList list : this.entries){
            ItemModel model = list.get(stack);
            if(model != null)
                model.update(renderState, stack, modelResolver, displayContext, level, owner, seed);
        }
    }

    public record ConditionalList(List<ModelEntry> entries) {
        @Nullable
        ItemModel get(ItemStack stack){
            for(ModelEntry entry : this.entries){
                if(entry.predicate == null || entry.predicate.testForItem(stack))
                    return entry.model;
            }
            return null;
        }
    }

    public record ModelEntry(ItemModel model, ModelPredicate predicate) {
    }
}
