package com.supermartijn642.fusion.api.texture;

import com.supermartijn642.fusion.api.texture.custom.TextureCreationContext;
import com.supermartijn642.fusion.api.texture.custom.TextureOutput;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.api.util.UserErrorException;

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
}
