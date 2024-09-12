package com.supermartijn642.fusion.texture.types.base;

import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.data.AnimationMetadataSection;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseTextureSprite extends TextureAtlasSprite {

    private final BaseTextureData data;

    public BaseTextureSprite(AtlasTexture atlas, Info info, int mipmapLevels, int atlasWidth, int atlasHeight, int spriteX, int spriteY, NativeImage image, BaseTextureData data){
        super(atlas, info, mipmapLevels, atlasWidth, atlasHeight, spriteX, spriteY, image);
        this.data = data;
    }

    public BaseTextureSprite(TextureAtlasSprite original, BaseTextureData data){
        this(
            original.atlas(),
            new Info(original.getName(), original.getWidth(), original.getHeight(), AnimationMetadataSection.EMPTY),
            0,
            1,
            1,
            original.x,
            original.y,
            FusionClient.getDummyImage(),
            data
        );
        this.mainImage = original.mainImage;
        this.u0 = original.u0;
        this.u1 = original.u1;
        this.v0 = original.v0;
        this.v1 = original.v1;
        this.framesX = original.framesX;
        this.framesY = original.framesY;
    }

    public BaseTextureData data(){
        return this.data;
    }
}
