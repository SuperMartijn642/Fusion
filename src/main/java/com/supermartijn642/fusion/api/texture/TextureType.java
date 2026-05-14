package com.supermartijn642.fusion.api.texture;

import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.texture.custom.QuadProcessor;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.texture.custom.TextureCreationContext;
import com.supermartijn642.fusion.api.texture.custom.TextureOutput;
import com.supermartijn642.fusion.api.util.PropertyStore;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.api.util.UserErrorException;
import org.jetbrains.annotations.Nullable;

/**
 * Created 26/04/2023 by SuperMartijn642
 * @param <T> Serializable properties to create the texture
 * @param <X> Arbitrary data stored with the texture instance
 */
public interface TextureType<T, X> extends Serializer<T> {

    /**
     * Creates the sprites from the custom texture data.
     * @param output  output for the sprites and any custom data
     * @param context context for creating the sprites
     * @param data    custom texture data
     * @throws UserErrorException when there's a user error in the resource pack, for example an invalid configuration
     */
    void createTexture(TextureOutput<X> output, TextureCreationContext context, T data) throws UserErrorException;

    /**
     * Configures the given quad in a baked model.
     * One can return a quad processor to adjust the quad based on the render context.
     * @param quad       quad properties to configure
     * @param sprite     sprite of the quad
     * @param data       custom texture data
     * @param properties property store containing model properties as well as shared properties. May be used to store data shared between multiple quads. Any stored properties will be available to quad processors.
     * @return a processor to adjust the quad based on the render context
     */
    @Nullable
    QuadProcessor<?> initializeModelQuad(MutableQuad quad, SpriteInstance sprite, X data, PropertyStore properties);
}
