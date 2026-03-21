package com.supermartijn642.fusion.texture;

import com.supermartijn642.fusion.api.texture.custom.AllocatedSprite;
import com.supermartijn642.fusion.api.texture.custom.SpriteImageSource;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.texture.custom.SpriteImageSourceImpl;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.data.AnimationFrame;

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
        super(allocation.identifier(), allocation.width(), allocation.height());
        this.x = allocation.x();
        this.y = allocation.y();
        this.u0 = allocation.u0();
        this.u1 = allocation.u1();
        this.v0 = allocation.v0();
        this.v1 = allocation.v1();
        this.mainImage = new NativeImage[mipmapLevels];
        this.mainImage[0] = imageSource.getImage();
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
            if(currentFrame.getIndex() != newFrame.getIndex())
                this.uploadUniqueFrame(newFrame.getIndex());
        }else if(this.imageSource.shouldInterpolateFrames())
            this.uploadInterpolatedFrame();
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
        }
        for(int mipLevel = 0; mipLevel < this.mainImage.length; mipLevel++)
            this.mainImage[mipLevel].upload(mipLevel, destinationX >> mipLevel, destinationY >> mipLevel, sourceX >> mipLevel, sourceY >> mipLevel, width >> mipLevel, height >> mipLevel, this.mainImage.length > 1);
    }

    @Override
    protected void uploadInterpolatedFrame(){
        AnimationFrame currentFrame = this.frameInfos.get(this.currentFrame);
        double transitionPercentage = 1 - (double)this.frameTickCounter / currentFrame.getTime();
        int currentFrameIndex = currentFrame.getIndex();
        int nextFrameIndex = this.frameInfos.get((this.currentFrame + 1) % this.frameInfos.size()).getIndex();
        if(currentFrameIndex != nextFrameIndex){
            if(this.activeFrame == null || this.activeFrame.length != this.mainImage.length){
                if(this.activeFrame != null){
                    for(NativeImage nativeimage : this.activeFrame){
                        if(nativeimage != null)
                            nativeimage.close();
                    }
                }

                this.activeFrame = new NativeImage[this.mainImage.length];
            }

            for(int mipLevel = 0; mipLevel < this.mainImage.length; ++mipLevel){
                int width = this.width >> mipLevel;
                int height = this.height >> mipLevel;
                if(this.activeFrame[mipLevel] == null)
                    //noinspection resource
                    this.activeFrame[mipLevel] = new NativeImage(width, height, false);

                for(int y = 0; y < height; ++y){
                    for(int x = 0; x < width; ++x){
                        int currentPixel = this.getPixel(currentFrameIndex, mipLevel, x, y);
                        int nextPixel = this.getPixel(nextFrameIndex, mipLevel, x, y);
                        int blue = this.mix(transitionPercentage, currentPixel >> 16 & 255, nextPixel >> 16 & 255);
                        int green = this.mix(transitionPercentage, currentPixel >> 8 & 255, nextPixel >> 8 & 255);
                        int red = this.mix(transitionPercentage, currentPixel & 255, nextPixel & 255);
                        this.activeFrame[mipLevel].setPixelRGBA(x, y, currentPixel & -16777216 | blue << 16 | green << 8 | red);
                    }
                }
            }
            this.upload(0, 0, this.activeFrame);
        }
    }
}
