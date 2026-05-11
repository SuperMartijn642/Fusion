package com.supermartijn642.fusion.mixin.forge;

import com.supermartijn642.fusion.api.util.UserErrorException;
import com.supermartijn642.fusion.util.LoggingHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * Created 05/06/2026 by SuperMartijn642
 */
@Mixin(ModelLoaderRegistry.class)
public class ModelLoaderRegistryMixin {

    @Shadow(remap = false)
    private static IModel getMissingModel(ResourceLocation location, Throwable cause){
        throw new AssertionError();
    }

    @Redirect(
        method = "getModel",
        at = @At(
            value = "NEW",
            target = "Lnet/minecraftforge/client/model/ModelLoaderRegistry$LoaderException;",
            ordinal = 4,
            remap = false
        ),
        remap = false
    )
    private static ModelLoaderRegistry.LoaderException rethrowFusionErrors(String message, Throwable e, ResourceLocation location) throws UserErrorException{
        // Report Fusion model user errors in a more readable way
        if(e instanceof UserErrorException)
            throw new UserErrorException("Failed to load model '" + location + "':", e);
        return new ModelLoaderRegistry.LoaderException(message, e);
    }

    @Inject(
        method = "getModelOrLogError",
        at = @At(
            value = "INVOKE",
            target = "Lorg/apache/logging/log4j/Logger;error(Ljava/lang/String;Ljava/lang/Throwable;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true,
        locals = LocalCapture.CAPTURE_FAILHARD,
        remap = false
    )
    private static void interceptFusionErrors(ResourceLocation location, String error, CallbackInfoReturnable<IModel> ci, Exception e){
        // Report Fusion model user errors in a more readable way
        if(e instanceof UserErrorException){
            LoggingHelper.logUserError(e.getCause(), e.getMessage());
            ci.setReturnValue(getMissingModel(location, e.getCause()));
        }
    }
}
