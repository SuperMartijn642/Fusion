package com.supermartijn642.fusion.api.texture.custom;

import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.util.PropertyStore;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Instance of a texture whose sprites have been stitched.
 * <p>
 * Created 22/03/2026 by SuperMartijn642
 */
@ApiStatus.NonExtendable
public interface TextureInstance<X> {

    /**
     * Gets the type of this texture instance.
     */
    TextureType<?,X> getTextureType();

    /**
     * Gets the identifier of this texture.
     */
    Identifier getIdentifier();

    /**
     * Gets the sprites associated with this texture.
     */
    List<SpriteInstance> getSprites();

    /**
     * Gets the default sprite for this texture.
     */
    SpriteInstance getDefaultSprite();

    /**
     * Gets the custom data from this texture.
     * @see TextureOutput#setCustomData(Object)
     */
    X getCustomData();

    /**
     * Configures the given quad in a block state model.
     * Can return a quad processor to adjust the quad based on world state.
     * @param quad       quad properties to configure
     * @param sprite     sprite of the quad
     * @param properties property store containing model properties as well as shared properties. May be used to store data shared between multiple quads. Any stored properties will be available to quad processors.
     * @return a processor to adjust the quad based on world state
     */
    @Nullable
    default BlockStateQuadProcessor<?> initializeBlockStateModelQuad(MutableQuad quad, SpriteInstance sprite, PropertyStore properties){
        return this.getTextureType().initializeBlockStateModelQuad(quad, sprite, this.getCustomData(), properties);
    }

    /**
     * Configures the given quad in an item model.
     * Can return a quad processor to adjust the quad based on item state.
     * @param quad       quad properties to configure
     * @param sprite     sprite of the quad
     * @param properties property store containing model properties as well as shared properties. May be used to store data shared between multiple quads. Any stored properties will be available to quad processors.
     * @return a processor to adjust the quad based on item state
     */
    @Nullable
    default ItemQuadProcessor<?> initializeItemModelQuad(MutableQuad quad, SpriteInstance sprite, PropertyStore properties){
        return this.getTextureType().initializeItemModelQuad(quad, sprite, this.getCustomData(), properties);
    }
}
