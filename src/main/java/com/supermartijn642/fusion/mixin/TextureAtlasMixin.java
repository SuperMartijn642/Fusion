package com.supermartijn642.fusion.mixin;

import com.mojang.blaze3d.platform.PngInfo;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.extensions.TextureAtlasSpriteExtension;
import com.supermartijn642.fusion.texture.FusionMetadataSection;
import com.supermartijn642.fusion.texture.SpriteCreationContextImpl;
import com.supermartijn642.fusion.texture.SpritePreparationContextImpl;
import com.supermartijn642.fusion.texture.TextureTypeRegistryImpl;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
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
import java.util.Queue;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
@Mixin(value = TextureAtlas.class, priority = 900)
public class TextureAtlasMixin {

    @Unique
    private final Map<ResourceLocation,Pair<TextureType<Object>,Object>> fusionTextureMetadata = new HashMap<>();

    @Inject(
        method = "method_18160(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/Queue;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite$Info;<init>(Lnet/minecraft/resources/ResourceLocation;IILnet/minecraft/client/resources/metadata/animation/AnimationMetadataSection;)V",
            shift = At.Shift.BY,
            by = 2
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void gatherMetadata(ResourceLocation identifier, ResourceManager resourceManager, Queue<?> queue, CallbackInfo ci, ResourceLocation location, TextureAtlasSprite.Info info, Resource resource, PngInfo pngInfo){
        // Get the fusion metadata
        Pair<TextureType<Object>,Object> metadata = resource.getMetadata(FusionMetadataSection.INSTANCE);
        if(metadata != null){
            this.fusionTextureMetadata.put(info.name(), metadata);
            // Adjust the frame size
            Pair<Integer,Integer> newSize;
            try{
                newSize = metadata.left().getFrameSize(new SpritePreparationContextImpl(info.width(), info.height(), pngInfo.width, pngInfo.height, identifier), metadata.right());
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
        method = "getLoadedSprites(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/client/renderer/texture/Stitcher;I)Ljava/util/List;",
        at = @At("RETURN"),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void getLoadedSprites(ResourceManager resourceManager, Stitcher stitcher, int i, CallbackInfoReturnable<List<TextureAtlasSprite>> ci){
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
