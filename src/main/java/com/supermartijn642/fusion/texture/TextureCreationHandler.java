package com.supermartijn642.fusion.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.texture.RawTextureInstance;
import com.supermartijn642.fusion.api.texture.custom.*;
import com.supermartijn642.fusion.api.util.UserErrorException;
import com.supermartijn642.fusion.extensions.TextureAtlasSpriteExtension;
import com.supermartijn642.fusion.texture.custom.*;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionType;

import java.util.*;
import java.util.function.Consumer;

/**
 * Created 28/03/2026 by SuperMartijn642
 */
public class TextureCreationHandler {

    /*
     * One texture can result in multiple atlas sprites.
     * As such, we take the following steps:
     * 1. When the texture is loaded, we output a dummy sprite contents instance that holds the sprite entries and custom texture type data.
     * 2. Right before stitching, we replace the single dummy sprite contents with one sprite contents for each sprite entry.
     * 3. After the sprites have been allocated, we replace them with our custom sprites.
     */

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static Result<SpriteContents> onLoadTexture(ResourceLocation identifier, NativeImage image, Optional<AnimationMetadataSection> animationMetadata, List<MetadataSectionType.WithValue<?>> resourceMetadata){
        // Get the fusion metadata
        RawTextureInstance<Object,Object> rawTexture = null;
        for(MetadataSectionType.WithValue<?> entry : resourceMetadata){
            if(entry.type() == FusionTextureMetadataSection.TYPE){
                //noinspection unchecked
                rawTexture = (RawTextureInstance<Object,Object>)entry.value();
                break;
            }
        }
        if(rawTexture == null)
            return null;

        // Create texture
        TextureOutputImpl<Object> output = new TextureOutputImpl<>(identifier, rawTexture.getTextureType());
        try(TextureCreationContextImpl context = new TextureCreationContextImpl(identifier, image, animationMetadata.orElse(null))){
            rawTexture.createTexture(output, context);
            output.finish();
        }catch(UserErrorException e){
            FusionClient.LOGGER.error("Error for texture '{}': {}", identifier, e.getMessage());
            return Result.empty();
        }catch(Exception e){
            FusionClient.LOGGER.error("Encountered an exception whilst creating texture for type '{}' for texture '{}'!", TextureTypeRegistryImpl.getIdentifier(rawTexture.getTextureType()), identifier, e);
            return Result.empty();
        }

        // Create the sprite contents
        return new Result<>(new DummyTextureSpriteContents(output));
    }

    public static List<SpriteContents> onStitchSprites(List<SpriteContents> sprites){
        // Replace dummy sprite with its sub-sprites
        List<SpriteContents> copy = null;
        for(int i = sprites.size() - 1; i >= 0; i--){
            if(sprites.get(i) instanceof DummyTextureSpriteContents){
                if(copy == null)
                    copy = new ArrayList<>(sprites);
                //noinspection resource
                DummyTextureSpriteContents contents = (DummyTextureSpriteContents)copy.remove(i);
                copy.addAll(contents.createChildren());
            }
        }
        return copy != null ? copy : sprites;
    }

    public static void afterLoadSprites(Map<ResourceLocation,TextureAtlasSprite> textures, int atlasWidth, int atlasHeight, Stitcher<SpriteContents> stitcher){
        // Collect dummy textures
        Set<DummyTextureSpriteContents> dummyTextures = new HashSet<>();
        for(Map.Entry<ResourceLocation,TextureAtlasSprite> entry : textures.entrySet()){
            TextureAtlasSprite texture = entry.getValue();
            if(!(texture.contents() instanceof DummyTextureSpriteContents.Child))
                continue;
            dummyTextures.add(((DummyTextureSpriteContents.Child)texture.contents()).parent().getTopTexture());
        }
        // Replace sprites
        for(DummyTextureSpriteContents contents : dummyTextures){
            try{
                createTextureInstance(contents, textures, atlasWidth, atlasHeight, stitcher);
            }catch(Exception e){
                FusionClient.LOGGER.error("Error while creating texture '{}': {}", contents.name(), e.getMessage());
                removeTextureSprites(contents, textures);
            }
        }
    }

    private static TextureInstance<?> createTextureInstance(DummyTextureSpriteContents contents, Map<ResourceLocation,TextureAtlasSprite> textures, int atlasWidth, int atlasHeight, Stitcher<SpriteContents> stitcher){
        // Create sub-textures
        TextureInstance<?> defaultSubTexture = null;
        for(DummyTextureSpriteContents subTexture : contents.getSubTextures()){
            try{
                TextureInstance<?> textureInstance = createTextureInstance(subTexture, textures, atlasWidth, atlasHeight, stitcher);
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
            TextureAtlasSprite currentSprite = textures.get(identifier);
            SpriteBuilderImpl spriteBuilder = child.spriteBuilder();
            // Create custom sprite
            SpriteBuilder.Constructor constructor = spriteBuilder.getConstructor();
            TextureAtlasSprite newSprite;
            if(constructor == null){
                newSprite = new TextureAtlasSprite(
                    currentSprite.atlasLocation(),
                    new FusionSpriteContents(identifier, (SpriteImageSourceImpl)spriteBuilder.getImageSource(), contents.additionalMetadata),
                    atlasWidth,
                    atlasHeight,
                    currentSprite.getX(),
                    currentSprite.getY()
                ) {};
            }else{
                AllocatedSprite allocation = new AllocatedSpriteImpl(
                    identifier,
                    currentSprite.getX(), currentSprite.getY(),
                    child.width(), child.height(),
                    currentSprite.getU0(), currentSprite.getU1(),
                    currentSprite.getV0(), currentSprite.getV1()
                );
                if(context == null){
                    context = new SpriteConstructionContextImpl(
                        atlasWidth, atlasHeight,
                        currentSprite.atlasLocation(),
                        stitcher.mipLevel
                    );
                }
                try{
                    newSprite = constructor.create(allocation, context);
                }catch(Exception e){
                    throw new RuntimeException("Encountered an exception whilst creating sprite '" + identifier + "' for texture type '" + TextureTypeRegistryImpl.getIdentifier(textureOutput.getTextureType()) + "'!", e);
                }
                if(!identifier.equals(newSprite.contents().name()))
                    throw new RuntimeException("Sprite constructor for sprite '" + identifier + "' from texture type '" + TextureTypeRegistryImpl.getIdentifier(textureOutput.getTextureType()) + "' returned sprite with incorrect identifier '" + newSprite.contents().name() + "'!");
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
            textures.put(spriteInstance.getIdentifier(), spriteInstance.getSprite());
        return textureInstance;
    }

    private static void removeTextureSprites(DummyTextureSpriteContents contents, Map<ResourceLocation,TextureAtlasSprite> textures){
        for(DummyTextureSpriteContents.Child child : contents.children())
            textures.remove(child.name());
        for(DummyTextureSpriteContents subTexture : contents.getSubTextures())
            removeTextureSprites(subTexture, textures);
    }

    public record Result<T>(T value) {
        static <T> Result<T> empty(){
            return new Result<>(null);
        }
    }
}
