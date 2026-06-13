package com.supermartijn642.fusion.texture.custom;

import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Created 23/03/2026 by SuperMartijn642
 */
public class TextureInstanceImpl<X> implements TextureInstance<X> {

    private final TextureType<?,X> textureType;
    private final Identifier identifier;
    private final X customData;
    private List<SpriteInstance> sprites;
    private SpriteInstance defaultSprite;

    public TextureInstanceImpl(TextureType<?,X> textureType, Identifier identifier, X customData){
        this.textureType = textureType;
        this.identifier = identifier;
        this.customData = customData;
    }

    public void setSprites(List<SpriteInstance> sprites, SpriteInstance defaultSprite){
        this.sprites = sprites;
        this.defaultSprite = defaultSprite;
    }

    @Override
    public TextureType<?,X> getTextureType(){
        return this.textureType;
    }

    @Override
    public Identifier getIdentifier(){
        return this.identifier;
    }

    @Override
    public List<SpriteInstance> getSprites(){
        return this.sprites;
    }

    @Override
    public SpriteInstance getDefaultSprite(){
        return this.defaultSprite;
    }

    @Override
    public X getCustomData(){
        return this.customData;
    }
}
