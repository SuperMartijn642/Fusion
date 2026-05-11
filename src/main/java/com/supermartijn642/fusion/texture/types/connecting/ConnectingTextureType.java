package com.supermartijn642.fusion.texture.types.connecting;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.platform.NativeImage;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.*;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureData;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureLayout;
import com.supermartijn642.fusion.api.util.UserErrorException;
import com.supermartijn642.fusion.texture.DummyTextureSpriteContents;
import com.supermartijn642.fusion.texture.types.connecting.layouts.ConnectingTextureLayoutHandler;
import net.minecraft.client.resources.metadata.animation.AnimationFrame;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
public class ConnectingTextureType implements TextureType<ConnectingTextureData,StitchedConnectingTextureData> {

    @Override
    public void createTexture(TextureOutput<StitchedConnectingTextureData> output, TextureCreationContext context, ConnectingTextureData data) throws UserErrorException{
        ConnectingTextureLayoutHandler layout = ConnectingTextureLayoutHandler.get(data.getLayout());
        int imageWidth = context.getImageWidth(), imageHeight = context.getImageHeight();
        NativeImage image = context.getImage();

        // Calculate frame size
        int frameWidth = context.getImageWidth(), frameHeight = context.getImageHeight();
        int defaultTileSize = Math.min(frameWidth / layout.getWidth(), frameHeight / layout.getHeight());
        AnimationMetadataSection animationMetadata = context.getAnimationMetadata();
        if(data.getLayout() == ConnectingTextureLayout.FULL && frameWidth == frameHeight){ // Legacy full layout was a square image, so change the framing to the new aspect ratio
            if(animationMetadata != null)
                throw new UserErrorException("Image must use the 'full' layouts 6 : 8 aspect ratio to support animation!");
            frameHeight = frameHeight * 6 / 8;
            imageHeight = imageHeight * 6 / 8;
            image = ImageHelper.createCrop(image, 0, 0, imageWidth, imageHeight, true);
        }else if(animationMetadata != null){
            if(animationMetadata.frameWidth < 0 && animationMetadata.frameHeight < 0){
                // Use the expected aspect ratio for the layout
                frameWidth = layout.getWidth() * defaultTileSize;
                frameHeight = layout.getHeight() * defaultTileSize;
            }else{
                if(animationMetadata.frameWidth >= 0)
                    frameWidth = animationMetadata.frameWidth;
                if(animationMetadata.frameHeight >= 0)
                    frameHeight = animationMetadata.frameHeight;
            }
        }

        // Do frame size checks
        if(frameWidth == 0 || frameHeight == 0)
            throw new UserErrorException("Image must not be empty!");
        if(imageWidth % frameWidth != 0 || imageHeight % frameHeight != 0)
            throw new UserErrorException("Image size " + imageWidth + "x" + imageHeight + " is not a multiple of frame size " + frameWidth + "x" + frameHeight + "!");
        if(frameWidth % layout.getWidth() != 0 || frameHeight % layout.getHeight() != 0)
            throw new UserErrorException("Image/frame size " + frameWidth + "x" + frameHeight + " is not a multiple of '" + data.getLayout().name().toLowerCase(Locale.ROOT) + "' layout's " + layout.getWidth() + " : " + layout.getHeight() + " aspect ratio!");

        // Create animation data
        int frameColumns = imageWidth / frameWidth;
        int frameRows = imageHeight / frameHeight;
        int tileWidth = frameWidth / layout.getWidth();
        int tileHeight = frameHeight / layout.getHeight();
        List<SpriteImageSource.AnimationFrame> frames = null;
        if(animationMetadata != null){
            if(!animationMetadata.frames.isEmpty()){
                frames = new ArrayList<>(animationMetadata.frames.size());
                for(AnimationFrame frame : animationMetadata.frames){
                    int index = frame.getIndex();
                    if(index >= frameRows * frameColumns)
                        throw new UserErrorException("Frame index " + index + " is greater than the number of frames in the image!");
                    int x = tileWidth * (index % frameColumns);
                    int y = tileHeight * (index / frameColumns);
                    frames.add(SpriteImageSource.AnimationFrame.of(x, y, frame.getTime(animationMetadata.getDefaultFrameTime())));
                }
            }else{
                frames = new ArrayList<>(frameRows * frameColumns);
                for(int row = 0; row < frameRows; row++){
                    for(int column = 0; column < frameColumns; column++){
                        frames.add(SpriteImageSource.AnimationFrame.of(column * tileWidth, row * tileHeight, animationMetadata.getDefaultFrameTime()));
                    }
                }
            }
            if(frameRows == 1 && frameColumns == 1) // If there is only a single frame, ignore the animation data but still validate it
                frames = null;
        }

        // Create sprites
        List<SpriteInstance> tiles = new ArrayList<>(layout.getWidth() * layout.getHeight());
        try(NativeImage n = image){
            for(int y = 0; y < layout.getHeight(); y++){
                for(int x = 0; x < layout.getWidth(); x++){
                    tiles.add(null);
                    // Skip empty tiles
                    if((x != layout.defaultTileX() || y != layout.defaultTileY()) &&
                        DummyTextureSpriteContents.isSubImageEmpty(image, x * tileWidth, y * tileHeight, tileWidth, tileHeight)){
                        continue;
                    }
                    NativeImage subImage = ImageHelper.createCropFramed(image, x * tileWidth, y * tileHeight, tileWidth, tileHeight, frameWidth, frameHeight, false);
                    SpriteImageSource imageSource = frames == null ?
                        SpriteImageSource.constant(subImage) :
                        SpriteImageSource.animated(subImage, tileWidth, tileHeight, frames, animationMetadata.isInterpolatedFrames());
                    int index = x + y * layout.getWidth();
                    output.createSprite()
                        .image(imageSource)
                        .markDefaultSprite(x == layout.defaultTileX() && y == layout.defaultTileY())
                        .setCreationCallback(s -> tiles.set(index, s))
                        .submit();
                }
            }
        }

        // Set custom texture data
        output.setCustomData(new StitchedConnectingTextureData(data, tiles));
    }

