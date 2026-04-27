package com.supermartijn642.fusion.texture;

import com.supermartijn642.fusion.api.texture.custom.SpriteImageSource;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.texture.custom.SpriteImageSourceImpl;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteTicker;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
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

    public FusionSpriteContents(ResourceLocation identifier, SpriteImageSourceImpl imageSource){
        super(identifier, new FrameSize(imageSource.getFrameWidth(), imageSource.getFrameHeight()), imageSource.getImage(), AnimationMetadataSection.EMPTY);
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
            this.animatedTexture = this.createAnimatedTexture();
    }

    @Override
    public boolean isTransparent(int uniqueFrameIndex, int x, int y){
        SpriteImageSource.AnimationFrame frame = this.uniqueFrames.get(0);
        x = (frame.u() + x) % this.imageWidth;
        y = (frame.v() + y) % this.imageHeight;
        return (this.imageSource.getImage().getPixelRGBA(x, y) >> 24 & 255) == 0;
    }

    @Override
    public void uploadFirstFrame(int destinationX, int destinationY){
        this.uploadUniqueFrame(destinationX, destinationY, 0);
    }

    @Override
    public IntStream getUniqueFrames(){
        return IntStream.range(0, this.uniqueFrames.size());
    }

    @Override
    protected @Nullable AnimatedTexture createAnimatedTexture(FrameSize frameSize, int imageWidth, int imageHeight, AnimationMetadataSection animationMetadataSection){
        return null;
    }

    private AnimatedTexture createAnimatedTexture(){
        return new AnimatedTexture(this.frameInfos, 0, this.imageSource.shouldInterpolateFrames()) {
            @Override
            protected void uploadFrame(int destinationX, int destinationY, int frame){
                int uniqueFrame = this.frames.get(frame).index;
                FusionSpriteContents.this.uploadUniqueFrame(destinationX, destinationY, uniqueFrame);
            }

            @Override
            public SpriteTicker createTicker(){
                if(!this.interpolateFrames)
                    return super.createTicker();
                InterpolationData interpolationData = new InterpolationData() {
                    @Override
                    protected int getPixel(AnimatedTexture animatedTexture, int frameIndex, int mipLevel, int x, int y){
                        SpriteImageSource.AnimationFrame frame = FusionSpriteContents.this.uniqueFrames.get(frameIndex);
                        return FusionSpriteContents.this.byMipLevel[mipLevel].getPixelRGBA(x + (frame.u() >> mipLevel), y + (frame.v() >> mipLevel));
                    }
                };
                return new Ticker(this, interpolationData);
            }
        };
    }

    private void uploadUniqueFrame(int destinationX, int destinationY, int frameIndex){
        SpriteImageSource.AnimationFrame frame = this.uniqueFrames.get(frameIndex);
        this.uploadToTexture(destinationX, destinationY, frame.u(), frame.v(), this.frameWidth, this.frameHeight);
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
        for(int mipLevel = 0; mipLevel < this.byMipLevel.length; mipLevel++)
            this.byMipLevel[mipLevel].upload(mipLevel, destinationX >> mipLevel, destinationY >> mipLevel, sourceX >> mipLevel, sourceY >> mipLevel, width >> mipLevel, height >> mipLevel, this.byMipLevel.length > 1, false);
    }
}
