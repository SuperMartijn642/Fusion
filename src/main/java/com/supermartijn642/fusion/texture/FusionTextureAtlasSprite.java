package com.supermartijn642.fusion.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import com.supermartijn642.fusion.api.texture.custom.AllocatedSprite;
import com.supermartijn642.fusion.api.texture.custom.SpriteImageSource;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.texture.custom.SpriteImageSourceImpl;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.data.AnimationFrame;
import net.minecraft.client.resources.data.AnimationMetadataSection;

import java.util.*;

/**
 * Created 27/03/2026 by SuperMartijn642
 */
public class FusionTextureAtlasSprite extends TextureAtlasSprite {

    private final SpriteImageSourceImpl imageSource;
    private final int imageWidth, imageHeight;
    private final int frameWidth, frameHeight;
    private final List<SpriteImageSource.AnimationFrame> uniqueFrames;
    private final List<AnimationFrame> frameInfos;

    private int currentFrame;
    private int frameTickCounter;

    public FusionTextureAtlasSprite(AllocatedSprite allocation, AtlasTexture textureAtlas, SpriteImageSourceImpl imageSource, int mipmapLevels){
        super(textureAtlas, new Info(allocation.identifier(), allocation.width(), allocation.height(), AnimationMetadataSection.EMPTY), mipmapLevels, 1, 1, allocation.x(), allocation.y(), imageSource.getImage());
        this.u0 = allocation.u0();
        this.u1 = allocation.u1();
        this.v0 = allocation.v0();
        this.v1 = allocation.v1();
        this.imageSource = imageSource;
        this.imageWidth = imageSource.getImage().getWidth();
        this.imageHeight = imageSource.getImage().getHeight();
        this.frameWidth = imageSource.getFrameWidth();
        this.frameHeight = imageSource.getFrameHeight();
        // Create vanilla frame infos
        Map<Pair<Integer,Integer>,Integer> frameUVs = new HashMap<>();
        List<SpriteImageSource.AnimationFrame> uniqueFrames = new ArrayList<>();
        List<AnimationFrame> frameInfos = new ArrayList<>(imageSource.getFrames().size());
        for(SpriteImageSource.AnimationFrame frame : imageSource.getFrames()){
            int uniqueFrameIndex = frameUVs.computeIfAbsent(Pair.of(frame.u(), frame.v()), p -> {
                uniqueFrames.add(frame);
                return uniqueFrames.size() - 1;
            });
            frameInfos.add(new AnimationFrame(uniqueFrameIndex, frame.time()));
        }
        this.uniqueFrames = Collections.unmodifiableList(uniqueFrames);
        this.frameInfos = Collections.unmodifiableList(frameInfos);
        // Create interpolation data
        if(imageSource.shouldInterpolateFrames())
            this.interpolationData = new InterpolationData(this.info, this.mainImage.length - 1) {
                @Override
                public void uploadInterpolatedFrame(){
                    AnimationFrame currentFrame = FusionTextureAtlasSprite.this.frameInfos.get(FusionTextureAtlasSprite.this.currentFrame);
                    int currentIndex = currentFrame.getIndex();
                    int nextIndex = FusionTextureAtlasSprite.this.frameInfos.get((FusionTextureAtlasSprite.this.currentFrame + 1) % FusionTextureAtlasSprite.this.frameInfos.size()).getIndex();
                    if(currentIndex == nextIndex)
                        return;
                    float progress = (float)FusionTextureAtlasSprite.this.frameTickCounter / currentFrame.getTime();
                    float remainder = 1 - progress;
                    for(int mipLevel = 0; mipLevel < this.activeFrame.length; mipLevel++){
                        int frameWidth = FusionTextureAtlasSprite.this.frameWidth >> mipLevel;
                        int frameHeight = FusionTextureAtlasSprite.this.frameHeight >> mipLevel;
                        for(int y = 0; y < frameHeight; y++){
                            for(int x = 0; x < frameWidth; x++){
                                int currentPixel = this.getPixel(currentIndex, mipLevel, x, y);
                                int nextPixel = this.getPixel(nextIndex, mipLevel, x, y);
                                int alpha = (int)(remainder * (currentPixel >> 24 & 0xff) + progress * (nextPixel >> 24 & 0xff));
                                int red = (int)(remainder * (currentPixel >> 16 & 0xff) + progress * (nextPixel >> 16 & 0xff));
                                int green = (int)(remainder * (currentPixel >> 8 & 0xff) + progress * (nextPixel >> 8 & 0xff));
                                int blue = (int)(remainder * (currentPixel & 0xff) + progress * (nextPixel & 0xff));
                                this.activeFrame[mipLevel].setPixelRGBA(x, y, alpha << 24 | red << 16 | green << 8 | blue);
                            }
                        }
                    }
                    FusionTextureAtlasSprite.this.upload(0, 0, this.activeFrame);
                }

                @Override
                protected int getPixel(int frameIndex, int mipLevel, int x, int y){
                    SpriteImageSource.AnimationFrame frame = FusionTextureAtlasSprite.this.uniqueFrames.get(frameIndex);
                    return FusionTextureAtlasSprite.this.mainImage[mipLevel].getPixelRGBA(x + (frame.u() >> mipLevel), y + (frame.v() >> mipLevel));
                }
            };
    }

