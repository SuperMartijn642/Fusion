package com.supermartijn642.fusion.model.modifiers.item;

import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.api.util.Pair;
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
 * Created 28/12/2024 by SuperMartijn642
 */
public class ItemModelModifierItemModel implements ItemModel {

    private final ItemModel defaultModel;
    private final List<Pair<ItemModelPredicate,ItemModel>> models;

    public ItemModelModifierItemModel(ItemModel defaultModel, List<Pair<ItemModelPredicate,ItemModel>> models){
        this.defaultModel = defaultModel;
        this.models = models;
    }

    private ItemModel forStack(ItemStack stack){
        for(Pair<ItemModelPredicate,ItemModel> entry : this.models){
            if(entry.left().test(stack))
                return entry.right();
        }
        return null;
    }

    @Override
    public void update(ItemStackRenderState renderState, ItemStack stack, ItemModelResolver modelResolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int i){
        ItemModel model = this.forStack(stack);
        if(model == null)
            this.defaultModel.update(renderState, stack, modelResolver, displayContext, level, owner, i);
        else
            model.update(renderState, stack, modelResolver, displayContext, level, owner, i);
    }
}
