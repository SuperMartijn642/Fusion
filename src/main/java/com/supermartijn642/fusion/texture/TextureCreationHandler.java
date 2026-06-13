package com.supermartijn642.fusion.texture;

import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.texture.RawTextureInstance;
import com.supermartijn642.fusion.api.texture.custom.*;
import com.supermartijn642.fusion.api.util.UserErrorException;
import com.supermartijn642.fusion.extensions.TextureAtlasSpriteExtension;
import com.supermartijn642.fusion.texture.custom.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.ReportedException;
import net.minecraft.util.ResourceLocation;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
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

    public static boolean onLoadTexture(ResourceLocation identifier, ResourceLocation location, IResourceManager resourceManager, Consumer<TextureAtlasSprite> queue){
        try(IResource resource = resourceManager.getResource(location)){
            return onLoadTexture(identifier, resource, queue);
        }catch(IOException ignore){
            // Let vanilla handle this
            return false;
        }
    }

    private static boolean onLoadTexture(ResourceLocation identifier, IResource resource, Consumer<TextureAtlasSprite> queue){
        // Get the fusion metadata
        RawTextureInstance<Object,Object> rawTexture = null;
        try{
            FusionTextureMetadataSection.Data data = resource.getMetadata(FusionTextureMetadataSection.INSTANCE.getSectionName());
            if(data != null)
                //noinspection unchecked,rawtypes
                rawTexture = (RawTextureInstance)data.texture;
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
            animationMetadata = resource.getMetadata("animation");
        }catch(Exception e){
            FusionClient.LOGGER.error("Unable to parse animation metadata for texture '{}':", identifier, e);
            return true;
        }

        // Read image
        BufferedImage image;
        try(InputStream stream = resource.getInputStream()){
            image = TextureUtil.readBufferedImage(stream);
        }catch(Exception e){
            FusionClient.LOGGER.error("Encountered an exception reading image for texture '{}'!", identifier, e);
            return true;
        }

        // Create texture
        TextureOutputImpl<Object> output = new TextureOutputImpl<>(identifier, rawTexture.getTextureType());
        TextureCreationContextImpl context = new TextureCreationContextImpl(identifier, image, animationMetadata);
        try{
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
        parent.createChildren().forEach(queue);
        return true;
    }

    public static void onLoadSprite(DummyTextureSpriteContents contents, int atlasWidth, int atlasHeight, int mipmapLevels, Consumer<TextureAtlasSprite> queue){
        if(!contents.hasAllAllocations())
            throw new IllegalStateException("Texture sprites have not been allocated!");

        List<TextureAtlasSprite> customSprites = new ArrayList<>();
        try{
            createTextureInstance(contents, atlasWidth, atlasHeight, mipmapLevels, customSprites::add);
        }catch(Exception e){
            FusionClient.LOGGER.error("Error while creating texture '{}': {}", contents.getTextureOutput().getIdentifier(), e.getMessage());
            return;
        }
        customSprites.forEach(queue);
    }

    private static TextureInstance<?> createTextureInstance(DummyTextureSpriteContents contents, int atlasWidth, int atlasHeight, int mipmapLevels, Consumer<TextureAtlasSprite> spriteOutput){
        // Create sub-textures
        TextureInstance<?> defaultSubTexture = null;
        for(DummyTextureSpriteContents subTexture : contents.getSubTextures()){
            try{
                TextureInstance<?> textureInstance = createTextureInstance(subTexture, atlasWidth, atlasHeight, mipmapLevels, spriteOutput);
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
                    (SpriteImageSourceImpl)spriteBuilder.getImageSource(),
                    mipmapLevels
                );
            }else{
                AllocatedSprite allocation = child.allocation();
                if(context == null){
                    context = new SpriteConstructionContextImpl(
                        atlasWidth, atlasHeight,
                        mipmapLevels
                    );
                }
                try{
                    newSprite = constructor.create(allocation, context);
                }catch(Exception e){
                    throw new RuntimeException("Encountered an exception whilst creating sprite '" + identifier + "' for texture type '" + TextureTypeRegistryImpl.getIdentifier(textureOutput.getTextureType()) + "'!", e);
                }
                if(!identifier.equals(newSprite.getIconName()))
                    throw new RuntimeException("Sprite constructor for sprite '" + identifier + "' from texture type '" + TextureTypeRegistryImpl.getIdentifier(textureOutput.getTextureType()) + "' returned sprite with incorrect identifier '" + newSprite.getIconName() + "'!");
            }
            // Create sprite instance
            SpriteInstance spriteInstance = new SpriteInstanceImpl(textureInstance, newSprite, identifier);
            //noinspection DataFlowIssue
            ((TextureAtlasSpriteExtension)newSprite).setFusionSpriteInstance(spriteInstance);
            sprites.add(spriteInstance);
            // Copy vanilla mipmap generation behaviour
            try{
                newSprite.generateMipmaps(mipmapLevels);
            }catch(Throwable throwable){
                CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Applying mipmap");
                CrashReportCategory crashreportcategory = crashreport.makeCategory("Sprite being mipmapped");
                crashreportcategory.addDetail("Sprite name", newSprite::getIconName);
                crashreportcategory.addDetail("Sprite size", () -> newSprite.getIconWidth() + " x " + newSprite.getIconHeight());
                crashreportcategory.addDetail("Sprite frames", () -> newSprite.getFrameCount() + " frames");
                crashreportcategory.addCrashSection("Mipmap levels", mipmapLevels);
                throw new ReportedException(crashreport);
            }
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

        // Upload sprites to atlas
        for(SpriteInstance spriteInstance : sprites){
            TextureAtlasSprite sprite = spriteInstance.getSprite();
            try{
                TextureUtil.uploadTextureMipmap(sprite.getFrameTextureData(0), sprite.getIconWidth(), sprite.getIconHeight(), sprite.getOriginX(), sprite.getOriginY(), false, false);
            }catch(Exception e){
                throw new RuntimeException("Encountered an exception whilst uploading sprite '" + sprite.getIconName() + "' from texture type '" + TextureTypeRegistryImpl.getIdentifier(textureOutput.getTextureType()) + "'!", e);
            }
        }

        // Replace the current sprites
        for(SpriteInstance spriteInstance : sprites)
            spriteOutput.accept(spriteInstance.getSprite());
        return textureInstance;
    }
}
