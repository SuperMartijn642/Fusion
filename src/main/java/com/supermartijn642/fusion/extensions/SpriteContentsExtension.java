package com.supermartijn642.fusion.extensions;

import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.util.Pair;

/**
 * Created 12/09/2023 by SuperMartijn642
 */
public interface SpriteContentsExtension {

    Pair<TextureType<Object>,Object> fusionTextureMetadata();

    void clearFusionTextureMetadata();
}
