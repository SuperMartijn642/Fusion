package com.supermartijn642.fusion.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.texture.TextureErrorException;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.extensions.SpriteContentsExtension;
import com.supermartijn642.fusion.extensions.TextureAtlasSpriteExtension;
import com.supermartijn642.fusion.texture.FusionTextureMetadataSection;
import com.supermartijn642.fusion.texture.SpriteCreationContextImpl;
import com.supermartijn642.fusion.texture.SpritePreparationContextImpl;
import com.supermartijn642.fusion.texture.TextureTypeRegistryImpl;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Created 21/05/2023 by SuperMartijn642
 */
@Mixin(value = SpriteLoader.class, priority = 900)
public class SpriteLoaderMixin {

    @Inject(
        method = "loadSprite",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/client/resources/metadata/animation/AnimationMetadataSection;calculateFrameSize(II)Lnet/minecraft/client/resources/metadata/animation/FrameSize;",
            shift = At.Shift.AFTER
        ),
        cancellable = true,
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void gatherMetadata(ResourceLocation identifier, Resource resource, CallbackInfoReturnable<SpriteContents> ci, AnimationMetadataSection animationMetadata, NativeImage image, FrameSize originalSize){
        // Get the fusion metadata
        Pair<TextureType<Object>,Object> metadata = null;
        try{
            metadata = resource.metadata().getSection(FusionTextureMetadataSection.INSTANCE).orElse(null);
        }catch(IOException ignored){ /* Metadata will always be cached already, so need to worry about exceptions */ }
        if(metadata != null){
            // Adjust the frame size
            Pair<Integer,Integer> newSize;
            try{
                newSize = metadata.left().getFrameSize(new SpritePreparationContextImpl(originalSize.width(), originalSize.height(), image.getWidth(), image.getHeight(), identifier, animationMetadata), metadata.right());
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
            SpriteContents contents = new SpriteContents(identifier, new FrameSize(newSize.left(), newSize.right()), image, animationMetadata);
            //noinspection DataFlowIssue
            ((SpriteContentsExtension)contents).setFusionMetadata(metadata);
            ci.setReturnValue(contents);
        }
    }

    @Inject(
        method = "loadAndStitch",
        at = @At("RETURN")
    )
    private void initializeTextures(ResourceManager resourceManager, ResourceLocation atlas, int i, Executor executor, CallbackInfoReturnable<CompletableFuture<SpriteLoader.Preparations>> ci){
        ci.getReturnValue().thenApply(preparations -> {
            // Replace sprites
            Map<ResourceLocation,TextureAtlasSprite> textures = preparations.regions();
            for(Map.Entry<ResourceLocation,TextureAtlasSprite> entry : textures.entrySet()){
                ResourceLocation identifier = entry.getKey();
                TextureAtlasSprite texture = entry.getValue();
                //noinspection resource
                Pair<TextureType<Object>,Object> textureData = ((SpriteContentsExtension)texture.contents()).getFusionMetadata();
                if(textureData != null){
                    // Create the sprite
                    TextureAtlasSprite newTexture;
                    try(SpriteCreationContextImpl context = new SpriteCreationContextImpl(preparations, atlas, texture)){
                        newTexture = textureData.left().createSprite(context, textureData.right());
                    }catch(Exception e){
                        throw new RuntimeException("Encountered an exception whilst initialising texture '" + identifier + "' for texture type '" + TextureTypeRegistryImpl.getIdentifier(textureData.left()) + "'!", e);
                    }
                    if(newTexture == null)
                        throw new RuntimeException("Received null texture from texture type '" + TextureTypeRegistryImpl.getIdentifier(textureData.left()) + "' for texture '" + identifier + "'!");
                    ((TextureAtlasSpriteExtension)newTexture).setFusionTextureType(textureData.left());
                    // Replace the current texture
                    textures.put(identifier, newTexture);
                }
            }
            return preparations;
        });
    }
}
