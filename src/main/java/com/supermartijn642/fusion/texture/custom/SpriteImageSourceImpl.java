package com.supermartijn642.fusion.texture.custom;

import com.mojang.blaze3d.platform.NativeImage;
import com.supermartijn642.fusion.api.texture.custom.SpriteImageSource;
import com.supermartijn642.fusion.api.util.UserErrorException;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Created 22/03/2026 by SuperMartijn642
 */
public class SpriteImageSourceImpl implements SpriteImageSource {

    public static SpriteImageSource constant(NativeImage image){
        return animated(
            image,
            image.getWidth(),
            image.getHeight(),
            List.of(AnimationFrame.of(0, 0, 1)),
            false
        );
    }

    public static SpriteImageSource animated(NativeImage image, int frameWidth, int frameHeight, List<AnimationFrame> frames, boolean interpolateFrames){
        // Wrap uvs to be within the image bounds
        frames = new ArrayList<>(frames);
        for(int i = 0; i < frames.size(); i++){
            AnimationFrame frame = frames.get(i);
            if(frame.u() < 0 || frame.u() >= image.getWidth() || frame.v() < 0 || frame.v() >= image.getHeight()){
                frames.set(i, new Frame(
                    (frame.u() % image.getWidth() + image.getWidth()) % image.getWidth(),
                    (frame.v() % image.getHeight() + image.getHeight()) % image.getHeight(),
                    frame.time()
                ));
            }
        }
        frames = List.copyOf(frames);
        return new SpriteImageSourceImpl(image, frameWidth, frameHeight, frames, interpolateFrames);
    }

    public static SpriteImageSource vanilla(NativeImage image, AnimationMetadataSection vanillaMetadata) throws UserErrorException{
        int defaultSize = Math.min(image.getWidth(), image.getHeight());
        return vanilla(image, vanillaMetadata, defaultSize, defaultSize);
    }

    public static SpriteImageSource vanilla(NativeImage image, AnimationMetadataSection vanillaMetadata, int defaultFrameWidth, int defaultFrameHeight) throws UserErrorException{
        if(vanillaMetadata == null)
            return constant(image);

        // Calculate the frame size
        int frameWidth = image.getWidth(), frameHeight = image.getHeight();
        if(vanillaMetadata.frameWidth().isEmpty() && vanillaMetadata.frameHeight().isEmpty()){
            frameWidth = defaultFrameWidth;
            frameHeight = defaultFrameHeight;
        }else{
            if(vanillaMetadata.frameWidth().isPresent())
                frameWidth = vanillaMetadata.frameWidth().get();
            if(vanillaMetadata.frameHeight().isPresent())
                frameHeight = vanillaMetadata.frameHeight().get();
        }

        // Do vanilla frame size check
        if(image.getWidth() % frameWidth != 0 || image.getHeight() % frameHeight != 0)
            throw new UserErrorException("Image size " + image.getWidth() + "x" + image.getHeight() + " is not a multiple of frame size " + frameWidth + "x" + frameHeight + "!");

        // Convert to Fusion frame info
        int frameColumns = image.getWidth() / frameWidth, frameRows = image.getHeight() / frameHeight;
        if(vanillaMetadata.frames().isEmpty()){
            List<AnimationFrame> frames = new ArrayList<>(frameRows * frameColumns);
            for(int row = 0; row < frameRows; row++){
                for(int column = 0; column < frameColumns; column++){
                    frames.add(AnimationFrame.of(column * frameWidth, row * frameHeight, vanillaMetadata.defaultFrameTime()));
                }
            }
            return animated(image, frameWidth, frameHeight, frames, vanillaMetadata.interpolatedFrames());
        }
        List<AnimationFrame> frames = new ArrayList<>(vanillaMetadata.frames().get().size());
        for(net.minecraft.client.resources.metadata.animation.AnimationFrame frame : vanillaMetadata.frames().get()){
            frames.add(AnimationFrame.of(
                (frame.index() % frameColumns) * frameWidth,
                frame.index() / frameColumns * frameHeight,
                frame.timeOr(vanillaMetadata.defaultFrameTime())
            ));
        }
        return animated(image, frameWidth, frameHeight, frames, vanillaMetadata.interpolatedFrames());
    }

    public static AnimationFrame frame(int u, int v, int time){
        return new Frame(u, v, time);
    }

    private final NativeImage image;
    private final int frameWidth;
    private final int frameHeight;
    private final List<AnimationFrame> frames;
    private final boolean interpolateFrames;

    private SpriteImageSourceImpl(NativeImage image, int frameWidth, int frameHeight, List<AnimationFrame> frames, boolean interpolateFrames){
        this.image = image;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.frames = frames;
        this.interpolateFrames = interpolateFrames;
    }

    public NativeImage getImage(){
        return this.image;
    }

    public int getFrameWidth(){
        return this.frameWidth;
    }

    public int getFrameHeight(){
        return this.frameHeight;
    }

    public List<AnimationFrame> getFrames(){
        return this.frames;
    }

    public boolean shouldInterpolateFrames(){
        return this.interpolateFrames;
    }

    private record Frame(int u, int v, int time) implements AnimationFrame {
        Frame{
            if(time <= 0){
                throw new IllegalArgumentException("Invalid delay: " + time + "! Frame must be displayed for at least 1 tick!");
            }
        }
    }
}
