package com.supermartijn642.fusion.model.modifiers.item;

import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.api.util.Pair;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Created 28/12/2024 by SuperMartijn642
 */
public class ItemModelModifierItemModel implements ItemModel {

    private final ItemModel defaultModel;
    private final List<Pair<ItemModelPredicate,BakedModel>> models;

    public ItemModelModifierItemModel(ItemModel defaultModel, List<Pair<ItemModelPredicate,BakedModel>> models){
        this.defaultModel = defaultModel;
        this.models = models;
    }

    private BakedModel forStack(ItemStack stack){
        for(Pair<ItemModelPredicate,BakedModel> entry : this.models){
            if(entry.left().test(stack))
                return entry.right();
        }
        return null;
    }

    @Override
    public void update(ItemStackRenderState renderState, ItemStack stack, ItemModelResolver modelResolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable LivingEntity entity, int i){
        BakedModel model = this.forStack(stack);
        if(model == null)
            this.defaultModel.update(renderState, stack, modelResolver, displayContext, level, entity, i);
        else{
            ItemStackRenderState.FoilType foilType = stack.hasFoil() ?
                BlockModelWrapper.hasSpecialAnimatedTexture(stack) ? ItemStackRenderState.FoilType.SPECIAL : ItemStackRenderState.FoilType.STANDARD
                : null;
            //noinspection removal
            model.getRenderPasses(stack).forEach(pass -> {
                ItemStackRenderState.LayerRenderState layer = renderState.newLayer();
                if(foilType != null)
                    layer.setFoilType(foilType);
                layer.setupBlockModel(pass, pass.getRenderType(stack));
            });
        }
    }
}
