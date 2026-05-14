package com.supermartijn642.fusion.texture.types;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.*;
import com.supermartijn642.fusion.api.util.PropertyStore;
import com.supermartijn642.fusion.api.util.UserErrorException;
import org.jetbrains.annotations.Nullable;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class VanillaTextureType implements TextureType<Void,Void> {

    @Override
    public void createTexture(TextureOutput<Void> output, TextureCreationContext context, Void data) throws UserErrorException{
        output.createSprite()
            .image(SpriteImageSource.vanilla(context.getImage(), context.getAnimationMetadata()))
            .submit();
    }

    @Override
    public @Nullable QuadProcessor<?> initializeModelQuad(MutableQuad quad, SpriteInstance sprite, Void data, PropertyStore properties){
        return null;
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
