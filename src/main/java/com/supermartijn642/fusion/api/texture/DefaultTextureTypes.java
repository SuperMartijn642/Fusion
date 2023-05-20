package com.supermartijn642.fusion.api.texture;

import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureLayout;
import com.supermartijn642.fusion.api.texture.data.ScrollingTextureData;
import com.supermartijn642.fusion.texture.types.VanillaTextureType;
import com.supermartijn642.fusion.texture.types.connecting.ConnectingTextureType;
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
    public static final TextureType<Void> VANILLA = new VanillaTextureType();
    /**
     * Texture type with a connected texture layout. Should be used in conjunction with {@link DefaultModelTypes#CONNECTING} model type.
     * @see ConnectingTextureLayout
     * @see DefaultModelTypes#CONNECTING
     */
    public static final TextureType<ConnectingTextureLayout> CONNECTING = new ConnectingTextureType();
    /**
     * Texture type with an animated sprite which scrolls over the texture.
     * @see ScrollingTextureData#builder()
     */
    public static final TextureType<ScrollingTextureData> SCROLLING = new ScrollingTextureType();
}
