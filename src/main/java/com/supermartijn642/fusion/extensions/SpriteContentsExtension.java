package com.supermartijn642.fusion.extensions;

import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.util.Pair;

/**
 * Created 15/07/2025 by SuperMartijn642
 */
public interface SpriteContentsExtension {

    void setFusionMetadata(Pair<TextureType<Object>,Object> metadata);

    Pair<TextureType<Object>,Object> getFusionMetadata();
}
