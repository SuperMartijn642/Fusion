package com.supermartijn642.fusion.texture;

import com.google.gson.JsonParseException;
import com.mojang.blaze3d.platform.NativeImage;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.texture.RawTextureInstance;
import com.supermartijn642.fusion.api.texture.custom.*;
import com.supermartijn642.fusion.api.util.UserErrorException;
import com.supermartijn642.fusion.extensions.TextureAtlasSpriteExtension;
import com.supermartijn642.fusion.texture.custom.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Created 28/03/2026 by SuperMartijn642
 */
public class TextureCreationHandler {

    /*
     * One texture can result in multiple atlas sprites.
     * As such, we take the following steps:
     * 1. When the texture is loaded, we output a dummy sprite info instance for each of the sprite entries.
     * 2. When a sprite is created, we store its allocated region in the dummy sprite info
     * 3. Once all dummy sprites have their region, we actually create our custom sprites
     */

    public static boolean onLoadTexture(ResourceLocation identifier, Resource resource, Queue<TextureAtlasSprite.Info> queue){
        // Get the resource's metadata
        ResourceMetadata resourceMetadata;
        try{
            resourceMetadata = resource.metadata();
        }catch(IOException ignore){
            // If this throws an error, ignore it and let vanilla handle the texture
            return false;
        }

        // Get the fusion metadata
        RawTextureInstance<Object,Object> rawTexture;
        try{
            //noinspection unchecked,rawtypes
            rawTexture = (RawTextureInstance)resourceMetadata.getSection(FusionTextureMetadataSection.INSTANCE).orElse(null);
        }catch(JsonParseException e){
            FusionClient.LOGGER.error("Error parsing Fusion metadata for texture '{}': {}", identifier, e.getMessage());
            return true;
        }catch(RuntimeException e){
            if(e.getCause() instanceof JsonParseException)
                FusionClient.LOGGER.error("Error parsing Fusion metadata for texture '{}': {}: {}", identifier, e.getMessage(), e.getCause().getMessage());
            else
                FusionClient.LOGGER.error("Encountered an exception parsing Fusion metadata for texture '{}'!", identifier, e);
            return true;
        }catch(Exception e){
            FusionClient.LOGGER.error("Encountered an exception parsing Fusion metadata for texture '{}'!", identifier, e);
            return true;
        }
        if(rawTexture == null)
            return false;

        // Get vanilla animation metadata
        AnimationMetadataSection animationMetadata;
        try{
            animationMetadata = resourceMetadata.getSection(AnimationMetadataSection.SERIALIZER).orElse(null);
        }catch(Exception e){
            FusionClient.LOGGER.error("Unable to parse animation metadata for texture '{}':", identifier, e);
            return true;
        }

        // Read image
        NativeImage image;
        try(InputStream stream = resource.open()){
            image = NativeImage.read(stream);
        }catch(Exception e){
            FusionClient.LOGGER.error("Encountered an exception reading image for texture '{}'!", identifier, e);
            return true;
        }

        // Create texture
        TextureOutputImpl<Object> output = new TextureOutputImpl<>(identifier, rawTexture.getTextureType());
        try(TextureCreationContextImpl context = new TextureCreationContextImpl(identifier, image, animationMetadata)){
            rawTexture.createTexture(output, context);
            output.finish();
        }catch(UserErrorException e){
            FusionClient.LOGGER.error("Error for texture '{}': {}", identifier, e.getMessage());
            return true;
        }catch(Exception e){
            FusionClient.LOGGER.error("Encountered an exception whilst creating texture for type '{}' for texture '{}'!", TextureTypeRegistryImpl.getIdentifier(rawTexture.getTextureType()), identifier, e);
            return true;
        }

        // Create dummy sprite contents for each sub-sprite
        DummyTextureSpriteContents parent = new DummyTextureSpriteContents(output);
        queue.addAll(parent.createChildren());
        return true;
    }

    public static Result<CompletableFuture<Void>> onLoadSprite(TextureAtlasSprite.Info spriteInfo, int spriteX, int spriteY, TextureAtlas textureAtlas, int atlasWidth, int atlasHeight, int mipmapLevels, Queue<TextureAtlasSprite> queue){
        // Check if the sprite is from Fusion
        if(!(spriteInfo instanceof DummyTextureSpriteContents.Child child))
            return null;

        // Store the allocated area
        child.setAllocation(new AllocatedSpriteImpl(
            spriteInfo.name(),
            spriteX, spriteY,
            spriteInfo.width, spriteInfo.height,
            (float)spriteX / atlasWidth, (float)(spriteX + spriteInfo.width) / atlasWidth,
            (float)spriteY / atlasHeight, (float)(spriteY + spriteInfo.height) / atlasHeight
        ));

        // If all child sprites have been allocated, create the texture
        DummyTextureSpriteContents topTexture = child.parent().getTopTexture();
        if(topTexture.hasAllAllocations())
            return new Result<>(CompletableFuture.runAsync(() -> createTexture(topTexture, textureAtlas, atlasWidth, atlasHeight, mipmapLevels, queue)));
        return new Result<>(null);
    }

    private static void createTexture(DummyTextureSpriteContents contents, TextureAtlas textureAtlas, int atlasWidth, int atlasHeight, int mipmapLevels, Queue<TextureAtlasSprite> queue){
        List<TextureAtlasSprite> customSprites = new ArrayList<>();
        try{
            createTextureInstance(contents, textureAtlas, atlasWidth, atlasHeight, mipmapLevels, customSprites::add);
        }catch(Exception e){
            FusionClient.LOGGER.error("Error while creating texture '{}': {}", contents.getTextureOutput().getIdentifier(), e.getMessage());
            return;
        }
        queue.addAll(customSprites);
    }

