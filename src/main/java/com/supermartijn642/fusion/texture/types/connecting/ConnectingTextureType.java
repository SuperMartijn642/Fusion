package com.supermartijn642.fusion.texture.types.connecting;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.SpriteCreationContext;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureData;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureLayout;
import com.supermartijn642.fusion.texture.types.connecting.layouts.ConnectingTextureLayoutHandler;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.util.Arrays;
import java.util.Locale;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
public class ConnectingTextureType implements TextureType<ConnectingTextureData> {

    @Override
    public ConnectingTextureData deserialize(JsonObject json) throws JsonParseException{
        // Deserialize base properties
        BaseTextureData base = DefaultTextureTypes.BASE.deserialize(json);
        // Copy base properties
        ConnectingTextureData.Builder builder = ConnectingTextureData.builder();
        builder.renderType(base.getRenderType());
        builder.emissive(base.isEmissive());
        builder.tinting(base.getTinting());
        // Deserialize 'layout'
        if(json.has("layout")){
            if(!json.get("layout").isJsonPrimitive() || !json.getAsJsonPrimitive("layout").isString())
                throw new JsonParseException("Property 'layout' must be a string!");
            String layoutString = json.get("layout").getAsString();
            ConnectingTextureLayout layout;
            try{
                layout = ConnectingTextureLayout.valueOf(layoutString.toUpperCase(Locale.ROOT));
            }catch(IllegalArgumentException e){
                throw new JsonParseException("Property 'layout' must be one of " + Arrays.toString(ConnectingTextureLayout.values()).toLowerCase(Locale.ROOT) + ", not '" + layoutString + "'!");
            }
            builder.layout(layout);
        }
        return builder.build();
    }

    @Override
    public JsonObject serialize(ConnectingTextureData data){
        // Serialize base properties
        JsonObject json = DefaultTextureTypes.BASE.serialize(data);
        // Serialize 'layout'
        if(data.getLayout() != ConnectingTextureLayout.FULL)
            json.addProperty("layout", data.getLayout().name().toLowerCase(Locale.ROOT));
        return json.size() == 0 ? null : json;
    }

    @Override
    public TextureAtlasSprite createSprite(SpriteCreationContext context, ConnectingTextureData data){
        ConnectingTextureLayoutHandler layoutHandler = ConnectingTextureLayoutHandler.get(data.getLayout());
        TextureAtlasSprite sprite = context.createOriginalSprite();
        float tileWidth = (sprite.u1 - sprite.u0) / layoutHandler.getWidth();
        float tileHeight = (sprite.v1 - sprite.v0) / layoutHandler.getHeight();
        sprite.u1 = sprite.u0 + tileWidth * (layoutHandler.defaultTileX() + 1);
        sprite.v1 = sprite.v0 + tileHeight * (layoutHandler.defaultTileY() + 1);
        sprite.u0 = sprite.u0 + tileWidth * layoutHandler.defaultTileX();
        sprite.v0 = sprite.v0 + tileHeight * layoutHandler.defaultTileY();
        return new ConnectingTextureSprite(sprite, data);
    }
}
