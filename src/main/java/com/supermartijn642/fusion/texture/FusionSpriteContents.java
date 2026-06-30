package com.supermartijn642.fusion.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.supermartijn642.fusion.api.texture.custom.SpriteImageSource;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.texture.custom.SpriteImageSourceImpl;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteTicker;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Created 27/03/2026 by SuperMartijn642
 */
public class FusionSpriteContents extends SpriteContents {

    private final SpriteImageSourceImpl imageSource;
    private final int imageWidth, imageHeight;
    private final int frameWidth, frameHeight;
    private final List<SpriteImageSource.AnimationFrame> uniqueFrames;
    private final List<FrameInfo> frameInfos;

    public FusionSpriteContents(ResourceLocation identifier, SpriteImageSourceImpl imageSource, ResourceMetadata resourceMetadata){
        super(identifier, new FrameSize(imageSource.getFrameWidth(), imageSource.getFrameHeight()), imageSource.getImage(), ResourceMetadata.EMPTY);
        this.metadata = resourceMetadata;
        this.imageSource = imageSource;
        this.imageWidth = imageSource.getImage().getWidth();
        this.imageHeight = imageSource.getImage().getHeight();
        this.frameWidth = imageSource.getFrameWidth();
        this.frameHeight = imageSource.getFrameHeight();
        // Create vanilla frame infos
        Map<Pair<Integer,Integer>,Integer> frameUVs = new HashMap<>();
        List<SpriteImageSource.AnimationFrame> uniqueFrames = new ArrayList<>();
        List<FrameInfo> frameInfos = new ArrayList<>(imageSource.getFrames().size());
        for(SpriteImageSource.AnimationFrame frame : imageSource.getFrames()){
            int uniqueFrameIndex = frameUVs.computeIfAbsent(Pair.of(frame.u(), frame.v()), p -> {
                uniqueFrames.add(frame);
                return uniqueFrames.size() - 1;
            });
            frameInfos.add(new FrameInfo(uniqueFrameIndex, frame.time()));
        }
        this.uniqueFrames = List.copyOf(uniqueFrames);
        this.frameInfos = List.copyOf(frameInfos);
        // Create vanilla animation data
        if(uniqueFrames.size() > 1)
            this.animatedTexture = this.createAnimatedTexture(null, 0, 0, null);
    }

    @Override
    public boolean isTransparent(int uniqueFrameIndex, int x, int y){
        SpriteImageSource.AnimationFrame frame = this.uniqueFrames.get(0);
        x = (frame.u() + x) % this.imageWidth;
        y = (frame.v() + y) % this.imageHeight;
        return ARGB.alpha(this.imageSource.getImage().getPixel(x, y)) == 0;
    }

    @Override
    public void uploadFirstFrame(int destinationX, int destinationY, GpuTexture gpuTexture){
        this.uploadUniqueFrame(destinationX, destinationY, gpuTexture, 0);
    }

    @Override
    public IntStream getUniqueFrames(){
        return IntStream.range(0, this.uniqueFrames.size());
    }

