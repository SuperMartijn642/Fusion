package com.supermartijn642.fusion.api.texture;

import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
public interface TextureType<T> extends Serializer<T> {

    /**
     * Gets the size of a single frame. The returned size will be allocated in the relevant atlas.
     * @param context context for calculating the frame size
     * @param data    custom texture data
     * @return the size to allocate to this sprite in the atlas
     * @see SpritePreparationContext
     */
    default Pair<Integer,Integer> getFrameSize(SpritePreparationContext context, T data){
        return context.getOriginalFrameSize();
    }

    /**
     * The {@link Stitcher} may rotate textures when stitching them onto the texture atlas.
     * If {@code false} is returned, this behaviour is prevented and the texture will not be rotated.
     * @return whether the texture is allowed to be rotated during atlas stitching
     */
    default boolean allowRotation(){
        return true;
    }

    /**
     * Creates the sprite from the custom texture data.
     * @param context context for creating the sprite
     * @param data    custom texture data
     * @see SpriteCreationContext
     */
    TextureAtlasSprite createSprite(SpriteCreationContext context, T data);
}
