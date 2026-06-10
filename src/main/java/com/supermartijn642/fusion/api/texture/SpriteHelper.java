package com.supermartijn642.fusion.api.texture;

import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.texture.SpriteHelperImpl;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import javax.annotation.Nullable;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public final class SpriteHelper {

    /**
     * Returns the texture type of the given sprite.
     * For non-fusion sprites, this will return {@link DefaultTextureTypes#VANILLA}.
     */
    public static TextureType<?,?> getTextureType(TextureAtlasSprite sprite){
        return SpriteHelperImpl.getTextureType(sprite);
    }

    /**
     * Returns the Fusion {@link TextureInstance} that the given sprite belongs to.
     * For non-fusion sprites, this will return {@code null}.
     */
    @Nullable
    public static TextureInstance<?> getTextureInstance(TextureAtlasSprite sprite){
        return SpriteHelperImpl.getTextureInstance(sprite);
    }

    /**
     * Returns the Fusion {@link TextureInstance} that the given sprite belongs to if the texture type matches the given type.
     * For non-fusion sprites, this will return {@code null}.
     */
    @Nullable
    public static <X> TextureInstance<X> getTextureInstance(TextureType<?,X> type, TextureAtlasSprite sprite){
        return SpriteHelperImpl.getTextureInstance(type, sprite);
    }

    /**
     * Returns the Fusion {@link SpriteInstance} associated with the given sprite.
     * For non-fusion sprites, this will return {@code null}.
     */
    @Nullable
    public static SpriteInstance getSpriteInstance(TextureAtlasSprite sprite){
        return SpriteHelperImpl.getSpriteInstance(sprite);
    }
}
