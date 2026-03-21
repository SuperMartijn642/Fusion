package com.supermartijn642.fusion.api.texture.custom;

import com.supermartijn642.fusion.api.texture.TextureType;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Created 22/03/2026 by SuperMartijn642
 */
public interface TextureInstance<X> {

    /**
     * Gets the type of this texture instance.
     */
    TextureType<?,X> getTextureType();

    /**
     * Gets the identifier of this texture.
     */
    ResourceLocation getIdentifier();

    /**
     * Gets the sprites associated with this texture.
     */
    List<SpriteInstance> getSprites();

    /**
     * Gets the custom data from this texture.
     * @see TextureOutput#setCustomData(Object)
     */
    X getCustomData();
}
