package com.supermartijn642.fusion.texture;

import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;

/**
 * Created 28/04/2026 by SuperMartijn642
 */
public class TextureAtlasMixinHelper {

    /*
     * This is needed as to not directly reference DummyTextureSpriteContents from TextureAtlasMixin.
     *
     * Referencing DummyTextureSpriteContents from TextureAtlasMixin causes the TextureAtlasSprite class
     * to be loaded during validation of TextureAtlasMixin. This means it gets loaded before our
     * TextureAtlasSpriteMixin mixin can be applied.
     * This seems like a bug with Mixin and only happens on 1.14 ¯\(o_o)/¯
     */

    public static boolean isDummySprite(TextureAtlasSprite sprite){
        return sprite instanceof DummyTextureSpriteContents.Child;
    }

    public static Object getDummyParent(TextureAtlasSprite sprite){
        return ((DummyTextureSpriteContents.Child)sprite).parent();
    }

    public static TextureCreationHandler.Result<CompletableFuture<Void>> onLoadSprite(Object dummySprites, AtlasTexture textureAtlas, int atlasWidth, int atlasHeight, int mipmapLevels, Queue<TextureAtlasSprite> queue){
        return TextureCreationHandler.onLoadSprite((DummyTextureSpriteContents)dummySprites, textureAtlas, atlasWidth, atlasHeight, mipmapLevels, queue);
    }
}
