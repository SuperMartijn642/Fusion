package com.supermartijn642.fusion.texture;

import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.*;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.api.util.UserErrorException;
import com.supermartijn642.fusion.extensions.TextureAtlasSpriteExtension;
import com.supermartijn642.fusion.texture.custom.*;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.resources.IResource;
import net.minecraft.util.ResourceLocation;

import java.io.InputStream;
import java.util.*;
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

    public static boolean onLoadTexture(ResourceLocation identifier, IResource resource, Queue<TextureAtlasSprite.Info> queue){
        // Get the fusion metadata
        Pair<TextureType<Object,Object>,Object> metadata;
        try{
            //noinspection unchecked,rawtypes
            metadata = (Pair)resource.getMetadata(FusionTextureMetadataSection.INSTANCE);
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
        if(metadata == null)
            return false;

        TextureType<Object,Object> textureType = metadata.left();
        Object textureData = metadata.right();

        // Get vanilla animation metadata
        AnimationMetadataSection animationMetadata;
        try{
            animationMetadata = resource.getMetadata(AnimationMetadataSection.SERIALIZER);
        }catch(Exception e){
            FusionClient.LOGGER.error("Unable to parse animation metadata for texture '{}':", identifier, e);
            return true;
        }

        // Read image
        NativeImage image;
        try(InputStream stream = resource.getInputStream()){
            image = NativeImage.read(stream);
        }catch(Exception e){
            FusionClient.LOGGER.error("Encountered an exception reading image for texture '{}'!", identifier, e);
            return true;
        }

        // Create texture
        TextureOutputImpl output = new TextureOutputImpl();
        try(TextureCreationContextImpl context = new TextureCreationContextImpl(identifier, image, animationMetadata)){
            textureType.createTexture(output, context, textureData);
            output.checkFinished();
        }catch(UserErrorException e){
            FusionClient.LOGGER.error("Error for texture '{}': {}", identifier, e.getMessage());
            image.close();
            return true;
        }catch(Exception e){
            FusionClient.LOGGER.error("Encountered an exception whilst creating texture for type '{}' for texture '{}'!", TextureTypeRegistryImpl.getIdentifier(textureType), identifier, e);
            image.close();
            return true;
        }
        if(output.getSprites().isEmpty()){
            image.close();
            return true;
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
                    return true;
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

        // Create dummy sprite contents for each sub-sprite
        DummyTextureSpriteContents parent = new DummyTextureSpriteContents(
            identifier,
            textureType,
            customData,
            sprites,
            textureCreationCallback
        );
        queue.addAll(parent.createChildren());
        return true;
    }

    public static Result<CompletableFuture<Void>> onLoadSprite(TextureAtlasSprite.Info spriteInfo, int spriteX, int spriteY, AtlasTexture textureAtlas, int atlasWidth, int atlasHeight, int mipmapLevels, Queue<TextureAtlasSprite> queue){
        // Check if the sprite is from Fusion
        if(!(spriteInfo instanceof DummyTextureSpriteContents.Child))
            return null;
        DummyTextureSpriteContents.Child child = (DummyTextureSpriteContents.Child)spriteInfo;

        // Store the allocated area
        child.setAllocation(new AllocatedSpriteImpl(
            spriteInfo.name(),
            spriteX, spriteY,
            spriteInfo.width, spriteInfo.height,
            (float)spriteX / atlasWidth, (float)(spriteX + spriteInfo.width) / atlasWidth,
            (float)spriteY / atlasHeight, (float)(spriteY + spriteInfo.height) / atlasHeight
        ));

        // If all child sprites have been allocated, create the texture
        if(child.parent().hasAllAllocations())
            return new Result<>(CompletableFuture.runAsync(() -> createTexture(child.parent(), textureAtlas, atlasWidth, atlasHeight, mipmapLevels, queue)));
        return new Result<>(null);
    }

    private static void createTexture(DummyTextureSpriteContents contents, AtlasTexture textureAtlas, int atlasWidth, int atlasHeight, int mipmapLevels, Queue<TextureAtlasSprite> queue){
        // Create the custom sprites
        List<TextureAtlasSprite> sprites = new ArrayList<>(contents.spriteBuilders().size());
        SpriteConstructionContext context = null;
        for(DummyTextureSpriteContents.Child child : contents.children()){
            SpriteBuilderImpl spriteBuilder = child.spriteBuilder();
            SpriteBuilder.Constructor constructor = spriteBuilder.getConstructor();
            if(constructor == null){
                sprites.add(new FusionTextureAtlasSprite(
                    child.allocation(),
                    textureAtlas,
                    (SpriteImageSourceImpl)spriteBuilder.getImageSource(),
                    mipmapLevels
                ));
            }else{
                AllocatedSprite allocation = child.allocation();
                if(context == null){
                    context = new SpriteConstructionContextImpl(
                        atlasWidth, atlasHeight,
                        textureAtlas,
                        mipmapLevels
                    );
                }
                TextureAtlasSprite sprite;
                try{
                    sprite = constructor.create(allocation, context);
                }catch(Exception e){
                    FusionClient.LOGGER.error("Encountered an exception whilst creating sprite '{}' for texture type '{}'!", child.name(), TextureTypeRegistryImpl.getIdentifier(contents.textureType()), e);
                    return;
                }
                if(!child.name().equals(sprite.getName())){
                    FusionClient.LOGGER.error("Sprite constructor for sprite '{}' from texture type '{}' returned sprite with incorrect identifier '{}'!", child.name(), TextureTypeRegistryImpl.getIdentifier(contents.textureType()), sprite.getName());
                    return;
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
            SpriteInstanceImpl spriteInstance = new SpriteInstanceImpl(textureInstance, sprite, sprite.getName());
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
                return;
            }
        }
        if(contents.textureCreationCallback() != null){
            try{
                contents.textureCreationCallback().accept(textureInstance);
            }catch(Exception e){
                FusionClient.LOGGER.error("Encountered an exception whilst calling texture creation callback for texture '{}' from texture type '{}'!", contents.identifier(), TextureTypeRegistryImpl.getIdentifier(contents.textureType()), e);
                return;
            }
        }

        // Add the sprites
        queue.addAll(sprites);
    }

    public static class Result<T> {

        private final T value;

        public Result(T value){
            this.value = value;
        }

        public T value(){
            return this.value;
        }
    }
}
