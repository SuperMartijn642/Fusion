package com.supermartijn642.fusion.texture.types.base;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.TextureCreationContext;
import com.supermartijn642.fusion.api.texture.custom.TextureErrorException;
import com.supermartijn642.fusion.api.texture.custom.TextureOutput;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;

import java.util.Arrays;
import java.util.Locale;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
public class BaseTextureType implements TextureType<BaseTextureData,BaseTextureData> {

    @Override
    public void createTexture(TextureOutput<BaseTextureData> output, TextureCreationContext context, BaseTextureData data) throws TextureErrorException{
        // Create vanilla sprite
        //noinspection rawtypes,unchecked
        DefaultTextureTypes.VANILLA.createTexture((TextureOutput)output, context, null);
        // Set custom texture data
        output.setCustomData(data);
    }

    @Override
    public BaseTextureData deserialize(JsonObject json) throws JsonParseException{
        BaseTextureData.Builder<?,BaseTextureData> builder = BaseTextureData.builder();
        // render_type
        if(json.has("render_type")){
            if(!json.get("render_type").isJsonPrimitive() || !json.getAsJsonPrimitive("render_type").isString())
                throw new JsonParseException("Property 'render_type' must be a string!");
            String renderTypeString = json.get("render_type").getAsString();
            BaseTextureData.RenderType renderType;
            try{
                renderType = BaseTextureData.RenderType.valueOf(renderTypeString.toUpperCase(Locale.ROOT));
            }catch(IllegalArgumentException e){
                throw new JsonParseException("Property 'render_type' must be one of " + Arrays.toString(BaseTextureData.RenderType.values()).toLowerCase(Locale.ROOT) + ", not '" + renderTypeString + "'!");
            }
            builder.renderType(renderType);
        }
        // emissive
        if(json.has("emissive")){
            if(!json.get("emissive").isJsonPrimitive() || !json.getAsJsonPrimitive("emissive").isBoolean())
                throw new JsonParseException("Property 'emissive' must be a boolean!");
            builder.emissive(json.get("emissive").getAsBoolean());
        }
        // tinting
        if(json.has("tinting")){
            if(!json.get("tinting").isJsonPrimitive() || !json.getAsJsonPrimitive("tinting").isString())
                throw new JsonParseException("Property 'tinting' must be a string!");
            String tintingString = json.get("tinting").getAsString();
            BaseTextureData.QuadTinting tinting;
            try{
                tinting = BaseTextureData.QuadTinting.valueOf(tintingString.toUpperCase(Locale.ROOT));
            }catch(IllegalArgumentException e){
                throw new JsonParseException("Property 'tinting' must be one of " + Arrays.toString(BaseTextureData.QuadTinting.values()).toLowerCase(Locale.ROOT) + ", not '" + tintingString + "'!");
            }
            builder.tinting(tinting);
        }
        return builder.build();
    }

    @Override
    public JsonObject serialize(BaseTextureData value){
        JsonObject json = new JsonObject();
        if(value.getRenderType() != null)
            json.addProperty("render_type", value.getRenderType().name().toLowerCase());
        if(value.isEmissive())
            json.addProperty("emissive", true);
        if(value.getTinting() != null)
            json.addProperty("tinting", value.getTinting().name().toLowerCase());
        return json;
    }
}
