package com.supermartijn642.fusion.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.texture.TextureErrorException;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.texture.FusionTextureMetadataSection;
import com.supermartijn642.fusion.texture.SpritePreparationContextImpl;
import com.supermartijn642.fusion.texture.TextureTypeRegistryImpl;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
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
        method = "lambda$create$0(Ljava/util/Set;Lnet/minecraft/resources/Identifier;Lnet/minecraft/server/packs/resources/Resource;Lnet/neoforged/neoforge/client/textures/SpriteContentsConstructor;)Lnet/minecraft/client/renderer/texture/SpriteContents;",
        at = {
            @At(
                value = "INVOKE_ASSIGN",
                target = "Lnet/minecraft/client/resources/metadata/animation/AnimationMetadataSection;calculateFrameSize(II)Lnet/minecraft/client/resources/metadata/animation/FrameSize;",
                shift = At.Shift.AFTER
            ),
            @At(
                value = "INVOKE",
                target = "Lnet/neoforged/neoforge/client/textures/SpriteContentsConstructor;create(Lnet/minecraft/resources/Identifier;Lnet/minecraft/client/resources/metadata/animation/FrameSize;Lcom/mojang/blaze3d/platform/NativeImage;Ljava/util/Optional;Ljava/util/List;Ljava/util/Optional;)Lnet/minecraft/client/renderer/texture/SpriteContents;",
                shift = At.Shift.BEFORE
            )
        },
        require = 2,
        cancellable = true,
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void modifyFrameSize(Set<?> metadataSerializers, Identifier identifier, Resource resource, SpriteContentsConstructor constructor, CallbackInfoReturnable<SpriteContents> ci, Optional<AnimationMetadataSection> animationMetadata, Optional<TextureMetadataSection> textureMetadata, List<MetadataSectionType.WithValue<?>> resourceMetadata, NativeImage image, FrameSize originalSize){
        // Get the fusion metadata
        Pair<TextureType<Object>,Object> metadata = null;
        for(MetadataSectionType.WithValue<?> entry : resourceMetadata){
            if(entry.type() == FusionTextureMetadataSection.TYPE){
                //noinspection unchecked
                metadata = (Pair<TextureType<Object>,Object>)entry.value();
                break;
            }
        }
        if(metadata != null){
            // Adjust the frame size
            Pair<Integer,Integer> newSize;
            try{
                newSize = metadata.left().getFrameSize(new SpritePreparationContextImpl(originalSize.width(), originalSize.height(), image.getWidth(), image.getHeight(), identifier, animationMetadata.orElse(null)), metadata.right());
            }catch(TextureErrorException e){
                FusionClient.LOGGER.error("Error for texture '{}': {}", identifier, e.getMessage());
                image.close();
                ci.setReturnValue(null);
                return;
            }catch(Exception e){
                throw new RuntimeException("Encountered an exception whilst getting frame size from texture type '" + TextureTypeRegistryImpl.getIdentifier(metadata.left()) + "' for texture '" + identifier + "'!", e);
            }
            if(newSize == null)
                throw new RuntimeException("Received null frame size from texture type '" + TextureTypeRegistryImpl.getIdentifier(metadata.left()) + "' for texture '" + identifier + "'!");
            // Create the sprite contents
            ci.setReturnValue(constructor.create(identifier, new FrameSize(newSize.left(), newSize.right()), image, animationMetadata, resourceMetadata, textureMetadata));
        }
    }
}