    @Override
    public int getPixelRGBA(int frameIndex, int x, int y){
        int uniqueFrameIndex = this.frameInfos.get(0).getIndex();
        SpriteImageSource.AnimationFrame frame = this.uniqueFrames.get(uniqueFrameIndex);
        x = (frame.u() + x) % this.imageWidth;
        y = (frame.v() + y) % this.imageHeight;
        return this.imageSource.getImage().getPixelRGBA(x, y);
    }

    @Override
    public boolean isTransparent(int frameIndex, int x, int y){
        return (this.getPixelRGBA(frameIndex, x, y) >> 24 & 255) == 0;
    }

    @Override
    public void uploadFirstFrame(){
        this.uploadUniqueFrame(0);
    }

    @Override
    public int getFrameCount(){
        return this.uniqueFrames.size();
    }

    @Override
    public boolean isAnimation(){
        return this.uniqueFrames.size() > 1;
    }

    @Override
    public void cycleFrames(){
        this.frameTickCounter++;
        AnimationFrame currentFrame = this.frameInfos.get(this.currentFrame);
        if(this.frameTickCounter >= currentFrame.getTime()){
            this.currentFrame = (this.currentFrame + 1) % this.frameInfos.size();
            this.frameTickCounter = 0;
            AnimationFrame newFrame = this.frameInfos.get(this.currentFrame);
            if(currentFrame.getIndex() != newFrame.getIndex() || this.interpolationData != null)
                this.uploadUniqueFrame(newFrame.getIndex());
        }else if(this.interpolationData != null){
            if(!RenderSystem.isOnRenderThread())
                RenderSystem.recordRenderCall(() -> this.interpolationData.uploadInterpolatedFrame());
            else
                this.interpolationData.uploadInterpolatedFrame();
        }
    }

    private void uploadUniqueFrame(int frameIndex){
        SpriteImageSource.AnimationFrame frame = this.uniqueFrames.get(frameIndex);
        this.uploadToTexture(this.x, this.y, frame.u(), frame.v(), this.frameWidth, this.frameHeight);
    }

    private void uploadToTexture(int destinationX, int destinationY, int sourceX, int sourceY, int width, int height){
        if(sourceX + width > this.imageWidth){
            int overflow = sourceX + width - this.imageWidth;
            this.uploadToTexture(destinationX, destinationY, sourceX, sourceY, width - overflow, height);
            this.uploadToTexture(destinationX + width - overflow, 0, 0, sourceY, overflow, height);
            return;
        }
        if(sourceY + height > this.imageHeight){
            int overflow = sourceY + height - this.imageHeight;
            this.uploadToTexture(destinationX, destinationY, sourceX, sourceY, width, height - overflow);
            this.uploadToTexture(destinationX, destinationY + height - overflow, sourceX, 0, width, overflow);
            return;
        }
        for(int mipLevel = 0; mipLevel < this.mainImage.length; mipLevel++)
            this.mainImage[mipLevel].upload(mipLevel, destinationX >> mipLevel, destinationY >> mipLevel, sourceX >> mipLevel, sourceY >> mipLevel, width >> mipLevel, height >> mipLevel, this.mainImage.length > 1, false);
    }
}