    @Override
    public ConnectingTextureData deserialize(JsonObject json) throws JsonParseException{
        // Deserialize base properties
        BaseTextureData base = DefaultTextureTypes.BASE.deserialize(json);
        // Copy base properties
        ConnectingTextureData.Builder builder = ConnectingTextureData.builder();
        builder.renderType(base.getRenderType());
        builder.emissive(base.isEmissive());
        builder.tinting(base.getTinting());
        // Deserialize 'layout'
        if(json.has("layout")){
            if(!json.get("layout").isJsonPrimitive() || !json.getAsJsonPrimitive("layout").isString())
                throw new JsonParseException("Property 'layout' must be a string!");
            String layoutString = json.get("layout").getAsString();
            ConnectingTextureLayout layout;
            try{
                layout = ConnectingTextureLayout.valueOf(layoutString.toUpperCase(Locale.ROOT));
            }catch(IllegalArgumentException e){
                throw new JsonParseException("Property 'layout' must be one of " + Arrays.toString(ConnectingTextureLayout.values()).toLowerCase(Locale.ROOT) + ", not '" + layoutString + "'!");
            }
            builder.layout(layout);
        }
        return builder.build();
    }

    @Override
    public JsonObject serialize(ConnectingTextureData data){
        // Serialize base properties
        JsonObject json = DefaultTextureTypes.BASE.serialize(data);
        // Serialize 'layout'
        if(data.getLayout() != ConnectingTextureLayout.FULL)
            json.addProperty("layout", data.getLayout().name().toLowerCase(Locale.ROOT));
        return json.isEmpty() ? null : json;
    }
}
