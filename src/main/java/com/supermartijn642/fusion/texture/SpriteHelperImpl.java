package com.supermartijn642.fusion.texture;

import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.extensions.TextureAtlasSpriteExtension;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class SpriteHelperImpl {

    public static TextureType<?> getTextureType(TextureAtlasSprite sprite){
        TextureType<?> textureType = ((TextureAtlasSpriteExtension)sprite).getFusionTextureType();
        if(textureType == null){
            ((TextureAtlasSpriteExtension)sprite).setFusionTextureType(DefaultTextureTypes.VANILLA);
            return DefaultTextureTypes.VANILLA;
        }
        return textureType;
    }
}
