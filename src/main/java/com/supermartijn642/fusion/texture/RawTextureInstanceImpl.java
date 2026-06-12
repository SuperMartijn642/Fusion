package com.supermartijn642.fusion.texture;

import com.supermartijn642.fusion.api.texture.RawTextureInstance;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.TextureCreationContext;
import com.supermartijn642.fusion.api.texture.custom.TextureOutput;
import com.supermartijn642.fusion.api.util.UserErrorException;

/**
 * Created 12/06/2026 by SuperMartijn642
 */
public class RawTextureInstanceImpl<T, X> implements RawTextureInstance<T,X> {

    private final TextureType<T,X> textureType;
    private final T textureData;

    public RawTextureInstanceImpl(TextureType<T,X> textureType, T textureData){
        this.textureType = textureType;
        this.textureData = textureData;
    }

    @Override
    public TextureType<T,X> getTextureType(){
        return this.textureType;
    }

    @Override
    public T getTextureData(){
        return this.textureData;
    }

    @Override
    public void createTexture(TextureOutput<X> output, TextureCreationContext context) throws UserErrorException{
        this.textureType.createTexture(output, context, this.textureData);
    }
}
