package com.supermartijn642.fusion.texture.types.scrolling;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.SpriteImageSource;
import com.supermartijn642.fusion.api.texture.custom.TextureCreationContext;
import com.supermartijn642.fusion.api.texture.custom.TextureErrorException;
import com.supermartijn642.fusion.api.texture.custom.TextureOutput;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.api.texture.data.ScrollingTextureData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Created 28/04/2023 by SuperMartijn642
 */
public class ScrollingTextureType implements TextureType<ScrollingTextureData,BaseTextureData> {

    @Override
    public void createTexture(TextureOutput<BaseTextureData> output, TextureCreationContext context, ScrollingTextureData data) throws TextureErrorException{
        // Can not be combined with vanilla animation data
        if(context.getAnimationMetadata() != null)
            throw new TextureErrorException("Can not be used in combination with vanilla animation data!");

        // Validate frame size
        if(context.getImageWidth() < data.getFrameWidth() || context.getImageHeight() < data.getFrameHeight())
            throw new TextureErrorException("Image size " + context.getImageWidth() + "x" + context.getImageHeight() + " is smaller than frame size " + data.getFrameWidth() + "x" + data.getFrameHeight() + "!");

        // Calculate frame start and end
        boolean reverse = data.getLoopType() == ScrollingTextureData.LoopType.REVERSE;
        int startX = data.getStartPosition() == ScrollingTextureData.Position.TOP_LEFT || data.getStartPosition() == ScrollingTextureData.Position.BOTTOM_LEFT ? 0 : context.getImageWidth() - data.getFrameWidth();
        int startY = data.getStartPosition() == ScrollingTextureData.Position.TOP_LEFT || data.getStartPosition() == ScrollingTextureData.Position.TOP_RIGHT ? 0 : context.getImageHeight() - data.getFrameHeight();
        int endX = data.getEndPosition() == ScrollingTextureData.Position.TOP_LEFT || data.getEndPosition() == ScrollingTextureData.Position.BOTTOM_LEFT ? 0 : context.getImageWidth() - data.getFrameWidth();
        int endY = data.getEndPosition() == ScrollingTextureData.Position.TOP_LEFT || data.getEndPosition() == ScrollingTextureData.Position.TOP_RIGHT ? 0 : context.getImageHeight() - data.getFrameHeight();

        // Calculate all the frames
        int stepCount = Math.max(Math.abs(endX - startX), Math.abs(endY - startY)) + 1;
        int frameCount = reverse ? Math.max((stepCount - 1) * 2, 1) : stepCount;
        List<SpriteImageSource.AnimationFrame> frames = new ArrayList<>(frameCount);
        for(int index = 0; index < frameCount; index++){
            float percentage = index < stepCount ?
                stepCount > 1 ? (float)index / (stepCount - 1) : 0.5f :
                stepCount > 1 ? 1 - (float)index / (stepCount - 1) : 0.5f;
            int frameTime = (index == 0 && reverse) || index == stepCount - 1 ? data.getFrameTime() + data.getLoopPause() : data.getFrameTime();
            frames.add(SpriteImageSource.AnimationFrame.of(
                Math.round(startX + (endX - startX) * percentage),
                Math.round(startY + (endY - startY) * percentage),
                frameTime
            ));
        }

        // Create the sprite
        output.createSprite()
            .image(SpriteImageSource.animated(
                context.getImage(),
                data.getFrameWidth(), data.getFrameHeight(),
                frames,
                false
            ))
            .submit();

        // Set custom texture data
        output.setCustomData(data);
    }

