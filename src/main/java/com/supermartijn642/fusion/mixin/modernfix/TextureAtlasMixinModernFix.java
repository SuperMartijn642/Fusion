package com.supermartijn642.fusion.mixin.modernfix;

import com.google.common.collect.Lists;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.extensions.TextureAtlasSpriteExtension;
import com.supermartijn642.fusion.texture.FusionMetadataSection;
import com.supermartijn642.fusion.texture.SpriteCreationContextImpl;
import com.supermartijn642.fusion.texture.SpritePreparationContextImpl;
import com.supermartijn642.fusion.texture.TextureTypeRegistryImpl;
import net.minecraft.Util;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
@Mixin(value = TextureAtlas.class, priority = 900)
public class TextureAtlasMixinModernFix {

    @Unique
    private final Map<ResourceLocation,Pair<TextureType<Object>,Object>> fusionTextureMetadata = new HashMap<>();

    @Shadow
    private ResourceLocation getResourceLocation(ResourceLocation p_118325_){
        throw new AssertionError();
    }

    @Inject(
        method = "getBasicSpriteInfos(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/Set;)Ljava/util/Collection;",
        at = @At(value = "RETURN")
    )
    private void gatherMetadata(ResourceManager resourceManager, Set<ResourceLocation> sprites, CallbackInfoReturnable<Collection<TextureAtlasSprite.Info>> ci){
        Collection<TextureAtlasSprite.Info> spriteInfos = ci.getReturnValue();
        List<CompletableFuture<?>> tasks = Lists.newArrayList();
        for(TextureAtlasSprite.Info info : spriteInfos){
            tasks.add(CompletableFuture.runAsync(() -> {
                // Load the texture resource
                ResourceLocation location = this.getResourceLocation(info.name());
                Optional<Resource> optional = resourceManager.getResource(location);
                if(optional.isPresent()){
                    Resource resource = optional.get();
                    // Get the fusion metadata
                    Pair<TextureType<Object>,Object> metadata;
                    try{
                        metadata = resource.metadata().getSection(FusionMetadataSection.INSTANCE).orElse(null);
                    }catch(IOException e){
                        throw new RuntimeException("Encountered an exception whilst reading metadata for '" + location + "'!", e);
                    }
                    if(metadata != null){
                        this.fusionTextureMetadata.put(info.name(), metadata);
                        // Adjust the frame size
                        Pair<Integer,Integer> newSize;
                        try{
                            newSize = metadata.left().getFrameSize(new SpritePreparationContextImpl(info.width(), info.height(), info.width(), info.height(), info.name()), metadata.right());
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
            }, Util.backgroundExecutor()));
        }
        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();
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
