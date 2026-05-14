package com.supermartijn642.fusion.texture.types.base;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.QuadProcessor;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.texture.custom.TextureCreationContext;
import com.supermartijn642.fusion.api.texture.custom.TextureOutput;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.api.util.PropertyStore;
import com.supermartijn642.fusion.api.util.UserErrorException;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
public class BaseTextureType implements TextureType<BaseTextureData,BaseTextureData> {

    @Override
    public void createTexture(TextureOutput<BaseTextureData> output, TextureCreationContext context, BaseTextureData data) throws UserErrorException{
        // Create vanilla sprite
        //noinspection rawtypes,unchecked
        DefaultTextureTypes.VANILLA.createTexture((TextureOutput)output, context, null);
        // Set custom texture data
        output.setCustomData(data);
    }

    @Override
    public @Nullable QuadProcessor<?> initializeModelQuad(MutableQuad quad, SpriteInstance sprite, BaseTextureData data, PropertyStore properties){
        applyProperties(quad, data);
        return null;
    }

    public static void applyProperties(MutableQuad quad, BaseTextureData data){
        if(data.isEmissive())
            quad.emissive(true);
        if(data.getRenderType() != null)
            quad.chunkRenderType(getChunkLayer(data.getRenderType()));
        if(data.getTinting() != null)
            quad.tintIndex(39216);
    }

    public static RenderType getChunkLayer(BaseTextureData.RenderType renderType){
        if(renderType == null)
            return null;
        return switch(renderType){
            case OPAQUE -> RenderType.solid();
            case CUTOUT -> RenderType.cutout();
            case TRANSLUCENT -> RenderType.translucent();
        };
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
            json.addProperty("render_type", value.getRenderType().name().toLowerCase(Locale.ROOT));
        if(value.isEmissive())
            json.addProperty("emissive", true);
        if(value.getTinting() != null)
            json.addProperty("tinting", value.getTinting().name().toLowerCase(Locale.ROOT));
        return json;
    }
}