    @Override
    protected @Nullable AnimatedTexture createAnimatedTexture(FrameSize frameSize, int imageWidth, int imageHeight, AnimationMetadataSection animationMetadataSection){
        return new AnimatedTexture(this.frameInfos, 0, this.imageSource.shouldInterpolateFrames()) {
            @Override
            protected void uploadFrame(int destinationX, int destinationY, int frame, GpuTexture destination){
                int uniqueFrame = this.frames.get(frame).index();
                FusionSpriteContents.this.uploadUniqueFrame(destinationX, destinationY, destination, uniqueFrame);
            }

            @Override
            public SpriteTicker createTicker(){
                if(!this.interpolateFrames)
                    return super.createTicker();
                class FusionInterpolationData extends InterpolationData implements SodiumBypassingInterpolationData {
                    @Override
                    protected void uploadInterpolatedFrame(int atlasX, int atlasY, SpriteContents.Ticker ticker, GpuTexture atlasTexture){
                        // We have to copy the vanilla code here because of Sodium's mixin that overwrites it
                        FrameInfo currentFrame = FusionSpriteContents.this.frameInfos.get(ticker.frame);
                        int currentIndex = currentFrame.index();
                        int nextIndex = FusionSpriteContents.this.frameInfos.get((ticker.frame + 1) % FusionSpriteContents.this.frameInfos.size()).index();
                        if(currentIndex == nextIndex)
                            return;
                        float progress = (float)ticker.subFrame / currentFrame.time();
                        for(int mipLevel = 0; mipLevel < this.activeFrame.length; mipLevel++){
                            int frameWidth = FusionSpriteContents.this.frameWidth >> mipLevel;
                            int frameHeight = FusionSpriteContents.this.frameHeight >> mipLevel;
                            for(int y = 0; y < frameHeight; y++){
                                for(int x = 0; x < frameWidth; x++){
                                    int currentPixel = this.getPixel(null, currentIndex, mipLevel, x, y);
                                    int nextPixel = this.getPixel(null, nextIndex, mipLevel, x, y);
                                    this.activeFrame[mipLevel].setPixel(x, y, ARGB.lerp(progress, currentPixel, nextPixel));
                                }
                            }
                        }

                        FusionSpriteContents.this.upload(atlasX, atlasY, 0, 0, this.activeFrame, atlasTexture);
                    }

                    @Override
                    protected int getPixel(@Nullable AnimatedTexture animatedTexture, int frameIndex, int mipLevel, int x, int y){
                        SpriteImageSource.AnimationFrame frame = FusionSpriteContents.this.uniqueFrames.get(frameIndex);
                        x = (x + (frame.u() >> mipLevel)) % FusionSpriteContents.this.imageWidth;
                        y = (y + (frame.v() >> mipLevel)) % FusionSpriteContents.this.imageHeight;
                        return FusionSpriteContents.this.byMipLevel[mipLevel].getPixel(x, y);
                    }

                    @Override
                    public void bypassSodiumUploadInterpolatedFrameOverwrite(int atlasX, int atlasY, SpriteContents.Ticker ticker, GpuTexture atlasTexture){
                        this.uploadInterpolatedFrame(atlasX, atlasY, ticker, atlasTexture);
                    }
                }
                return new Ticker(this, new FusionInterpolationData());
            }
        };
    }

    private void uploadUniqueFrame(int destinationX, int destinationY, GpuTexture destination, int frameIndex){
        SpriteImageSource.AnimationFrame frame = this.uniqueFrames.get(frameIndex);
        this.uploadToTexture(destination, destinationX, destinationY, frame.u(), frame.v(), this.frameWidth, this.frameHeight);
    }

    private void uploadToTexture(GpuTexture destination, int destinationX, int destinationY, int sourceX, int sourceY, int width, int height){
        if(sourceX + width > this.imageWidth){
            int overflow = sourceX + width - this.imageWidth;
            this.uploadToTexture(destination, destinationX, destinationY, sourceX, sourceY, width - overflow, height);
            this.uploadToTexture(destination, destinationX + width - overflow, 0, 0, sourceY, overflow, height);
            return;
        }
        if(sourceY + height > this.imageHeight){
            int overflow = sourceY + height - this.imageHeight;
            this.uploadToTexture(destination, destinationX, destinationY, sourceX, sourceY, width, height - overflow);
            this.uploadToTexture(destination, destinationX, destinationY + height - overflow, sourceX, 0, width, overflow);
            return;
        }
        for(int mipLevel = 0; mipLevel < this.byMipLevel.length; mipLevel++)
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(destination, this.byMipLevel[mipLevel], mipLevel, 0, destinationX >> mipLevel, destinationY >> mipLevel, width >> mipLevel, height >> mipLevel, sourceX >> mipLevel, sourceY >> mipLevel);
    }

    public interface SodiumBypassingInterpolationData {
        void bypassSodiumUploadInterpolatedFrameOverwrite(int atlasX, int atlasY, SpriteContents.Ticker ticker, GpuTexture atlasTexture);
    }
}
