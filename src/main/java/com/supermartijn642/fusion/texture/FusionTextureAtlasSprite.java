package com.supermartijn642.fusion.texture;

import com.supermartijn642.fusion.api.texture.custom.AllocatedSprite;
import com.supermartijn642.fusion.api.texture.custom.SpriteImageSource;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.texture.custom.SpriteImageSourceImpl;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Created 27/03/2026 by SuperMartijn642
 */
public class FusionTextureAtlasSprite extends TextureAtlasSprite {

    private final SpriteImageSourceImpl imageSource;
    private final int imageWidth, imageHeight;
    private final int frameWidth, frameHeight;
    private final List<SpriteImageSource.AnimationFrame> uniqueFrames;
    private final List<FrameInfo> frameInfos;

    public FusionTextureAtlasSprite(AllocatedSprite allocation, TextureAtlas textureAtlas, SpriteImageSourceImpl imageSource, int mipmapLevels){
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
            this.animatedTexture = this.createAnimatedTexture(new Info(allocation.identifier(), allocation.width(), allocation.height(), AnimationMetadataSection.EMPTY));
    }

    @Override
    public boolean isTransparent(int uniqueFrameIndex, int x, int y){
        SpriteImageSource.AnimationFrame frame = this.uniqueFrames.get(0);
        x = (frame.u() + x) % this.imageWidth;
        y = (frame.v() + y) % this.imageHeight;
        return (this.imageSource.getImage().getPixelRGBA(x, y) >> 24 & 255) == 0;
    }

    @Override
    public void uploadFirstFrame(){
        this.uploadUniqueFrame(0);
    }

    @Override
    public IntStream getUniqueFrames(){
        return IntStream.range(0, this.uniqueFrames.size());
    }

    @Override
    protected TextureAtlasSprite.AnimatedTexture createTicker(TextureAtlasSprite.Info info, int imageWidth, int imageHeight, int mipmapLevels){
        return null;
    }

    private AnimatedTexture createAnimatedTexture(Info info){
        InterpolationData interpolationData = this.imageSource.shouldInterpolateFrames() ?
            new InterpolationData(info, this.mainImage.length - 1) {
                @Override
                protected void uploadInterpolatedFrame(TextureAtlasSprite.AnimatedTexture animatedTexture) {
                    // We have to copy the vanilla code here because of Sodium's mixin that overwrites it
                    FrameInfo currentFrame = FusionTextureAtlasSprite.this.frameInfos.get(animatedTexture.frame);
                    int currentIndex = currentFrame.index;
                    int nextIndex = FusionTextureAtlasSprite.this.frameInfos.get((animatedTexture.frame + 1) % FusionTextureAtlasSprite.this.frameInfos.size()).index;
                    if(currentIndex == nextIndex)
                        return;
                    float progress = (float)animatedTexture.subFrame / currentFrame.time;
                    float remainder = 1 - progress;
                    for(int mipLevel = 0; mipLevel < this.activeFrame.length; mipLevel++){
                        int frameWidth = FusionTextureAtlasSprite.this.frameWidth >> mipLevel;
                        int frameHeight = FusionTextureAtlasSprite.this.frameHeight >> mipLevel;
                        for(int y = 0; y < frameHeight; y++){
                            for(int x = 0; x < frameWidth; x++){
                                int currentPixel = this.getPixel(null, currentIndex, mipLevel, x, y);
                                int nextPixel = this.getPixel(null, nextIndex, mipLevel, x, y);
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
                protected int getPixel(AnimatedTexture animatedTexture, int frameIndex, int mipLevel, int x, int y){
                    SpriteImageSource.AnimationFrame frame = FusionTextureAtlasSprite.this.uniqueFrames.get(frameIndex);
                    return FusionTextureAtlasSprite.this.mainImage[mipLevel].getPixelRGBA(x + (frame.u() >> mipLevel), y + (frame.v() >> mipLevel));
                }
            } : null;
        return new AnimatedTexture(this.frameInfos, 0, interpolationData) {
            @Override
            protected void uploadFrame(int frame){
                int uniqueFrame = FusionTextureAtlasSprite.this.frameInfos.get(frame).index;
                FusionTextureAtlasSprite.this.uploadUniqueFrame(uniqueFrame);
            }
        };
    }

    private void uploadUniqueFrame(int frameIndex){
        SpriteImageSource.AnimationFrame frame = this.uniqueFrames.get(frameIndex);
        this.uploadToTexture(this.getX(), this.getY(), frame.u(), frame.v(), this.frameWidth, this.frameHeight);
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
