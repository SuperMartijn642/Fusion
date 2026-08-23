package com.supermartijn642.fusion.integration.iris;

import com.mojang.blaze3d.platform.NativeImage;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.texture.RawTextureInstance;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.util.UserErrorException;
import com.supermartijn642.fusion.texture.FusionSpriteContents;
import com.supermartijn642.fusion.texture.FusionTextureMetadataSection;
import com.supermartijn642.fusion.texture.TextureTypeRegistryImpl;
import com.supermartijn642.fusion.texture.custom.*;
import com.supermartijn642.fusion.util.LoggingHelper;
import net.irisshaders.iris.texture.pbr.PBRAtlasTexture;
import net.irisshaders.iris.texture.pbr.PBRSpriteHolder;
import net.irisshaders.iris.texture.pbr.PBRType;
import net.irisshaders.iris.texture.pbr.SpriteContentsExtension;
import net.irisshaders.iris.texture.pbr.loader.AtlasPBRLoader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created 18/08/2026 by SuperMartijn642
 */
public class IrisPBRTextureCreationHandler {

    private static final Set<ResourceLocation> PROCESSED_TEXTURES = new HashSet<>();

    public static void clear(){
        PROCESSED_TEXTURES.clear();
    }

    /**
     * {@link AtlasPBRLoader.PBRSpriteContents} has protected access. Since the constructor for {@link AtlasPBRLoader.PBRTextureAtlasSprite} requires it, we have to use reflection to create both.
     */
    private static final Constructor<?> pbrSpriteContentsConstructor, pbrTextureAtlasSpriteConstructor;

    static{
        try{
            // PBRSpriteContents
            Class<?> pbrSpriteContents = null;
            for(Class<?> declaredClass : AtlasPBRLoader.class.getDeclaredClasses()){
                if(declaredClass.getName().endsWith(".AtlasPBRLoader$PBRSpriteContents")){
                    pbrSpriteContents = declaredClass;
                    break;
                }
            }
            if(pbrSpriteContents == null)
                throw new IllegalStateException("Could not find PBRSpriteContents sub-class!");
            Constructor<?> constructor = pbrSpriteContents.getDeclaredConstructor(ResourceLocation.class, FrameSize.class, NativeImage.class, AnimationMetadataSection.class, PBRType.class);
            constructor.setAccessible(true);
            pbrSpriteContentsConstructor = constructor;

            // PBRTextureAtlasSprite
            constructor = AtlasPBRLoader.PBRTextureAtlasSprite.class.getDeclaredConstructor(ResourceLocation.class, pbrSpriteContents, int.class, int.class, int.class, int.class, TextureAtlasSprite.class);
            constructor.setAccessible(true);
            pbrTextureAtlasSpriteConstructor = constructor;
        }catch(Exception e){
            throw new RuntimeException("Fusion failed to make Iris' PBRSpriteContents and PBRTextureAtlasSprite constructors accessible!", e);
        }
    }

    /**
     * Creates an instance of {@link AtlasPBRLoader.PBRTextureAtlasSprite} via reflection as it is protected.
     */
    private static AtlasPBRLoader.PBRTextureAtlasSprite instantiatePBRTextureAtlasSprite(ResourceLocation name, FrameSize size, NativeImage image, AnimationMetadataSection animationMetadata, PBRType pbrType, int atlasWidth, int atlasHeight, int x, int y, TextureAtlasSprite baseSprite){
        try{
            Object spriteContents = pbrSpriteContentsConstructor.newInstance(name, size, image, animationMetadata, pbrType);
            return (AtlasPBRLoader.PBRTextureAtlasSprite)pbrTextureAtlasSpriteConstructor.newInstance(name, spriteContents, atlasWidth, atlasHeight, x, y, baseSprite);
        }catch(InvocationTargetException | InstantiationException | IllegalAccessException e){
            throw new RuntimeException(e);
        }
    }

