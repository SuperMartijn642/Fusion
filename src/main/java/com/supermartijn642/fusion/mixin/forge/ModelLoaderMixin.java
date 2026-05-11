package com.supermartijn642.fusion.mixin.forge;

import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierReloadListener;
import com.supermartijn642.fusion.model.modifiers.item.ItemModelModifierReloadListener;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
@Mixin(ModelLoader.class)
public class ModelLoaderMixin {

    @Shadow(remap = false)
    @Final
    private Map<ModelResourceLocation,IModel> stateModels;
    @Shadow(remap = false)
    @Final
    private Map<ResourceLocation,Exception> loadingExceptions;

    @Inject(
        method = "setupModelRegistry",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/client/model/ModelLoader;loadVariantItemModels()V",
            shift = At.Shift.AFTER
        )
    )
    private void loadModelsInject(CallbackInfoReturnable<?> ci){
        Set<ModelResourceLocation> models = new HashSet<>(BlockModelModifierReloadListener.INSTANCE.registerOverlays());
        models.addAll(ItemModelModifierReloadListener.INSTANCE.registerPredicateModels());
        for(ModelResourceLocation modelLocation : models){
            IModel model;
            try{
                model = ModelLoaderRegistry.getModel(new ResourceLocation(modelLocation.getResourceDomain(), modelLocation.getResourcePath()));
            }catch(Exception e){
                this.loadingExceptions.put(modelLocation, e);
                model = ModelLoaderRegistry.getMissingModel();
            }
            this.stateModels.put(modelLocation, model);
        }
    }

    @Inject(
        method = "setupModelRegistry",
        at = @At("RETURN")
    )
    private void applyBakedModels(CallbackInfoReturnable<?> ci){
        //noinspection DataFlowIssue
        ModelBakery bakery = (ModelBakery)(Object)this;
        BlockModelModifierReloadListener.INSTANCE.applyOverlays(bakery);
        ItemModelModifierReloadListener.INSTANCE.applyPredicateModels(bakery);
    }
}
