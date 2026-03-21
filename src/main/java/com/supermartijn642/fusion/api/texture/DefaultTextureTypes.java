package com.supermartijn642.fusion.api.texture;

import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.texture.data.*;
import com.supermartijn642.fusion.texture.types.VanillaTextureType;
import com.supermartijn642.fusion.texture.types.base.BaseTextureType;
import com.supermartijn642.fusion.texture.types.connecting.ConnectingTextureType;
import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureType;
import com.supermartijn642.fusion.texture.types.random.RandomTextureType;
import com.supermartijn642.fusion.texture.types.scrolling.ScrollingTextureType;

/**
 * Contains references to the default texture types provided by Fusion.
 * <p>
 * Created 27/04/2023 by SuperMartijn642
 */
public final class DefaultTextureTypes {

    /**
     * Model type used for vanilla textures.
     */
    public static final TextureType<Void,Void> VANILLA = new VanillaTextureType();
    /**
     * Model type which adds some common properties.
     */
    public static final TextureType<BaseTextureData,?> BASE = new BaseTextureType();
    /**
     * Texture type with a connected texture layout. Should be used in conjunction with {@link DefaultModelTypes#CONNECTING} model type.
     * @see ConnectingTextureData#builder()
     * @see DefaultModelTypes#CONNECTING
     */
    public static final TextureType<ConnectingTextureData,?> CONNECTING = new ConnectingTextureType();
    /**
     * Texture type with an animated sprite which scrolls over the texture.
     * @see ScrollingTextureData#builder()
     */
    public static final TextureType<ScrollingTextureData,?> SCROLLING = new ScrollingTextureType();
    /**
     * Texture type which picks a random tile from a tile sheet. Should be used in conjunction with {@link DefaultModelTypes#BASE} model type.
     * @see RandomTextureData#builder()
     */
    public static final TextureType<RandomTextureData,?> RANDOM = new RandomTextureType();
    /**
     * Texture type for which the texture continues over multiple block spaces. Should be used in conjunction with {@link DefaultModelTypes#BASE} model type.
     * @see ContinuousTextureData#builder()
     */
    public static final TextureType<ContinuousTextureData,?> CONTINUOUS = new ContinuousTextureType();
}
