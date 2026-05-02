package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.model.FusionBlockModel;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Created 09/04/2025 by SuperMartijn642
 */
@Mixin(CuboidItemModelWrapper.Unbaked.class)
public class CuboidItemModelWrapperMixin {

    @Inject(
        method = "bake",
        at = @At("HEAD"),
        cancellable = true
    )
    private void bake(ItemModel.BakingContext context, Matrix4fc transformation, CallbackInfoReturnable<ItemModel> ci){
        //noinspection DataFlowIssue
        CuboidItemModelWrapper.Unbaked unbaked = (CuboidItemModelWrapper.Unbaked)(Object)this;
        Identifier location = unbaked.model();
        ResolvedModel wrapper = context.blockModelBaker().getModel(location);
        if(wrapper != null){
            UnbakedModel wrapped = wrapper.wrapped();
            if(wrapped instanceof FusionBlockModel fusionBlockModel){
                List<ItemTintSource> tintSources = unbaked.tints();
                ItemModel model = fusionBlockModel.bakeItemModel(wrapper, context.blockModelBaker(), tintSources, context.entityModelSet(), transformation);
                ci.setReturnValue(model);
            }
        }
    }
}
