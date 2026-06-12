package com.supermartijn642.fusion.api.texture;

import com.supermartijn642.fusion.api.texture.custom.TextureCreationContext;
import com.supermartijn642.fusion.api.texture.custom.TextureOutput;
import com.supermartijn642.fusion.api.util.UserErrorException;
import com.supermartijn642.fusion.texture.RawTextureInstanceImpl;
import org.jetbrains.annotations.ApiStatus;

/**
 * A container for a texture type along with its data.
 * <p>
 * Created 12/06/2026 by SuperMartijn642
 */
@ApiStatus.NonExtendable
public interface RawTextureInstance<T,X> {

    /**
     * Create a new texture instance for the given values.
     */
    static <T,X> RawTextureInstance<T,X> of(TextureType<T,X> textureType, T textureData){
        return new RawTextureInstanceImpl<>(textureType, textureData);
    }

    /**
     * The type of the texture.
     */
    TextureType<T,X> getTextureType();

    /**
     * The data of the texture.
     */
    T getTextureData();

    /**
     * Creates the sprites from the custom texture data.
     * @param output  output for the sprites and any custom data
     * @param context context for creating the sprites
     * @throws UserErrorException when there's a user error in the resource pack, for example an invalid configuration
     */
    void createTexture(TextureOutput<X> output, TextureCreationContext context) throws UserErrorException;
}
