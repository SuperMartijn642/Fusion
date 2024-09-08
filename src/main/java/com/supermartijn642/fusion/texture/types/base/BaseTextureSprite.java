package com.supermartijn642.fusion.texture.types.base;

import com.mojang.blaze3d.platform.NativeImage;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseTextureSprite extends TextureAtlasSprite {

    private final BaseTextureData data;

    public BaseTextureSprite(TextureAtlas atlas, Info info, int mipmapLevels, int atlasWidth, int atlasHeight, int spriteX, int spriteY, NativeImage image, BaseTextureData data){
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
            original.getX(),
            original.getY(),
            FusionClient.getDummyImage(),
            data
        );
        this.mainImage = original.mainImage;
        this.animatedTexture = original.animatedTexture;
        this.u0 = original.u0;
        this.u1 = original.u1;
        this.v0 = original.v0;
        this.v1 = original.v1;
    }

    public BaseTextureData data(){
        return this.data;
    }
}
