package com.supermartijn642.fusion.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.platform.NativeImage;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.texture.FusionTextureMetadataSection;
import com.supermartijn642.fusion.texture.SpritePreparationContextImpl;
import com.supermartijn642.fusion.texture.TextureTypeRegistryImpl;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.neoforged.neoforge.client.textures.SpriteContentsConstructor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Collection;

/**
 * Created 08/10/2024 by SuperMartijn642
 */
@Mixin(SpriteResourceLoader.class)
public interface SpriteResourceLoaderMixin {

    @Inject(
        method = "lambda$create$0(Ljava/util/Collection;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/server/packs/resources/Resource;Lnet/neoforged/neoforge/client/textures/SpriteContentsConstructor;)Lnet/minecraft/client/renderer/texture/SpriteContents;",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/client/resources/metadata/animation/AnimationMetadataSection;calculateFrameSize(II)Lnet/minecraft/client/resources/metadata/animation/FrameSize;",
            shift = At.Shift.AFTER
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void modifyFrameSize(Collection<?> metadataSerializers, ResourceLocation identifier, Resource resource, SpriteContentsConstructor constructor, CallbackInfoReturnable<?> ci, ResourceMetadata resourceMetadata, NativeImage image, AnimationMetadataSection animationMetadata, @Local LocalRef<FrameSize> frameSize){
        // Get the fusion metadata
        Pair<TextureType<Object>,Object> metadata = resourceMetadata.getSection(FusionTextureMetadataSection.INSTANCE).orElse(null);
        if(metadata != null){
            FrameSize originalSize = frameSize.get();
            // Adjust the frame size
            Pair<Integer,Integer> newSize;
            try{
                newSize = metadata.left().getFrameSize(new SpritePreparationContextImpl(originalSize.width(), originalSize.height(), image.getWidth(), image.getHeight(), identifier, animationMetadata), metadata.right());
            }catch(Exception e){
                throw new RuntimeException("Encountered an exception whilst getting frame size from texture type '" + TextureTypeRegistryImpl.getIdentifier(metadata.left()) + "' for texture '" + identifier + "'!", e);
            }
            if(newSize == null)
                throw new RuntimeException("Received null frame size from texture type '" + TextureTypeRegistryImpl.getIdentifier(metadata.left()) + "' for texture '" + identifier + "'!");
            // Replace the current size
            frameSize.set(new FrameSize(newSize.left(), newSize.right()));
        }
    }
}
