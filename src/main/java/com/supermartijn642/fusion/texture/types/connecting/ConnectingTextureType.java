package com.supermartijn642.fusion.texture.types.connecting;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.texture.SpriteCreationContext;
import com.supermartijn642.fusion.api.texture.SpritePreparationContext;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureData;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureLayout;
import com.supermartijn642.fusion.api.util.Pair;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.util.Arrays;
import java.util.Locale;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
public class ConnectingTextureType implements TextureType<ConnectingTextureData> {

    @Override
    public ConnectingTextureData deserialize(JsonObject json) throws JsonParseException{
        ConnectingTextureData.Builder builder = ConnectingTextureData.builder();
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
        if(json.has("render_type")){
            if(!json.get("render_type").isJsonPrimitive() || !json.getAsJsonPrimitive("render_type").isString())
                throw new JsonParseException("Property 'render_type' must be a string!");
            String renderTypeString = json.get("render_type").getAsString();
            ConnectingTextureData.RenderType renderType;
            try{
                renderType = ConnectingTextureData.RenderType.valueOf(renderTypeString.toUpperCase(Locale.ROOT));
            }catch(IllegalArgumentException e){
                throw new JsonParseException("Property 'render_type' must be one of " + Arrays.toString(ConnectingTextureData.RenderType.values()).toLowerCase(Locale.ROOT) + ", not '" + renderTypeString + "'!");
            }
            builder.renderType(renderType);
        }
        return builder.build();
    }

    @Override
    public JsonObject serialize(ConnectingTextureData data){
        JsonObject json = new JsonObject();
        if(data.getLayout() != ConnectingTextureLayout.FULL)
            json.addProperty("layout", data.getLayout().name().toLowerCase(Locale.ROOT));
        if(data.getRenderType() != null)
            json.addProperty("render_type", data.getRenderType().name().toLowerCase(Locale.ROOT));
        return json.size() == 0 ? null : json;
    }

    @Override
    public Pair<Integer,Integer> getFrameSize(SpritePreparationContext context, ConnectingTextureData data){
        if(ConnectingTextureLayoutHelper.shouldBeRotated(data.getLayout()))
            return Pair.of(context.getOriginalFrameHeight(), context.getOriginalFrameWith());
        return context.getOriginalFrameSize();
    }

    @Override
    public TextureAtlasSprite createSprite(SpriteCreationContext context, ConnectingTextureData data){
        TextureAtlasSprite sprite = context.createOriginalSprite();
        return new ConnectingTextureSprite(sprite, data.getLayout(), data.getRenderType());
    }
}
