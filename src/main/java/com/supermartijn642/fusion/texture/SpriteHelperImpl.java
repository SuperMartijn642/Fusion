package com.supermartijn642.fusion.texture;

import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.extensions.TextureAtlasSpriteExtension;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class SpriteHelperImpl {

    public static TextureType<?,?> getTextureType(TextureAtlasSprite sprite){
        TextureInstance<?> texture = getTextureInstance(sprite);
        return texture == null ? DefaultTextureTypes.VANILLA : texture.getTextureType();
    }

    public static TextureInstance<?> getTextureInstance(TextureAtlasSprite sprite){
        SpriteInstance spriteInstance = getSpriteInstance(sprite);
        return spriteInstance == null ? null : spriteInstance.getTexture();
    }

    public static <X> TextureInstance<X> getTextureInstance(TextureType<?,X> type, TextureAtlasSprite sprite){
        TextureInstance<?> texture = getTextureInstance(sprite);
        if(texture != null && texture.getTextureType() == type)
            //noinspection unchecked
            return (TextureInstance<X>)texture;
        return null;
    }

    public static SpriteInstance getSpriteInstance(TextureAtlasSprite sprite){
        return ((TextureAtlasSpriteExtension)sprite).getFusionSpriteInstance();
    }
}