    public static void createPBRTexture(TextureInstance<?> textureInstance, ResourceManager resourceManager, int atlasWidth, int atlasHeight, int mipLevel, PBRType pbrType, Supplier<PBRAtlasTexture> pbrAtlas){
        // Check for parent texture
        TextureInstance<?> parentTexture = ((TextureInstanceImpl<?>)textureInstance).getParent();
        if(parentTexture != null){
            createPBRTexture(parentTexture, resourceManager, atlasWidth, atlasHeight, mipLevel, pbrType, pbrAtlas);
            return;
        }

        // Check if texture has already been processed
        if(!PROCESSED_TEXTURES.add(textureInstance.getIdentifier()))
            return;

        // Read pbr image
        ResourceLocation identifier = textureInstance.getIdentifier();
        ResourceLocation pbrIdentifier = identifier.withPath(pbrType.appendSuffix(identifier.getPath()));
        ResourceLocation pbrImageLocation = pbrIdentifier.withPrefix("textures/").withSuffix(".png");
        Resource pbrResource = resourceManager.getResource(pbrImageLocation).orElse(null);
        if(pbrResource == null)
            return;
        NativeImage pbrImage;
        try(InputStream stream = pbrResource.open()){
            pbrImage = NativeImage.read(stream);
        }catch(IOException e){
            FusionClient.LOGGER.error("Error reading PBR image '{}'", pbrImageLocation, e);
            return;
        }

        // Get metadata from regular texture
        ResourceLocation regularImageLocation = identifier.withPrefix("textures/").withSuffix(".png");
        Resource resource = resourceManager.getResource(regularImageLocation).orElse(null);
        if(resource == null){
            FusionClient.LOGGER.error("Unable to get regular image '{}' whilst creating Iris PBR texture!", regularImageLocation);
            return;
        }
        ResourceMetadata metadata;
        try{
            metadata = resource.metadata();
        }catch(Exception e){
            FusionClient.LOGGER.error("Failed to parse regular image metadata for '{}' whilst creating Iris PBR texture!", regularImageLocation, e);
            return;
        }

        // Get animation metadata
        AnimationMetadataSection animationMetadata = metadata.getSection(AnimationMetadataSection.SERIALIZER).orElse(AnimationMetadataSection.EMPTY);
        if(animationMetadata == AnimationMetadataSection.EMPTY)
            animationMetadata = null;

        // Get the fusion metadata
        //noinspection unchecked,rawtypes
        RawTextureInstance<Object,Object> rawTexture = (RawTextureInstance)metadata.getSection(FusionTextureMetadataSection.INSTANCE).orElse(null);
        if(rawTexture == null){
            FusionClient.LOGGER.error("Missing Fusion metadata for regular image '{}' whilst creating Iris PBR texture!", regularImageLocation);
            return;
        }

        // Create pbr texture
        TextureOutputImpl<Object> output = new TextureOutputImpl<>(pbrIdentifier, rawTexture.getTextureType());
        try(TextureCreationContextImpl context = new TextureCreationContextImpl(pbrIdentifier, pbrImage, animationMetadata)){
            rawTexture.createTexture(output, context);
            output.finish();
        }catch(UserErrorException e){
            FusionClient.LOGGER.error("Error for Iris PBR texture '{}': {}", pbrIdentifier, e.getMessage());
            return;
        }catch(Exception e){
            FusionClient.LOGGER.error("Encountered an exception whilst creating Iris PBR texture for type '{}' for texture '{}'!", TextureTypeRegistryImpl.getIdentifier(rawTexture.getTextureType()), pbrIdentifier, e);
            return;
        }

        // Create sprites
        List<AtlasPBRLoader.PBRTextureAtlasSprite> sprites = new ArrayList<>();
        try{
            createPBRSprites(output, textureInstance, atlasWidth, atlasHeight, mipLevel, pbrType, sprites::add);
        }catch(UserErrorException e){
            LoggingHelper.logUserError(e, "Failed to create Iris PBR texture '%s' with texture type '%s'", pbrIdentifier, TextureTypeRegistryImpl.getIdentifier(textureInstance.getTextureType()));
            return;
        }catch(Exception e){
            FusionClient.LOGGER.error("Encountered an exception whilst creating Iris PBR texture sprites for type '{}' for texture '{}'!", TextureTypeRegistryImpl.getIdentifier(rawTexture.getTextureType()), pbrIdentifier, e);
            return;
        }
        PBRAtlasTexture atlas = pbrAtlas.get();
        for(AtlasPBRLoader.PBRTextureAtlasSprite sprite : sprites){
            PBRSpriteHolder holder = ((SpriteContentsExtension)sprite.getBaseSprite().contents()).getOrCreatePBRHolder();
            if(pbrType == PBRType.NORMAL)
                holder.setNormalSprite(sprite);
            else
                holder.setSpecularSprite(sprite);
            atlas.addSprite(sprite);
        }
    }

