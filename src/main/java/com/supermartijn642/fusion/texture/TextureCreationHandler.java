package com.supermartijn642.fusion.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.*;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.api.util.UserErrorException;
import com.supermartijn642.fusion.extensions.TextureAtlasSpriteExtension;
import com.supermartijn642.fusion.texture.custom.*;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;

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

    public static Result<SpriteContents> onLoadTexture(ResourceLocation identifier, NativeImage image, AnimationMetadataSection animationMetadata, ResourceMetadata resourceMetadata){
        // Get the fusion metadata
        //noinspection unchecked,rawtypes
        Pair<TextureType<Object,Object>,Object> metadata = (Pair)resourceMetadata.getSection(FusionTextureMetadataSection.INSTANCE).orElse(null);
        if(metadata == null)
            return null;

        TextureType<Object,Object> textureType = metadata.left();
        Object textureData = metadata.right();

        if(animationMetadata == AnimationMetadataSection.EMPTY)
            animationMetadata = null;

        // Create texture
        TextureOutputImpl output = new TextureOutputImpl();
        try(TextureCreationContextImpl context = new TextureCreationContextImpl(identifier, image, animationMetadata)){
            textureType.createTexture(output, context, textureData);
            output.checkFinished();
        }catch(UserErrorException e){
            FusionClient.LOGGER.error("Error for texture '{}': {}", identifier, e.getMessage());
            image.close();
            return Result.empty();
        }catch(Exception e){
            FusionClient.LOGGER.error("Encountered an exception whilst creating texture for type '{}' for texture '{}'!", TextureTypeRegistryImpl.getIdentifier(textureType), identifier, e);
            image.close();
            return Result.empty();
        }
        if(output.getSprites().isEmpty()){
            image.close();
            return Result.empty();
        }
        List<SpriteBuilderImpl> sprites = output.getSprites();
        Object customData = output.getCustomData();
        Consumer<TextureInstance<Object>> textureCreationCallback = output.getCallback();

        // Give unique sub-sprite names
        if(sprites.size() > 1){
            boolean hasDefaultSprite = false;
            Set<String> names = new HashSet<>();
            int index = 0;
            for(SpriteBuilderImpl sprite : sprites){
                if(sprite.isMarkedDefault()){
                    hasDefaultSprite = true;
                    sprite.setNameUnchecked(null);
                    continue;
                }
                if(sprite.getName() != null && !names.add(sprite.getName())){
                    FusionClient.LOGGER.error("Received duplicate sprite name '{}' from texture type '{}' for texture '{}'!", sprite.getName(), TextureTypeRegistryImpl.getIdentifier(textureType), identifier);
                    image.close();
                    return Result.empty();
                }
            }
            if(!hasDefaultSprite){
                sprites.get(0).markDefaultUnchecked();
                sprites.get(0).setNameUnchecked(null);
            }
            for(SpriteBuilderImpl sprite : sprites){
                if(sprite.isMarkedDefault() || sprite.getName() != null)
                    continue;
                String name = "sub_sprite_" + index++;
                while(names.contains(name))
                    name = "sub_sprite_" + index++;
                sprite.setNameUnchecked(name);
            }
        }else
            sprites.get(0).setNameUnchecked(null);

        // Create the sprite contents
        return new Result<>(new DummyTextureSpriteContents(
            identifier,
            textureType,
            customData,
            sprites,
            textureCreationCallback
        ));
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
        // Replace sprites
        loop:
        for(Map.Entry<ResourceLocation,TextureAtlasSprite> entry : textures.entrySet()){
            TextureAtlasSprite texture = entry.getValue();
            if(!(texture.contents() instanceof DummyTextureSpriteContents.Child))
                continue;
            DummyTextureSpriteContents contents = ((DummyTextureSpriteContents.Child)texture.contents()).parent();

            // Create the custom sprites
            List<TextureAtlasSprite> sprites = new ArrayList<>(contents.spriteBuilders().size());
            SpriteConstructionContext context = null;
            for(DummyTextureSpriteContents.Child child : contents.children()){
                TextureAtlasSprite currentSprite = textures.get(child.name());
                SpriteBuilderImpl spriteBuilder = child.spriteBuilder();
                SpriteBuilder.Constructor constructor = spriteBuilder.getConstructor();
                if(constructor == null){
                    sprites.add(new TextureAtlasSprite(
                        texture.atlasLocation(),
                        new FusionSpriteContents(child.name(), (SpriteImageSourceImpl)spriteBuilder.getImageSource()),
                        atlasWidth,
                        atlasHeight,
                        currentSprite.getX(),
                        currentSprite.getY()
                    ) {});
                }else{
                    AllocatedSprite allocation = new AllocatedSpriteImpl(
                        child.name(),
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
                    TextureAtlasSprite sprite;
                    try{
                        sprite = constructor.create(allocation, context);
                    }catch(Exception e){
                        FusionClient.LOGGER.error("Encountered an exception whilst creating sprite '{}' for texture type '{}'!", child.name(), TextureTypeRegistryImpl.getIdentifier(contents.textureType()), e);
                        continue loop;
                    }
                    if(!child.name().equals(sprite.contents().name())){
                        FusionClient.LOGGER.error("Sprite constructor for sprite '{}' from texture type '{}' returned sprite with incorrect identifier '{}'!", child.name(), TextureTypeRegistryImpl.getIdentifier(contents.textureType()), sprite.contents().name());
                        continue loop;
                    }
                    sprites.add(sprite);
                }
            }

            // Create texture instance
            TextureInstanceImpl<Object> textureInstance = new TextureInstanceImpl<>(
                contents.textureType(),
                contents.identifier(),
                contents.textureData()
            );
            // Create sprite instances
            List<SpriteInstance> spriteInstances = new ArrayList<>(sprites.size());
            for(TextureAtlasSprite sprite : sprites){
                SpriteInstanceImpl spriteInstance = new SpriteInstanceImpl(textureInstance, sprite, sprite.contents().name());
                ((TextureAtlasSpriteExtension)sprite).setFusionSpriteInstance(spriteInstance);
                spriteInstances.add(spriteInstance);
            }
            textureInstance.setSprites(spriteInstances);

            // Call callbacks
            for(int i = 0; i < spriteInstances.size(); i++){
                SpriteInstance spriteInstance = spriteInstances.get(i);
                Consumer<SpriteInstance> callback = contents.children().get(i).spriteBuilder().getCallback();
                if(callback == null)
                    continue;
                try{
                    callback.accept(spriteInstance);
                }catch(Exception e){
                    FusionClient.LOGGER.error("Encountered an exception whilst calling sprite creation callback for sprite '{}' from texture type '{}'!", spriteInstance.getIdentifier(), TextureTypeRegistryImpl.getIdentifier(contents.textureType()), e);
                    continue loop;
                }
            }
            if(contents.textureCreationCallback() != null){
                try{
                    contents.textureCreationCallback().accept(textureInstance);
                }catch(Exception e){
                    FusionClient.LOGGER.error("Encountered an exception whilst calling texture creation callback for texture '{}' from texture type '{}'!", contents.identifier(), TextureTypeRegistryImpl.getIdentifier(contents.textureType()), e);
                    continue;
                }
            }

            // Replace the current sprites
            for(SpriteInstance spriteInstance : spriteInstances)
                textures.put(spriteInstance.getIdentifier(), spriteInstance.getSprite());
        }
    }

    public record Result<T>(T value) {
        static <T> Result<T> empty(){
            return new Result<>(null);
        }
    }
}
