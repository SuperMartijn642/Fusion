package com.supermartijn642.fusion.texture;

import com.supermartijn642.fusion.api.texture.custom.AllocatedSprite;
import com.supermartijn642.fusion.api.texture.custom.SpriteImageSource;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.texture.custom.SpriteImageSourceImpl;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.data.AnimationFrame;

import java.util.*;

/**
 * Created 27/03/2026 by SuperMartijn642
 */
public class FusionTextureAtlasSprite extends TextureAtlasSprite {

    private final SpriteImageSourceImpl imageSource;
    private final int imageWidth, imageHeight;
    private final int frameWidth, frameHeight;
    private final List<int[][]> uniqueFrames;
    private final List<AnimationFrame> frameInfos;

    private int currentFrame;
    private int frameTickCounter;

    public FusionTextureAtlasSprite(AllocatedSprite allocation, SpriteImageSourceImpl imageSource, int mipmapLevels){
        super(allocation.identifier().toString());
        this.originX = allocation.x();
        this.originY = allocation.y();
        this.rotated = false;
        this.minU = allocation.u0();
        this.maxU = allocation.u1();
        this.minV = allocation.v0();
        this.maxV = allocation.v1();
        this.width = allocation.width();
        this.height = allocation.height();
        this.imageSource = imageSource;
        this.imageWidth = imageSource.getImage().getWidth();
        this.imageHeight = imageSource.getImage().getHeight();
        this.frameWidth = imageSource.getFrameWidth();
        this.frameHeight = imageSource.getFrameHeight();
        // Create vanilla frame infos
        Map<Pair<Integer,Integer>,Integer> frameUVs = new HashMap<>();
        List<SpriteImageSource.AnimationFrame> uniqueFrameUVs = new ArrayList<>();
        List<AnimationFrame> frameInfos = new ArrayList<>(imageSource.getFrames().size());
        for(SpriteImageSource.AnimationFrame frame : imageSource.getFrames()){
            int uniqueFrameIndex = frameUVs.computeIfAbsent(Pair.of(frame.u(), frame.v()), p -> {
                uniqueFrameUVs.add(frame);
                return uniqueFrameUVs.size() - 1;
            });
            frameInfos.add(new AnimationFrame(uniqueFrameIndex, frame.time()));
        }
        this.frameInfos = Collections.unmodifiableList(frameInfos);
        // Copy the data for each unique frame
        int[] original = new int[this.imageWidth * this.imageHeight];
        imageSource.getImage().getRGB(0, 0, this.imageWidth, this.imageHeight, original, 0, this.imageWidth);
        List<int[][]> uniqueFrames = new ArrayList<>();
        for(SpriteImageSource.AnimationFrame uv : uniqueFrameUVs){
            int[][] frame = new int[mipmapLevels + 1][];
            frame[0] = this.extractFrame(original, this.imageWidth, uv.u(), uv.v(), this.frameWidth, this.frameHeight);
            uniqueFrames.add(frame);
        }
        this.uniqueFrames = Collections.unmodifiableList(uniqueFrames);
    }

    @Override
    public int[][] getFrameTextureData(int index){
        return this.uniqueFrames.get(index);
    }

    @Override
    public void generateMipmaps(int mipmapLevels){
        this.setFramesTextureData(this.uniqueFrames);
        super.generateMipmaps(mipmapLevels);
        for(int i = 0; i < this.uniqueFrames.size(); i++){
            int[][] frame = this.uniqueFrames.get(i);
            int[][] mipmappedFrame = super.getFrameTextureData(i);
            System.arraycopy(mipmappedFrame, 1, frame, 1, frame.length - 1);
        }
    }

    @Override
    public int getFrameCount(){
        return this.uniqueFrames.size();
    }

    @Override
    public boolean hasAnimationMetadata(){
        return this.uniqueFrames.size() > 1;
    }

