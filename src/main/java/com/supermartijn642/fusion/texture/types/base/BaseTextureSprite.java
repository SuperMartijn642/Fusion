package com.supermartijn642.fusion.texture.types.base;

import com.mojang.blaze3d.platform.Transparency;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseTextureSprite extends TextureAtlasSprite {

    private final BaseTextureData data;

    public BaseTextureSprite(Identifier atlas, SpriteContents contents, int atlasWidth, int atlasHeight, int spriteX, int spriteY, int padding, BaseTextureData data){
        super(atlas, contents, atlasWidth, atlasHeight, spriteX, spriteY, padding);
        this.data = data;
    }

    public BaseTextureData data(){
        return this.data;
    }

    public Transparency computeTransparency(float u0, float v0, float u1, float v1){
        return this.contents().computeTransparency(u0, v0, u1, v1);
    }

    public static Transparency computeTiledTransparency(TextureAtlasSprite sprite, float u0, float v0, float u1, float v1, int columns, int rows){
        Transparency spriteTransparency = sprite.contents().transparency();
        if(spriteTransparency.isOpaque()){
            return spriteTransparency;
        }else if(u0 == 0.0f && v0 == 0.0f && u1 == 1.0f && v1 == 1.0f){
            return spriteTransparency;
        }
        Transparency transparency = Transparency.NONE;
        for(int y = 0; y < rows; y++){
            for(int x = 0; x < columns; x++){
                transparency = transparency.or(sprite.contents().computeTransparency(
                        (u0 + x) / columns,
                        (v0 + y) / rows,
                        (u1 + x) / columns,
                        (v1 + y) / rows
                ));
                if(transparency.hasTranslucent()){
                    return transparency;
                }
            }
        }
        return transparency;
    }
}
