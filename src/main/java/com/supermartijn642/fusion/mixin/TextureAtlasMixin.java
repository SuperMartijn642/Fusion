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
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    @Redirect(
        method = {
            "lambda$makeSprites$2(Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/resources/IResourceManager;Ljava/util/concurrent/ConcurrentLinkedQueue;)V",
            "lambda$getBasicSpriteInfos$2(Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/resources/IResourceManager;Ljava/util/concurrent/ConcurrentLinkedQueue;)V"
        },
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/texture/PngSizeInfo;width:I",
            opcode = Opcodes.GETFIELD
        )
    )
    private int storePngSizeInfo(PngSizeInfo info){
        this.pngSizeInfo.set(info);
        return info.width;
    }

    @Inject(
        method = {
            "lambda$makeSprites$2(Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/resources/IResourceManager;Ljava/util/concurrent/ConcurrentLinkedQueue;)V",
            "lambda$getBasicSpriteInfos$2(Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/resources/IResourceManager;Ljava/util/concurrent/ConcurrentLinkedQueue;)V"
        },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite$Info;<init>(Lnet/minecraft/util/ResourceLocation;IILnet/minecraft/client/resources/data/AnimationMetadataSection;)V",
            shift = At.Shift.BY,
            by = 2
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void gatherMetadata(ResourceLocation identifier, IResourceManager resourceManager, ConcurrentLinkedQueue<?> queue, CallbackInfo ci, ResourceLocation location, TextureAtlasSprite.Info info, IResource resource){
        // Get the fusion metadata
        Pair<TextureType<Object>,Object> metadata = resource.getMetadata(FusionMetadataSection.INSTANCE);
        if(metadata != null){
            synchronized(this.fusionTextureMetadata){
                this.fusionTextureMetadata.put(info.name(), metadata);
            }
            // Adjust the frame size
            Pair<Integer,Integer> newSize;
            try{
                newSize = metadata.left().getFrameSize(new SpritePreparationContextImpl(info.width(), info.height(), this.pngSizeInfo.get().width, this.pngSizeInfo.get().height, identifier), metadata.right());
            }catch(Exception e){
                throw new RuntimeException("Encountered an exception whilst getting frame size from texture type '" + TextureTypeRegistryImpl.getIdentifier(metadata.left()) + "' for texture '" + location + "'!", e);
            }
            if(newSize == null)
                throw new RuntimeException("Received null frame size from texture type '" + TextureTypeRegistryImpl.getIdentifier(metadata.left()) + "' for texture '" + location + "'!");
            // Replace the current size
            info.width = newSize.left();
            info.height = newSize.right();
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