    @Override
    public void updateAnimation(){
        this.frameTickCounter++;
        AnimationFrame currentFrame = this.frameInfos.get(this.currentFrame);
        if(this.frameTickCounter >= currentFrame.getFrameTime()){
            this.currentFrame = (this.currentFrame + 1) % this.frameInfos.size();
            this.frameTickCounter = 0;
            AnimationFrame newFrame = this.frameInfos.get(this.currentFrame);
            if(currentFrame.getFrameIndex() != newFrame.getFrameIndex())
                this.uploadUniqueFrame(newFrame.getFrameIndex());
        }else if(this.imageSource.shouldInterpolateFrames())
            this.updateAnimationInterpolated();
    }

    private void uploadUniqueFrame(int frameIndex){
        int[][] frame = this.uniqueFrames.get(frameIndex);
        TextureUtil.uploadTextureMipmap(frame, this.frameWidth, this.frameHeight, this.originX, this.originY, false, false);
    }

    @Override
    protected void updateAnimationInterpolated(){
        AnimationFrame currentFrame = this.frameInfos.get(this.currentFrame);
        double transitionPercentage = 1 - (double)this.frameTickCounter / currentFrame.getFrameTime();
        int currentFrameIndex = currentFrame.getFrameIndex();
        int nextFrameIndex = this.frameInfos.get((this.currentFrame + 1) % this.frameInfos.size()).getFrameIndex();
        if(currentFrameIndex != nextFrameIndex){
            int[][] currentFramePixels = this.uniqueFrames.get(currentFrameIndex);
            int[][] nextFramePixels = this.uniqueFrames.get(nextFrameIndex);

            if(this.interpolatedFrameData == null || this.interpolatedFrameData.length != currentFramePixels.length)
                this.interpolatedFrameData = new int[currentFramePixels.length][];

            for(int mipLevel = 0; mipLevel < currentFramePixels.length; ++mipLevel){
                if(this.interpolatedFrameData[mipLevel] == null)
                    this.interpolatedFrameData[mipLevel] = new int[currentFramePixels[mipLevel].length];
                for(int i = 0; i < currentFramePixels[mipLevel].length; i++){
                    int currentPixel = currentFramePixels[mipLevel][i];
                    int nextPixel = nextFramePixels[mipLevel][i];
                    int blue = this.interpolateColor(transitionPercentage, currentPixel >> 16 & 255, nextPixel >> 16 & 255);
                    int green = this.interpolateColor(transitionPercentage, currentPixel >> 8 & 255, nextPixel >> 8 & 255);
                    int red = this.interpolateColor(transitionPercentage, currentPixel & 255, nextPixel & 255);
                    this.interpolatedFrameData[mipLevel][i] = currentPixel & -16777216 | blue << 16 | green << 8 | red;
                }
            }
            TextureUtil.uploadTextureMipmap(this.interpolatedFrameData, this.frameWidth, this.frameHeight, this.originX, this.originY, false, false);
        }
    }

    private int[] extractFrame(int[] source, int sourceWidth, int x, int y, int width, int height){
        int[] frame = new int[width * height];
        this.extractFrame(source, sourceWidth, frame, width, 0, 0, x, y, width, height);
        return frame;
    }

    private void extractFrame(int[] source, int sourceWidth, int[] destination, int destinationWidth, int destinationX, int destinationY, int sourceX, int sourceY, int width, int height){
        if(sourceX + width > sourceWidth){
            int overflow = sourceX + width - sourceWidth;
            this.extractFrame(source, sourceWidth, destination, destinationWidth, destinationX, destinationY, sourceX, sourceY, width - overflow, height);
            this.extractFrame(source, sourceWidth, destination, destinationWidth, destinationX + width - overflow, 0, 0, sourceY, overflow, height);
            return;
        }
        if(sourceY + height > source.length / sourceWidth){
            int overflow = sourceY + height - source.length / sourceWidth;
            this.extractFrame(source, sourceWidth, destination, destinationWidth, destinationX, destinationY, sourceX, sourceY, width, height - overflow);
            this.extractFrame(source, sourceWidth, destination, destinationWidth, destinationX, destinationY + height - overflow, sourceX, 0, width, overflow);
            return;
        }

        for(int line = 0; line < height; line++)
            System.arraycopy(source, sourceX + (sourceY + line) * sourceWidth, destination, destinationX + (destinationY + line) * destinationWidth, width);
    }
}
