package com.supermartijn642.fusion.texture.types.scrolling;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.texture.SpriteCreationContext;
import com.supermartijn642.fusion.api.texture.SpritePreparationContext;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.data.ScrollingTextureData;
import com.supermartijn642.fusion.api.util.Pair;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Created 28/04/2023 by SuperMartijn642
 */
public class ScrollingTextureType implements TextureType<ScrollingTextureData> {

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

    @Override
    public Pair<Integer,Integer> getFrameSize(SpritePreparationContext context, ScrollingTextureData data){
        if(context.getTextureWidth() < data.getFrameWidth() || context.getTextureHeight() < data.getFrameHeight())
            throw new RuntimeException("Frame size must be smaller than the texture size!");
        return Pair.of(data.getFrameWidth(), data.getFrameHeight());
    }

    @Override
    public TextureAtlasSprite createSprite(SpriteCreationContext context, ScrollingTextureData data){
        // Calculate frame start and end
        boolean reverse = data.getLoopType() == ScrollingTextureData.LoopType.REVERSE;
        int startX = data.getStartPosition() == ScrollingTextureData.Position.TOP_LEFT || data.getStartPosition() == ScrollingTextureData.Position.BOTTOM_LEFT ? 0 : context.getTextureWidth() - data.getFrameWidth();
        int startY = data.getStartPosition() == ScrollingTextureData.Position.TOP_LEFT || data.getStartPosition() == ScrollingTextureData.Position.TOP_RIGHT ? 0 : context.getTextureHeight() - data.getFrameHeight();
        int endX = data.getEndPosition() == ScrollingTextureData.Position.TOP_LEFT || data.getEndPosition() == ScrollingTextureData.Position.BOTTOM_LEFT ? 0 : context.getTextureWidth() - data.getFrameWidth();
        int endY = data.getEndPosition() == ScrollingTextureData.Position.TOP_LEFT || data.getEndPosition() == ScrollingTextureData.Position.TOP_RIGHT ? 0 : context.getTextureHeight() - data.getFrameHeight();

        // Calculate all the frames
        int frameCount = Math.max(Math.abs(endX - startX), Math.abs(endY - startY)) + 1;
        int[] xPositions = new int[reverse ? (frameCount - 1) * 2 : frameCount];
        int[] yPositions = new int[reverse ? (frameCount - 1) * 2 : frameCount];
        int[] frameTimes = new int[reverse ? (frameCount - 1) * 2 : frameCount];
        for(int index = 0; index < frameCount; index++){
            float percentage = frameCount > 1 ? (float)index / (frameCount - 1) : 0.5f;
            xPositions[index] = Math.round(startX + (endX - startX) * percentage);
            yPositions[index] = Math.round(startY + (endY - startY) * percentage);
            frameTimes[index] = data.getFrameTime();
        }
        frameTimes[frameCount - 1] += data.getLoopPause();
        if(reverse){
            for(int index = 1; index < frameCount - 1; index++){
                float percentage = 1 - (float)index / (frameCount - 1);
                xPositions[index + frameCount - 1] = Math.round(startX + (endX - startX) * percentage);
                yPositions[index + frameCount - 1] = Math.round(startY + (endY - startY) * percentage);
                frameTimes[index + frameCount - 1] = data.getFrameTime();
            }
            frameTimes[0] += data.getLoopPause();
        }

        // Finally create the new sprite
        return new ScrollingSprite(
            context.getTextureIdentifier(),
            context.getSpriteWidth(),
            context.getSpriteHeight(),
            context.getAtlasWidth(),
            context.getAtlasHeight(),
            context.getSpritePositionX(),
            context.getSpritePositionY(),
            context.getTextureWidth(),
            context.getTextureHeight(),
            context.getTextureBuffers(),
            xPositions,
            yPositions,
            frameTimes
        );
    }

    private static class ScrollingSprite extends TextureAtlasSprite {

        private final int[] xPositions, yPositions;
        private final int[] frameTimes;
        private int frame, tickCounter;

        protected ScrollingSprite(ResourceLocation identifier, int width, int height, int atlasWidth, int atlasHeight, int atlasX, int atlasY, int imageWidth, int imageHeight, List<int[][]> mainImage, int[] xPositions, int[] yPositions, int[] frameTimes){
            super(identifier.toString());
            this.width = width;
            this.height = height;
            this.originX = atlasX;
            this.originY = atlasY;
            this.minU = (float)atlasX / atlasWidth;
            this.maxU = (float)(atlasX + width) / atlasWidth;
            this.minV = (float)atlasY / atlasHeight;
            this.maxV = (float)(atlasY + height) / atlasHeight;
            this.xPositions = xPositions;
            this.yPositions = yPositions;
            this.frameTimes = frameTimes;
            this.framesTextureData = mainImage;
            this.splitAnimationFrames(imageWidth, imageHeight);
        }

        @Override
        public void initSprite(int inX, int inY, int originInX, int originInY, boolean rotatedIn){
            super.initSprite(inX, inY, originInX, originInY, rotatedIn);
        }

        private void splitAnimationFrames(int imageWidth, int imageHeight){
            // Generate the frames
            int[] image = this.framesTextureData.get(0)[0];
            int mipmaps = this.framesTextureData.get(0).length - 1;
            List<int[][]> frames = new ArrayList<>();
            for(int frame = 0; frame < this.xPositions.length; frame++){
                int[] frameData = new int[this.width * this.height];
                for(int row = 0; row < this.height; row++){
                    int position = (this.yPositions[frame] + row) * imageWidth + this.xPositions[frame];
                    System.arraycopy(image, position, frameData, row * this.width, this.width);
                }
                int[][] frameMipmaps = new int[mipmaps + 1][];
                frameMipmaps[0] = frameData;
                frames.add(frameMipmaps);
            }

            // Generate mipmaps
            this.framesTextureData = frames;
            if(mipmaps > 0)
                this.generateMipmaps(mipmaps);
        }

        @Override
        public void updateAnimation(){
            if(++this.tickCounter >= this.frameTimes[this.frame]){
                this.frame = (this.frame + 1) % this.xPositions.length;
                this.tickCounter = 0;
                this.uploadFrame(this.frame);
            }
        }

        private void uploadFrame(int frame){
            TextureUtil.uploadTextureMipmap(this.framesTextureData.get(frame), this.width, this.height, this.originX, this.originY, false, false);
        }

        @Override
        public boolean hasAnimationMetadata(){
            return true;
        }
    }
}
