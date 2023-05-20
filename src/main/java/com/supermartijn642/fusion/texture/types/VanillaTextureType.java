package com.supermartijn642.fusion.texture.types;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.texture.SpriteCreationContext;
import com.supermartijn642.fusion.api.texture.SpritePreparationContext;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.util.Pair;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class VanillaTextureType implements TextureType<Void> {

    @Override
    public Pair<Integer,Integer> getFrameSize(SpritePreparationContext context, Void data){
        return context.getOriginalFrameSize();
    }

    @Override
    public TextureAtlasSprite createSprite(SpriteCreationContext context, Void data){
        return context.createOriginalSprite();
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
