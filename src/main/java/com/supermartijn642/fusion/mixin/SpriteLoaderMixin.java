package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.extensions.SpriteContentsExtension;
import com.supermartijn642.fusion.extensions.TextureAtlasSpriteExtension;
import com.supermartijn642.fusion.texture.SpriteCreationContextImpl;
import com.supermartijn642.fusion.texture.TextureTypeRegistryImpl;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Created 21/05/2023 by SuperMartijn642
 */
@Mixin(value = SpriteLoader.class, priority = 900)
public class SpriteLoaderMixin {

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
                Pair<TextureType<Object>,Object> textureData = ((SpriteContentsExtension)texture.contents()).fusionTextureMetadata();
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
                    ((SpriteContentsExtension)texture.contents()).clearFusionTextureMetadata();
                }
            }
            return preparations;
        });
    }
}
