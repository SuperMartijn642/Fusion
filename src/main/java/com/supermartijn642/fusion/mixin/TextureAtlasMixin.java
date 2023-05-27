package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.extensions.TextureAtlasSpriteExtension;
import com.supermartijn642.fusion.texture.FusionMetadataSection;
import com.supermartijn642.fusion.texture.SpriteCreationContextImpl;
import com.supermartijn642.fusion.texture.SpritePreparationContextImpl;
import com.supermartijn642.fusion.texture.TextureTypeRegistryImpl;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.PngSizeInfo;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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

    @Inject(
        method = "lambda$getBasicSpriteInfos$2(Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/resources/IResourceManager;Ljava/util/concurrent/ConcurrentLinkedQueue;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;<init>(Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/client/renderer/texture/PngSizeInfo;Lnet/minecraft/client/resources/data/AnimationMetadataSection;)V",
            shift = At.Shift.BY,
            by = 2
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void gatherMetadata(ResourceLocation identifier, IResourceManager resourceManager, ConcurrentLinkedQueue<?> queue, CallbackInfo ci, ResourceLocation location, TextureAtlasSprite sprite, IResource resource, Object object, PngSizeInfo pngSizeInfo){
        // Get the fusion metadata
        Pair<TextureType<Object>,Object> metadata = resource.getMetadata(FusionMetadataSection.INSTANCE);
        if(metadata != null){
            this.fusionTextureMetadata.put(sprite.getName(), metadata);
            // Adjust the frame size
            Pair<Integer,Integer> newSize;
            try{
                newSize = metadata.left().getFrameSize(new SpritePreparationContextImpl(sprite.getWidth(), sprite.getHeight(), pngSizeInfo.width, pngSizeInfo.height, identifier), metadata.right());
            }catch(Exception e){
                throw new RuntimeException("Encountered an exception whilst getting frame size from texture type '" + TextureTypeRegistryImpl.getIdentifier(metadata.left()) + "' for texture '" + location + "'!", e);
            }
            if(newSize == null)
                throw new RuntimeException("Received null frame size from texture type '" + TextureTypeRegistryImpl.getIdentifier(metadata.left()) + "' for texture '" + location + "'!");
            // Replace the current size
            sprite.width = newSize.left();
            sprite.height = newSize.right();
        }
    }

    @Inject(
        method = "getLoadedSprites(Lnet/minecraft/resources/IResourceManager;Lnet/minecraft/client/renderer/texture/Stitcher;)Ljava/util/List;",
        at = @At("RETURN"),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void getLoadedSprites(IResourceManager resourceManager, Stitcher stitcher, CallbackInfoReturnable<List<TextureAtlasSprite>> ci){
        // Replace sprites
        List<TextureAtlasSprite> textures = ci.getReturnValue();
        if(textures != null){
            for(int index = 0; index < textures.size(); index++){
                TextureAtlasSprite texture = textures.get(index);
                Pair<TextureType<Object>,Object> textureData = this.fusionTextureMetadata.get(texture.getName());
                if(textureData != null){
                    // Create the sprite
                    TextureAtlasSprite newTexture;
                    //noinspection DataFlowIssue
                    try(SpriteCreationContextImpl context = new SpriteCreationContextImpl(texture, (AtlasTexture)(Object)this)){
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