    @Override
    public ScrollingTextureData deserialize(JsonObject json) throws JsonParseException{
        ScrollingTextureData.Builder builder = ScrollingTextureData.builder();
        if(json.has("from")){
            if(!json.get("from").isJsonPrimitive() || !json.getAsJsonPrimitive("from").isString())
                throw new JsonParseException("Property 'from' must be a string!");
            String fromString = json.get("from").getAsString();
            ScrollingTextureData.Position from;
            try{
                from = ScrollingTextureData.Position.valueOf(fromString.toUpperCase(Locale.ROOT));
            }catch(IllegalArgumentException e){
                throw new JsonParseException("Property 'from' must be one of " + Arrays.toString(ScrollingTextureData.Position.values()).toLowerCase(Locale.ROOT) + ", not '" + fromString + "'!");
            }
            builder.startPosition(from);
        }
        if(json.has("to")){
            if(!json.get("to").isJsonPrimitive() || !json.getAsJsonPrimitive("to").isString())
                throw new JsonParseException("Property 'to' must be a string!");
            String toString = json.get("to").getAsString();
            ScrollingTextureData.Position to;
            try{
                to = ScrollingTextureData.Position.valueOf(toString.toUpperCase(Locale.ROOT));
            }catch(IllegalArgumentException e){
                throw new JsonParseException("Property 'to' must be one of " + Arrays.toString(ScrollingTextureData.Position.values()).toLowerCase(Locale.ROOT) + ", not '" + toString + "'!");
            }
            builder.endPosition(to);
        }
        if(json.has("frame_time")){
            if(!json.get("frame_time").isJsonPrimitive() || !json.getAsJsonPrimitive("frame_time").isNumber())
                throw new JsonParseException("Property 'frame_time' must be an integer!");
            int frameTime = json.get("frame_time").getAsNumber().intValue();
            if(frameTime <= 0)
                throw new JsonParseException("Property 'frame_time' must have a value greater than 0!");
            builder.frameTime(frameTime);
        }
        if(json.has("frame_width")){
            if(!json.get("frame_width").isJsonPrimitive() || !json.getAsJsonPrimitive("frame_width").isNumber())
                throw new JsonParseException("Property 'frame_width' must be an integer!");
            int frameWidth = json.get("frame_width").getAsNumber().intValue();
            if(frameWidth <= 0)
                throw new JsonParseException("Property 'frame_width' must have a value greater than 0!");
            builder.frameWidth(frameWidth);
        }
        if(json.has("frame_height")){
            if(!json.get("frame_height").isJsonPrimitive() || !json.getAsJsonPrimitive("frame_height").isNumber())
                throw new JsonParseException("Property 'frame_height' must be an integer!");
            int frameHeight = json.get("frame_height").getAsNumber().intValue();
            if(frameHeight <= 0)
                throw new JsonParseException("Property 'frame_height' must have a value greater than 0!");
            builder.frameHeight(frameHeight);
        }
        if(json.has("loop_type")){
            if(!json.get("loop_type").isJsonPrimitive() || !json.getAsJsonPrimitive("loop_type").isString())
                throw new JsonParseException("Property 'loop_type' must be a string!");
            String loopTypeString = json.get("loop_type").getAsString();
            ScrollingTextureData.LoopType loopType;
            try{
                loopType = ScrollingTextureData.LoopType.valueOf(loopTypeString.toUpperCase(Locale.ROOT));
            }catch(IllegalArgumentException e){
                throw new JsonParseException("Property 'loop_type' must be one of " + Arrays.toString(ScrollingTextureData.LoopType.values()).toLowerCase(Locale.ROOT) + ", not '" + loopTypeString + "'!");
            }
            builder.loopType(loopType);
        }
        if(json.has("loop_pause")){
            if(!json.get("loop_pause").isJsonPrimitive() || !json.getAsJsonPrimitive("loop_pause").isNumber())
                throw new JsonParseException("Property 'loop_pause' must be an integer!");
            int loopPause = json.get("loop_pause").getAsNumber().intValue();
            if(loopPause < 0)
                throw new JsonParseException("Property 'loop_pause' must have a positive value!");
            builder.loopPause(loopPause);
        }
        return builder.build();
    }

    @Override
    public JsonObject serialize(ScrollingTextureData data){
        JsonObject json = new JsonObject();
        if(data.getStartPosition() != ScrollingTextureData.Position.TOP_LEFT)
            json.addProperty("from", data.getStartPosition().name().toLowerCase(Locale.ROOT));
        if(data.getEndPosition() != ScrollingTextureData.Position.BOTTOM_LEFT)
            json.addProperty("to", data.getEndPosition().name().toLowerCase(Locale.ROOT));
        if(data.getFrameTime() != 10)
            json.addProperty("frame_time", data.getFrameTime());
        if(data.getFrameWidth() != 16)
            json.addProperty("frame_width", data.getFrameWidth());
        if(data.getFrameHeight() != 16)
            json.addProperty("frame_height", data.getFrameHeight());
        if(data.getLoopType() != ScrollingTextureData.LoopType.RESET)
            json.addProperty("loop_type", data.getLoopType().name().toLowerCase(Locale.ROOT));
        if(data.getLoopPause() != 0)
            json.addProperty("loop_pause", data.getLoopPause());
        return json;
    }
}
