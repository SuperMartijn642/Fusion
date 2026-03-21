package com.supermartijn642.fusion.texture.types;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.SpriteImageSource;
import com.supermartijn642.fusion.api.texture.custom.TextureCreationContext;
import com.supermartijn642.fusion.api.texture.custom.TextureErrorException;
import com.supermartijn642.fusion.api.texture.custom.TextureOutput;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class VanillaTextureType implements TextureType<Void,Void> {

    @Override
    public void createTexture(TextureOutput<Void> output, TextureCreationContext context, Void data) throws TextureErrorException{
        output.createSprite()
            .image(SpriteImageSource.vanilla(context.getImage(), context.getAnimationMetadata()))
            .submit();
    }

    @Override
    public Void deserialize(JsonObject json) throws JsonParseException{
        return null;
    }

    @Override
    public JsonObject serialize(Void value){
        return null;
    }
}
