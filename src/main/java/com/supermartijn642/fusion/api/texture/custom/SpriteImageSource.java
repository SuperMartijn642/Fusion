package com.supermartijn642.fusion.api.texture.custom;

import com.supermartijn642.fusion.texture.custom.SpriteImageSourceImpl;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.resources.data.AnimationMetadataSection;

import java.util.List;

/**
 * Created 22/03/2026 by SuperMartijn642
 */
public interface SpriteImageSource {

    /**
     * Simply uploads the given image to the texture atlas.
     */
    static SpriteImageSource constant(NativeImage image){
        return SpriteImageSourceImpl.constant(image);
    }

    /**
     * Uploads a {@code frameWidth} by {@code frameHeight} area of the given image to the texture atlas.
     * What area is uploaded and when it changes is specified by the given frame information.
     * @param image             image that frames are taken from
     * @param frameWidth        the width of a single frame on the texture atlas
     * @param frameHeight       the height of a single frame on the texture atlas
     * @param frames            the location and duration of each frame
     * @param interpolateFrames whether to interpolate between frames
     */
    static SpriteImageSource animated(NativeImage image, int frameWidth, int frameHeight, List<AnimationFrame> frames, boolean interpolateFrames){
        return SpriteImageSourceImpl.animated(image, frameWidth, frameHeight, frames, interpolateFrames);
    }

    /**
     * Uploads frames from the given image the same as vanilla would.
     * @throws TextureErrorException when the given image and animation metadata do not pass vanilla's frame size checks
     */
    static SpriteImageSource vanilla(NativeImage image, AnimationMetadataSection vanillaMetadata) throws TextureErrorException{
        return SpriteImageSourceImpl.vanilla(image, vanillaMetadata);
    }

    /**
     * Uploads frames from the given image the same as vanilla would, but with a different default frame size when no frame size is specified in the animation metadata.
     * @throws TextureErrorException when the given image and animation metadata do not pass vanilla's frame size checks
     */
    static SpriteImageSource vanilla(NativeImage image, AnimationMetadataSection vanillaMetadata, int defaultFrameWidth, int defaultFrameHeight) throws TextureErrorException{
        return SpriteImageSourceImpl.vanilla(image, vanillaMetadata, defaultFrameWidth, defaultFrameHeight);
    }

    interface AnimationFrame {

        static AnimationFrame of(int u, int v, int time){
            return SpriteImageSourceImpl.frame(u, v, time);
        }

        int u();

        int v();

        int time();
    }
}
