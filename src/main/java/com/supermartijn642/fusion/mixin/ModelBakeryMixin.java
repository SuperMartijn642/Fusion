package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.api.util.UserErrorException;
import com.supermartijn642.fusion.model.FusionBlockModelData;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierReloadListener;
import com.supermartijn642.fusion.model.modifiers.item.ItemModelModifierReloadListener;
import com.supermartijn642.fusion.util.LoggingHelper;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Created 22/09/2024 by SuperMartijn642
 */
@Mixin(ModelBakery.class)
public class ModelBakeryMixin {

    @Inject(
        method = "loadTopLevel",
        at = @At("HEAD")
    )
    private void registerBlockModelOverlays(ModelResourceLocation location, CallbackInfo ci){
        if(location == ModelBakery.MISSING_MODEL_LOCATION){
            //noinspection DataFlowIssue
            ModelBakery modelBakery = (ModelBakery)(Object)this;
            BlockModelModifierReloadListener.INSTANCE.registerModelDependencies(modelBakery);
            ItemModelModifierReloadListener.INSTANCE.registerModelDependencies(modelBakery);
        }
    }

    @Inject(
        method = "loadBlockModel",
        at = @At("HEAD")
    )
    private void storeBlockModelName(ResourceLocation identifier, CallbackInfoReturnable<?> ci){
        FusionBlockModelData.CURRENT_MODEL.set(identifier);
    }

    @Inject(
        method = "loadBlockModel",
        at = @At("RETURN")
    )
    private void clearBlockModelName(ResourceLocation identifier, CallbackInfoReturnable<?> ci){
        FusionBlockModelData.CURRENT_MODEL.remove();
    }

    @Redirect(
        method = "getModel(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/resources/model/UnbakedModel;",
        at = @At(
            value = "INVOKE",
            target = "Lorg/apache/logging/log4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V"
        )
    )
    private void interceptFusionErrors(Logger logger, String message, Object identifier, Object ignore, Object exception){
        // Report Fusion model user errors in a more readable way
        Exception e = (Exception)exception;
        if(e instanceof UserErrorException){
            LoggingHelper.logUserError(e.getCause(), "Failed to load model '%s':", identifier);
            return;
        }
        logger.warn(message, identifier, ignore, exception);
    }
}
