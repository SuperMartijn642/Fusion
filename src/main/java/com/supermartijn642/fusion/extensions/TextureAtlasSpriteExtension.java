package com.supermartijn642.fusion.extensions;

import com.supermartijn642.fusion.api.texture.TextureType;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
public interface TextureAtlasSpriteExtension {

    void setFusionTextureType(TextureType<?> type);

    TextureType<?> getFusionTextureType();
}