    private static void createPBRSprites(TextureOutputImpl<?> textureOutput, TextureInstance<?> regularTexture, int atlasWidth, int atlasHeight, int mipLevel, PBRType pbrType, Consumer<AtlasPBRLoader.PBRTextureAtlasSprite> spriteConsumer) throws UserErrorException{
        // Create sub-texture instances
        for(int i = 0; i < textureOutput.getSubTextures().size() && i < ((TextureInstanceImpl<?>)regularTexture).getSubTextures().size(); i++){
            TextureOutputImpl<?> subTextureOutput = textureOutput.getSubTextures().get(i);
            TextureInstance<?> regularSubTexture = ((TextureInstanceImpl<?>)regularTexture).getSubTextures().get(i);
            try{
                createPBRSprites(subTextureOutput, regularSubTexture, atlasWidth, atlasHeight, mipLevel, pbrType, spriteConsumer);
            }catch(UserErrorException e){
                throw new UserErrorException("Failed to create sub-texture of type '" + TextureTypeRegistryImpl.getIdentifier(textureOutput.getTextureType()) + "'", e);
            }catch(Exception e){
                throw new RuntimeException("Failed to create sub-texture of type '" + TextureTypeRegistryImpl.getIdentifier(textureOutput.getTextureType()) + "'!", e);
            }
        }

        // Create mapping of regular texture sprites by their suffix
        Map<String,SpriteInstance> regularSprites = new HashMap<>();
        for(SpriteInstance sprite : regularTexture.getSprites()){
            if(regularTexture.getDefaultSprite() == sprite)
                continue;
            regularSprites.put(
                sprite.getIdentifier().getPath().substring(regularTexture.getIdentifier().getPath().length()),
                sprite
            );
        }

        // Create the sprites
        List<AtlasPBRLoader.PBRTextureAtlasSprite> sprites = new ArrayList<>(textureOutput.getSprites().size());
        for(SpriteBuilderImpl spriteBuilder : textureOutput.getSprites()){
            if(spriteBuilder.getConstructor() != null)
                throw new UserErrorException("Texture type uses custom sprite constructor and thus cannot be used with Iris PBR textures!");
            ResourceLocation identifier = spriteBuilder.getIdentifier();
            SpriteInstance regularSprite = spriteBuilder.isMarkedDefault() ?
                regularTexture.getDefaultSprite() :
                regularSprites.get(identifier.getPath().substring(textureOutput.getIdentifier().getPath().length()));
            if(regularSprite == null)
                continue;
            if(spriteBuilder.getWidth() != regularSprite.getSprite().contents().width() || spriteBuilder.getHeight() != regularSprite.getSprite().contents().height())
                throw new UserErrorException("PBR sprites must have same dimensions as the regular texture!");
            // Create custom sprite
            FusionSpriteContents contents = new FusionSpriteContents(identifier, (SpriteImageSourceImpl)spriteBuilder.getImageSource());
            contents.increaseMipLevel(mipLevel);
            AtlasPBRLoader.PBRTextureAtlasSprite sprite = instantiatePBRTextureAtlasSprite(
                identifier,
                new FrameSize(((SpriteImageSourceImpl)spriteBuilder.getImageSource()).getFrameWidth(), ((SpriteImageSourceImpl)spriteBuilder.getImageSource()).getFrameHeight()),
                ((SpriteImageSourceImpl)spriteBuilder.getImageSource()).getImage(),
                AnimationMetadataSection.EMPTY,
                pbrType,
                atlasWidth,
                atlasHeight,
                regularSprite.getSprite().getX(),
                regularSprite.getSprite().getY(),
                regularSprite.getSprite()
            );
            sprite.contents = contents;
            sprites.add(sprite);
        }

        // Add sprites to the atlas
        sprites.forEach(spriteConsumer);
    }
}