    private static TextureInstance<?> createTextureInstance(DummyTextureSpriteContents contents, TextureAtlas textureAtlas, int atlasWidth, int atlasHeight, int mipmapLevels, Consumer<TextureAtlasSprite> spriteOutput){
        // Create sub-textures
        TextureInstance<?> defaultSubTexture = null;
        for(DummyTextureSpriteContents subTexture : contents.getSubTextures()){
            try{
                TextureInstance<?> textureInstance = createTextureInstance(subTexture, textureAtlas, atlasWidth, atlasHeight, mipmapLevels, spriteOutput);
                if(subTexture.getTextureOutput().isMarkedDefault())
                    defaultSubTexture = textureInstance;
            }catch(Exception e){
                throw new RuntimeException("Failed to create sub-texture of type '" + TextureTypeRegistryImpl.getIdentifier(subTexture.getTextureOutput().getTextureType()) + "'!", e);
            }
        }

        // Create texture instance
        TextureOutputImpl<?> textureOutput = contents.getTextureOutput();
        //noinspection unchecked,rawtypes
        TextureInstanceImpl<?> textureInstance = new TextureInstanceImpl(
            textureOutput.getTextureType(),
            textureOutput.getIdentifier(),
            textureOutput.getCustomData()
        );

        // Create the custom sprites
        SpriteInstance defaultSprite = null;
        List<SpriteInstance> sprites = new ArrayList<>(contents.children().size());
        SpriteConstructionContext context = null;
        for(DummyTextureSpriteContents.Child child : contents.children()){
            ResourceLocation identifier = child.spriteBuilder().getIdentifier();
            SpriteBuilderImpl spriteBuilder = child.spriteBuilder();
            // Create custom sprite
            SpriteBuilder.Constructor constructor = spriteBuilder.getConstructor();
            TextureAtlasSprite newSprite;
            if(constructor == null){
                newSprite = new FusionTextureAtlasSprite(
                    child.allocation(),
                    textureAtlas,
                    (SpriteImageSourceImpl)spriteBuilder.getImageSource(),
                    mipmapLevels
                ) {};
            }else{
                AllocatedSprite allocation = child.allocation();
                if(context == null){
                    context = new SpriteConstructionContextImpl(
                        atlasWidth, atlasHeight,
                        textureAtlas,
                        mipmapLevels
                    );
                }
                try{
                    newSprite = constructor.create(allocation, context);
                }catch(Exception e){
                    throw new RuntimeException("Encountered an exception whilst creating sprite '" + identifier + "' for texture type '" + TextureTypeRegistryImpl.getIdentifier(textureOutput.getTextureType()) + "'!", e);
                }
                if(!identifier.equals(newSprite.getName()))
                    throw new RuntimeException("Sprite constructor for sprite '" + identifier + "' from texture type '" + TextureTypeRegistryImpl.getIdentifier(textureOutput.getTextureType()) + "' returned sprite with incorrect identifier '" + newSprite.getName() + "'!");
            }
            // Create sprite instance
            SpriteInstance spriteInstance = new SpriteInstanceImpl(textureInstance, newSprite, identifier);
            //noinspection DataFlowIssue
            ((TextureAtlasSpriteExtension)newSprite).setFusionSpriteInstance(spriteInstance);
            sprites.add(spriteInstance);
            if(spriteBuilder.isMarkedDefault())
                defaultSprite = spriteInstance;
        }

        // Set sprite instance references
        if(defaultSprite == null){
            defaultSprite = defaultSubTexture.getDefaultSprite();
            ((TextureAtlasSpriteExtension)defaultSprite.getSprite()).setFusionSpriteInstance(new SpriteInstanceImpl(textureInstance, defaultSprite.getSprite(), defaultSprite.getIdentifier()));
        }
        textureInstance.setSprites(sprites, defaultSprite);

        // Call callbacks
        for(int i = 0; i < sprites.size(); i++){
            SpriteInstance spriteInstance = sprites.get(i);
            Consumer<SpriteInstance> callback = contents.children().get(i).spriteBuilder().getCallback();
            if(callback == null)
                continue;
            try{
                callback.accept(spriteInstance);
            }catch(Exception e){
                throw new RuntimeException("Encountered an exception whilst calling sprite creation callback for sprite '" + spriteInstance.getIdentifier() + "' from texture type '" + TextureTypeRegistryImpl.getIdentifier(textureInstance.getTextureType()) + "'!", e);
            }
        }
        if(textureOutput.getCreationCallback() != null){
            try{
                //noinspection unchecked,rawtypes
                ((TextureOutputImpl)textureOutput).getCreationCallback().accept(textureInstance);
            }catch(Exception e){
                throw new RuntimeException("Encountered an exception whilst calling texture creation callback for texture '" + textureInstance.getIdentifier() + "' from texture type '" + TextureTypeRegistryImpl.getIdentifier(textureInstance.getTextureType()) + "'!", e);
            }
        }
        if(textureOutput.getSubTextureCallback() != null){
            try{
                //noinspection unchecked,rawtypes
                ((TextureOutputImpl)textureOutput).getSubTextureCallback().accept(textureInstance);
            }catch(Exception e){
                throw new RuntimeException("Encountered an exception whilst calling sub-texture creation callback for sub-texture '" + textureInstance.getIdentifier() + "' of texture type '" + TextureTypeRegistryImpl.getIdentifier(textureInstance.getTextureType()) + "'!", e);
            }
        }

        // Replace the current sprites
        for(SpriteInstance spriteInstance : sprites)
            spriteOutput.accept(spriteInstance.getSprite());
        return textureInstance;
    }

    public record Result<T>(T value) {
    }
}
