package com.supermartijn642.fusion.mixin.forge;

import com.supermartijn642.fusion.api.util.UserErrorException;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierReloadListener;
import com.supermartijn642.fusion.model.modifiers.item.ItemModelModifierReloadListener;
import com.supermartijn642.fusion.util.LoggingHelper;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

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
        Consumer<ModelResourceLocation> modelLoader = identifier -> {
            IModel model;
            try{
                model = ModelLoaderRegistry.getModel(new ResourceLocation(identifier.getResourceDomain(), identifier.getResourcePath()));
            }catch(Exception e){
                this.loadingExceptions.put(identifier, e);
                model = ModelLoaderRegistry.getMissingModel();
            }
            this.stateModels.put(identifier, model);
        };
        BlockModelModifierReloadListener.INSTANCE.gatherModelDependencies().forEach(modelLoader);
        ItemModelModifierReloadListener.INSTANCE.gatherModelDependencies().forEach(modelLoader);
    }

    @Inject(
        method = "setupModelRegistry",
        at = @At("RETURN")
    )
    private void applyBakedModels(CallbackInfoReturnable<?> ci){
        //noinspection DataFlowIssue
        ModelBakery bakery = (ModelBakery)(Object)this;
        BlockModelModifierReloadListener.INSTANCE.applyModelModifiers(bakery);
        ItemModelModifierReloadListener.INSTANCE.applyModelModifiers(bakery);
    }

    @Inject(
        method = "onPostBakeEvent",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/registry/IRegistry;getObject(Ljava/lang/Object;)Ljava/lang/Object;",
            shift = At.Shift.BEFORE,
            ordinal = 0
        )
    )
    private void interceptFusionErrors(CallbackInfo ci){
        Iterator<Map.Entry<ResourceLocation,Exception>> iterator = this.loadingExceptions.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<ResourceLocation,Exception> entry = iterator.next();
            if(!(entry.getKey() instanceof ModelResourceLocation))
                continue;
            // Report Fusion model user errors in a more readable way
            Exception e = entry.getValue();
            if(e instanceof UserErrorException){
                LoggingHelper.logUserError(e.getCause(), e.getMessage());
                iterator.remove();
            }
        }
    }
}
