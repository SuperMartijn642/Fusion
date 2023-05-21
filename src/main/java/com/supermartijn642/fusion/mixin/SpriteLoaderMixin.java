package com.supermartijn642.fusion.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.extensions.TextureAtlasSpriteExtension;
import com.supermartijn642.fusion.texture.FusionMetadataSection;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Created 21/05/2023 by SuperMartijn642
 */
@Mixin(value = SpriteLoader.class, priority = 900)
public class SpriteLoaderMixin {

    @Unique
    private static final Map<ResourceLocation,Pair<TextureType<Object>,Object>> fusionTextureMetadata = new ConcurrentHashMap<>(); // TODO find a place to clear this
    @Unique
    private final static ThreadLocal<Boolean> modifyFrameSize = ThreadLocal.withInitial(() -> false);
    @Unique
    private final static ThreadLocal<Integer> frameWidth = new ThreadLocal<>(), frameHeight = new ThreadLocal<>();

    @Inject(
        method = "loadSprite",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resources/metadata/animation/AnimationMetadataSection;calculateFrameSize(II)Lnet/minecraft/client/resources/metadata/animation/FrameSize;",
            shift = At.Shift.BEFORE
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void gatherMetadata(ResourceLocation identifier, Resource resource, CallbackInfoReturnable<SpriteContents> ci, AnimationMetadataSection animationMetadataSection, NativeImage image){
        // Get the fusion metadata
        Pair<TextureType<Object>,Object> metadata = null;
        try{
            metadata = resource.metadata().getSection(FusionMetadataSection.INSTANCE).orElse(null);
        }catch(IOException ignored){ /* Metadata will always be cached already, so need to worry about exceptions */ }
        if(metadata != null){
            fusionTextureMetadata.put(identifier, metadata);
            // Adjust the frame size
            FrameSize originalSize = animationMetadataSection.calculateFrameSize(image.getWidth(), image.getHeight());
            Pair<Integer,Integer> newSize;
            try{
                newSize = metadata.left().getFrameSize(new SpritePreparationContextImpl(originalSize.width(), originalSize.height(), image.getWidth(), image.getHeight(), identifier), metadata.right());
            }catch(Exception e){
                throw new RuntimeException("Encountered an exception whilst getting frame size from texture type '" + TextureTypeRegistryImpl.getIdentifier(metadata.left()) + "' for texture '" + identifier + "'!", e);
            }
            if(newSize == null)
                throw new RuntimeException("Received null frame size from texture type '" + TextureTypeRegistryImpl.getIdentifier(metadata.left()) + "' for texture '" + identifier + "'!");
            // Replace the current size
            modifyFrameSize.set(true);
            frameWidth.set(newSize.left());
            frameHeight.set(newSize.right());
        }
    }

    @ModifyVariable(
        method = "loadSprite",
        at = @At("STORE"),
        ordinal = 0
    )
    private static FrameSize adjustFrameSize(FrameSize original){
        if(modifyFrameSize.get()){
            modifyFrameSize.set(false);
            return new FrameSize(frameWidth.get(), frameHeight.get());
        }
        return original;
    }

    @Inject(
        method = "loadAndStitch",
        at = @At("RETURN")
    )
    private void initializeTextures(ResourceManager resourceManager, ResourceLocation atlas, int i, Executor executor, CallbackInfoReturnable<CompletableFuture<SpriteLoader.Preparations>> ci){
        ci.getReturnValue().thenApply(preparations -> {
            // Replace sprites
            Map<ResourceLocation,TextureAtlasSprite> textures = preparations.regions();
            for(Map.Entry<ResourceLocation,Pair<TextureType<Object>,Object>> entry : fusionTextureMetadata.entrySet()){
                ResourceLocation identifier = entry.getKey();
                TextureAtlasSprite texture = textures.get(identifier);
                Pair<TextureType<Object>,Object> textureData = entry.getValue();
                if(texture != null){
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
