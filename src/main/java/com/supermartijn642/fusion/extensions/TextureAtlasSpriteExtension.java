package com.supermartijn642.fusion.extensions;

import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.util.Pair;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
public interface TextureAtlasSpriteExtension {

    void setFusionTextureType(TextureType<?> type);

    TextureType<?> getFusionTextureType();

    void setTextureSize(int width, int height);

    Pair<Integer,Integer> getTextureSize();
}
