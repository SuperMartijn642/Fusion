package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.texture.TextureErrorException;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.extensions.TextureAtlasSpriteExtension;
import com.supermartijn642.fusion.texture.FusionTextureMetadataSection;
import com.supermartijn642.fusion.texture.SpriteCreationContextImpl;
import com.supermartijn642.fusion.texture.SpritePreparationContextImpl;
import com.supermartijn642.fusion.texture.TextureTypeRegistryImpl;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.PngSizeInfo;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
@Mixin(value = AtlasTexture.class, priority = 900)
public class TextureAtlasMixin {

    @Unique
    private final Map<ResourceLocation,Pair<TextureType<Object>,Object>> fusionTextureMetadata = new HashMap<>();
    @Unique
    private final ThreadLocal<PngSizeInfo> pngSizeInfo = new ThreadLocal<>();
    @Unique
    private final ThreadLocal<AnimationMetadataSection> animationMetadata = new ThreadLocal<>();

    @ModifyVariable(
        method = {
            "lambda$makeSprites$2(Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/resources/IResourceManager;Ljava/util/concurrent/ConcurrentLinkedQueue;)V",
            "lambda$getBasicSpriteInfos$2(Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/resources/IResourceManager;Ljava/util/concurrent/ConcurrentLinkedQueue;)V"
        },
        at = @At(
            value = "STORE",
            ordinal = 0
        ),
        remap = false
    )
    private PngSizeInfo storePngSizeInfo(PngSizeInfo info){
        this.pngSizeInfo.set(info);
        return info;
    }

    @ModifyVariable(
        method = {
            "lambda$makeSprites$2(Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/resources/IResourceManager;Ljava/util/concurrent/ConcurrentLinkedQueue;)V",
            "lambda$getBasicSpriteInfos$2(Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/resources/IResourceManager;Ljava/util/concurrent/ConcurrentLinkedQueue;)V"
        },
        at = @At(
            value = "STORE",
            ordinal = 0
        ),
        remap = false
    )
    private AnimationMetadataSection storeAnimationMetadata(AnimationMetadataSection metadata){
        this.animationMetadata.set(metadata);
        return metadata;
    }

    @Inject(
        method = {
            "lambda$makeSprites$2(Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/resources/IResourceManager;Ljava/util/concurrent/ConcurrentLinkedQueue;)V",
            "lambda$getBasicSpriteInfos$2(Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/resources/IResourceManager;Ljava/util/concurrent/ConcurrentLinkedQueue;)V"
        },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resources/data/AnimationMetadataSection;getFrameSize(II)Lcom/mojang/datafixers/util/Pair;",
            shift = At.Shift.BEFORE
        ),
        cancellable = true,
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void gatherMetadata(ResourceLocation identifier, IResourceManager resourceManager, ConcurrentLinkedQueue<TextureAtlasSprite.Info> queue, CallbackInfo ci, ResourceLocation location, IResource resource){
        // For some reason the LVT contains a 'Lnull;' according to Mixin, hence the weird capturing for the png size and animation metadata
        // Get the fusion metadata
        Pair<TextureType<Object>,Object> metadata = resource.getMetadata(FusionTextureMetadataSection.INSTANCE);
        if(metadata != null){
            synchronized(this.fusionTextureMetadata){
                this.fusionTextureMetadata.put(identifier, metadata);
            }
            PngSizeInfo pngInfo = this.pngSizeInfo.get();
            AnimationMetadataSection animationMetadata = this.animationMetadata.get();
            if(animationMetadata == null)
                animationMetadata = AnimationMetadataSection.EMPTY;
            // Get the original frame size
            com.mojang.datafixers.util.Pair<Integer,Integer> originalSize = animationMetadata.calculateFrameSize(pngInfo.width, pngInfo.height);
            // Adjust the frame size
            Pair<Integer,Integer> newSize;
            try{
                newSize = metadata.left().getFrameSize(new SpritePreparationContextImpl(originalSize.getFirst(), originalSize.getSecond(), pngInfo.width, pngInfo.height, identifier, animationMetadata), metadata.right());
            }catch(TextureErrorException e){
                FusionClient.LOGGER.error("Error for texture '{}': {}", identifier, e.getMessage());
                ci.cancel();
                return;
            }catch(Exception e){
                throw new RuntimeException("Encountered an exception whilst getting frame size from texture type '" + TextureTypeRegistryImpl.getIdentifier(metadata.left()) + "' for texture '" + location + "'!", e);
            }
            if(newSize == null)
                throw new RuntimeException("Received null frame size from texture type '" + TextureTypeRegistryImpl.getIdentifier(metadata.left()) + "' for texture '" + location + "'!");
            // Replace the current size
            queue.add(new TextureAtlasSprite.Info(identifier, newSize.left(), newSize.right(), animationMetadata));
            ci.cancel();
        }
    }

    @Inject(
        method = "getLoadedSprites(Lnet/minecraft/resources/IResourceManager;Lnet/minecraft/client/renderer/texture/Stitcher;I)Ljava/util/List;",
        at = @At("RETURN"),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void getLoadedSprites(IResourceManager resourceManager, Stitcher stitcher, int i, CallbackInfoReturnable<List<TextureAtlasSprite>> ci){
        // Replace sprites
        List<TextureAtlasSprite> textures = ci.getReturnValue();
        if(textures != null){
            for(int index = 0; index < textures.size(); index++){
                TextureAtlasSprite texture = textures.get(index);
                Pair<TextureType<Object>,Object> textureData = this.fusionTextureMetadata.get(texture.getName());
                if(textureData != null){
                    // Create the sprite
                    TextureAtlasSprite newTexture;
                    try(SpriteCreationContextImpl context = new SpriteCreationContextImpl(texture)){
                        newTexture = textureData.left().createSprite(context, textureData.right());
                    }catch(Exception e){
                        throw new RuntimeException("Encountered an exception whilst initialising texture '" + texture.getName() + "' for texture type '" + TextureTypeRegistryImpl.getIdentifier(textureData.left()) + "'!", e);
                    }
                    if(newTexture == null)
                        throw new RuntimeException("Received null texture from texture type '" + TextureTypeRegistryImpl.getIdentifier(textureData.left()) + "' for texture '" + texture.getName() + "'!");
                    ((TextureAtlasSpriteExtension)newTexture).setFusionTextureType(textureData.left());
                    // Replace the current texture
                    textures.set(index, newTexture);
                }
            }
        }
        this.fusionTextureMetadata.clear();
    }
}
