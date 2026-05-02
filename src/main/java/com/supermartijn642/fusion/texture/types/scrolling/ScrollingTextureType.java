package com.supermartijn642.fusion.texture.types.scrolling;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.platform.Transparency;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.supermartijn642.fusion.api.texture.SpriteCreationContext;
import com.supermartijn642.fusion.api.texture.SpritePreparationContext;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.data.ScrollingTextureData;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.texture.types.base.BaseTextureSprite;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

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
        int stepCount = Math.max(Math.abs(endX - startX), Math.abs(endY - startY)) + 1;
        int frameCount = reverse ? Math.max((stepCount - 1) * 2, 1) : stepCount;
        int[] xPositions = new int[frameCount];
        int[] yPositions = new int[frameCount];
        int[] frameTimes = new int[frameCount];
        for(int index = 0; index < stepCount; index++){
            float percentage = stepCount > 1 ? (float)index / (stepCount - 1) : 0.5f;
            xPositions[index] = Math.round(startX + (endX - startX) * percentage);
            yPositions[index] = Math.round(startY + (endY - startY) * percentage);
            frameTimes[index] = data.getFrameTime();
        }
        frameTimes[stepCount - 1] += data.getLoopPause();
        if(reverse){
            for(int index = 1; index < stepCount - 1; index++){
                float percentage = 1 - (float)index / (stepCount - 1);
                xPositions[index + stepCount - 1] = Math.round(startX + (endX - startX) * percentage);
                yPositions[index + stepCount - 1] = Math.round(startY + (endY - startY) * percentage);
                frameTimes[index + stepCount - 1] = data.getFrameTime();
            }
            frameTimes[0] += data.getLoopPause();
        }

        // Finally create the new sprite
        ScrollingSpriteContents contents = new ScrollingSpriteContents(context.createOriginalSprite().contents(), xPositions, yPositions, frameTimes);
        return new BaseTextureSprite(
            context.getAtlasLocation(),
            contents,
            context.getAtlasWidth(),
            context.getAtlasHeight(),
            context.getSpritePositionX(),
            context.getSpritePositionY(),
            context.getSpritePadding(),
            data
        );
    }

    private static class ScrollingSpriteContents extends SpriteContents {

        private final int[] xPositions, yPositions;
        private final List<FrameInfo> frames;

        public ScrollingSpriteContents(SpriteContents original, int[] xPositions, int[] yPositions, int[] frameTimes){
            super(original.name(), new FrameSize(original.width(), original.height()), original.originalImage);
            this.byMipLevel = original.byMipLevel;
            this.mipmapStrategy = original.mipmapStrategy;
            this.alphaCutoffBias = original.alphaCutoffBias;

            List<Pair<Integer,Integer>> positions = new ArrayList<>();
            List<FrameInfo> frames = new ArrayList<>();
            for(int i = 0; i < frameTimes.length; i++){
                Pair<Integer,Integer> position = Pair.of(xPositions[i], yPositions[i]);
                int frameIndex = positions.indexOf(position);
                if(frameIndex == -1){
                    frameIndex = positions.size();
                    positions.add(position);
                }
                frames.add(new FrameInfo(frameIndex, frameTimes[i]));
            }
            this.xPositions = positions.stream().mapToInt(Pair::left).toArray();
            this.yPositions = positions.stream().mapToInt(Pair::right).toArray();
            this.frames = List.copyOf(frames);
            this.animatedTexture = new ScrollingAnimatedTexture();
        }

        @Override
        public boolean isTransparent(int frame, int x, int y){
            int index = this.frames.get(frame).index();
            return ARGB.alpha(this.originalImage.getPixel(
                this.xPositions[index] + x,
                this.yPositions[index] + y
            )) == 0;
        }

        @Override
        public Transparency computeTransparency(float u0, float v0, float u1, float v1){
            Transparency baseTransparency = this.transparency();
            if(baseTransparency.isOpaque()){
                return baseTransparency;
            }else if(u0 == 0.0f && v0 == 0.0f && u1 == 1.0f && v1 == 1.0f){
                return baseTransparency;
            }else{
                int x0 = Mth.floor(u0 * this.width);
                int y0 = Mth.floor(v0 * this.height);
                int x1 = Mth.ceil(u1 * this.width);
                int y1 = Mth.ceil(v1 * this.height);
                IntList uniqueFrames = this.animatedTexture.getUniqueFrames();
                Transparency transparency = Transparency.NONE;
                for(int i = 0; i < uniqueFrames.size(); i++){
                    int frame = uniqueFrames.getInt(i);
                    int frameX = this.xPositions[frame] * this.width;
                    int frameY = this.yPositions[frame] * this.height;
                    transparency = transparency.or(this.originalImage.computeTransparency(frameX + x0, frameY + y0, frameX + x1, frameY + y1));
                }
                return transparency;
            }
        }

        private class ScrollingAnimatedTexture extends SpriteContents.AnimatedTexture {

            public ScrollingAnimatedTexture(){
                super(ScrollingSpriteContents.this.frames, 1, false);
            }

            @Override
            public AnimationState createAnimationState(GpuBufferSlice bufferSlice, int offset){
                GpuDevice gpuDevice = RenderSystem.getDevice();
                Int2ObjectMap<GpuTextureView> textureViews = new Int2ObjectOpenHashMap<>();
                GpuBufferSlice[] slices = new GpuBufferSlice[ScrollingSpriteContents.this.byMipLevel.length];

                for(int frameIndex : this.getUniqueFrames().toIntArray()){
                    GpuTexture gpuTexture = gpuDevice.createTexture(
                        () -> ScrollingSpriteContents.this.name + " animation frame " + frameIndex,
                        5,
                        TextureFormat.RGBA8,
                        ScrollingSpriteContents.this.width,
                        ScrollingSpriteContents.this.height,
                        1,
                        ScrollingSpriteContents.this.byMipLevel.length + 1
                    );
                    int x = ScrollingSpriteContents.this.xPositions[frameIndex];
                    int y = ScrollingSpriteContents.this.yPositions[frameIndex];

                    for(int m = 0; m < ScrollingSpriteContents.this.byMipLevel.length; m++){
                        RenderSystem.getDevice()
                            .createCommandEncoder()
                            .writeToTexture(
                                gpuTexture, ScrollingSpriteContents.this.byMipLevel[m], m, 0, 0, 0, ScrollingSpriteContents.this.width >> m, ScrollingSpriteContents.this.height >> m, x >> m, y >> m
                            );
                    }

                    textureViews.put(frameIndex, RenderSystem.getDevice().createTextureView(gpuTexture));
                }

                for(int level = 0; level < ScrollingSpriteContents.this.byMipLevel.length; level++)
                    //noinspection IntegerMultiplicationImplicitCastToLong
                    slices[level] = bufferSlice.slice(level * offset, offset);

                return new AnimationState(this, textureViews, slices);
            }
        }
    }
}
