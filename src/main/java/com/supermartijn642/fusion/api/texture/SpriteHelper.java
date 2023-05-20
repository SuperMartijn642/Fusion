package com.supermartijn642.fusion.api.texture;

import com.supermartijn642.fusion.texture.SpriteHelperImpl;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public final class SpriteHelper {

    /**
     * Returns the texture type of a given sprite.
     */
    public static TextureType<?> getTextureType(TextureAtlasSprite sprite){
        return SpriteHelperImpl.getTextureType(sprite);
    }
}
