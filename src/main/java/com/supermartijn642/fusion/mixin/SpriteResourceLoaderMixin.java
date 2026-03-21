package com.supermartijn642.fusion.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import com.supermartijn642.fusion.texture.TextureCreationHandler;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.neoforge.client.textures.SpriteContentsConstructor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Created 08/10/2024 by SuperMartijn642
 */
@Mixin(SpriteResourceLoader.class)
public interface SpriteResourceLoaderMixin {

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Inject(
        method = "lambda$create$0(Ljava/util/Set;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/server/packs/resources/Resource;Lnet/neoforged/neoforge/client/textures/SpriteContentsConstructor;)Lnet/minecraft/client/renderer/texture/SpriteContents;",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Optional;isPresent()Z",
            shift = At.Shift.BEFORE
        ),
        cancellable = true,
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void handleFusionTextures(Set<?> metadataSerializers, ResourceLocation identifier, Resource resource, SpriteContentsConstructor spriteContentsConstructor, CallbackInfoReturnable<SpriteContents> ci, Optional<AnimationMetadataSection> animationMetadata, List<MetadataSectionType.WithValue<?>> resourceMetadata, NativeImage image){
        TextureCreationHandler.Result<SpriteContents> result = TextureCreationHandler.onLoadTexture(identifier, image, animationMetadata, resourceMetadata);
        if(result != null)
            ci.setReturnValue(result.value());
    }
}
