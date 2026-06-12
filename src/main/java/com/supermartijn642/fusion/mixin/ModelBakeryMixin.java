package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.MixinReEntrancePreventer;
import com.supermartijn642.fusion.api.util.UserErrorException;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierReloadListener;
import com.supermartijn642.fusion.model.modifiers.item.ItemModelModifierReloadListener;
import com.supermartijn642.fusion.util.LoggingHelper;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.ISprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.model.IModelState;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.function.Function;

/**
 * Created 22/09/2024 by SuperMartijn642
 */
@Mixin(ModelBakery.class)
public class ModelBakeryMixin {

    @Shadow
    private AtlasTexture blockAtlas;
    @Final
    @Shadow
    private Map<Triple<ResourceLocation,IModelState,Boolean>,IBakedModel> bakedCache;

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
        method = "getBakedModel(Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/client/renderer/texture/ISprite;Ljava/util/function/Function;Lnet/minecraft/client/renderer/vertex/VertexFormat;)Lnet/minecraft/client/renderer/model/IBakedModel;",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/client/renderer/model/ModelBakery;getModel(Lnet/minecraft/util/ResourceLocation;)Lnet/minecraft/client/renderer/model/IUnbakedModel;",
            shift = At.Shift.AFTER
        ),
        cancellable = true
    )
    private void bake(ResourceLocation location, ISprite modelState, Function<ResourceLocation,TextureAtlasSprite> spriteGetter, VertexFormat vertexFormat, CallbackInfoReturnable<IBakedModel> ci){
        //noinspection DataFlowIssue
        ModelBakery modelBakery = (ModelBakery)(Object)this;
        IUnbakedModel unbakedModel = modelBakery.unbakedCache.get(location);
        if(unbakedModel == null)
            return;
        MixinReEntrancePreventer.modelBakeryMixin$getBakedModel(location, modelState, vertexFormat, ci, modelBakery, unbakedModel, this.blockAtlas, this.bakedCache);
    }

    @Inject(
        method = "loadBlockModel",
        at = @At("HEAD")
    )
    private void storeBlockModelName(ResourceLocation identifier, CallbackInfoReturnable<?> ci){
        MixinReEntrancePreventer.fusionBlockModel$CURRENT_MODEL().set(identifier);
    }

    @Inject(
        method = "loadBlockModel",
        at = @At("RETURN")
    )
    private void clearBlockModelName(ResourceLocation identifier, CallbackInfoReturnable<?> ci){
        MixinReEntrancePreventer.fusionBlockModel$CURRENT_MODEL().remove();
    }

    @Redirect(
        method = "lambda$uploadTextures$8(Lnet/minecraft/util/ResourceLocation;)V",
        at = @At(
            value = "INVOKE",
            target = "Lorg/apache/logging/log4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"
        )
    )
    private void interceptFusionErrors(Logger logger, String message, Object identifier, Object exception){
        // Report Fusion model user errors in a more readable way
        Exception e = (Exception)exception;
        if(e instanceof UserErrorException){
            LoggingHelper.logUserError(e.getCause(), "Failed to load model '%s':", identifier);
            return;
        }
        logger.warn(message, identifier, exception);
    }
}
