package com.supermartijn642.fusion.texture.custom;

import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

/**
 * Created 23/03/2026 by SuperMartijn642
 */
public class SpriteInstanceImpl implements SpriteInstance {

    private final TextureInstance<?> texture;
    private final TextureAtlasSprite sprite;
    private final ResourceLocation identifier;

    public SpriteInstanceImpl(TextureInstance<?> texture, TextureAtlasSprite sprite, ResourceLocation identifier){
        this.texture = texture;
        this.sprite = sprite;
        this.identifier = identifier;
    }

    @Override
    public TextureInstance<?> getTexture(){
        return this.texture;
    }

    @Override
    public TextureAtlasSprite getSprite(){
        return this.sprite;
    }

    @Override
    public ResourceLocation getIdentifier(){
        return this.identifier;
    }

    @Override
    public float getU0(){
        return this.sprite.getU0();
    }

    @Override
    public float getU1(){
        return this.sprite.getU1();
    }

    @Override
    public float getV0(){
        return this.sprite.getV0();
    }

    @Override
    public float getV1(){
        return this.sprite.getV1();
    }
}
