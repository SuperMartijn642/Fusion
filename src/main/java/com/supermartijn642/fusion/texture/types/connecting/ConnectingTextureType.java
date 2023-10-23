package com.supermartijn642.fusion.texture.types.connecting;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.texture.SpriteCreationContext;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureData;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureLayout;
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
        return json.isEmpty() ? null : json;
    }

    @Override
    public TextureAtlasSprite createSprite(SpriteCreationContext context, ConnectingTextureData data){
        TextureAtlasSprite sprite = context.createOriginalSprite();
        int scale = getScaleFactor(data.getLayout());
        sprite.u1 = sprite.u0 + (sprite.u1 - sprite.u0) / scale;
        sprite.v1 = sprite.v0 + (sprite.v1 - sprite.v0) / scale;
        return new ConnectingTextureSprite(sprite, data.getLayout(), data.getRenderType());
    }

    public static int getScaleFactor(ConnectingTextureLayout layout){
        return layout == ConnectingTextureLayout.FULL ? 8 : 4;
    }

    public static int[] getStatePosition(ConnectingTextureLayout layout, boolean up, boolean upRight, boolean right, boolean bottomRight, boolean bottom, boolean bottomLeft, boolean left, boolean upLeft){
        if(layout == ConnectingTextureLayout.FULL)
            return getStatePositionFull(up, upRight, right, bottomRight, bottom, bottomLeft, left, upLeft);
        if(layout == ConnectingTextureLayout.SIMPLE)
            return getStatePositionSimple(up, right, bottom, left);

        throw new IllegalStateException("Unknown layout '" + layout + "'!");
    }

    public static int[] getStatePositionFull(boolean up, boolean upRight, boolean right, boolean bottomRight, boolean bottom, boolean bottomLeft, boolean left, boolean upLeft){
        int[] uv;

        if(!left && !up && !right && !bottom) // all directions
            uv = new int[]{0, 0};
        else{ // one direction
            if(left && !up && !right && !bottom)
                uv = new int[]{3, 0};
            else if(!left && up && !right && !bottom)
                uv = new int[]{0, 3};
            else if(!left && !up && right && !bottom)
                uv = new int[]{1, 0};
            else if(!left && !up && !right && bottom)
                uv = new int[]{0, 1};
            else{ // two directions
                if(left && !up && right && !bottom)
                    uv = new int[]{2, 0};
                else if(!left && up && !right && bottom)
                    uv = new int[]{0, 2};
                else if(left && up && !right && !bottom){
                    if(upLeft)
                        uv = new int[]{3, 3};
                    else
                        uv = new int[]{5, 1};
                }else if(!left && up && right && !bottom){
                    if(upRight)
                        uv = new int[]{1, 3};
                    else
                        uv = new int[]{4, 1};
                }else if(!left && !up && right && bottom){
                    if(bottomRight)
                        uv = new int[]{1, 1};
                    else
                        uv = new int[]{4, 0};
                }else if(left && !up && !right && bottom){
                    if(bottomLeft)
                        uv = new int[]{3, 1};
                    else
                        uv = new int[]{5, 0};
                }else{ // three directions
                    if(!left){
                        if(upRight && bottomRight)
                            uv = new int[]{1, 2};
                        else if(upRight)
                            uv = new int[]{4, 2};
                        else if(bottomRight)
                            uv = new int[]{6, 2};
                        else
                            uv = new int[]{6, 0};
                    }else if(!up){
                        if(bottomLeft && bottomRight)
                            uv = new int[]{2, 1};
                        else if(bottomLeft)
                            uv = new int[]{7, 2};
                        else if(bottomRight)
                            uv = new int[]{5, 2};
                        else
                            uv = new int[]{7, 0};
                    }else if(!right){
                        if(upLeft && bottomLeft)
                            uv = new int[]{3, 2};
                        else if(upLeft)
                            uv = new int[]{7, 3};
                        else if(bottomLeft)
                            uv = new int[]{5, 3};
                        else
                            uv = new int[]{7, 1};
                    }else if(!bottom){
                        if(upLeft && upRight)
                            uv = new int[]{2, 3};
                        else if(upLeft)
                            uv = new int[]{4, 3};
                        else if(upRight)
                            uv = new int[]{6, 3};
                        else
                            uv = new int[]{6, 1};
                    }else{ // four directions
                        if(upLeft && upRight && bottomLeft && bottomRight)
                            uv = new int[]{2, 2};
                        else{
                            if(!upLeft && upRight && bottomLeft && bottomRight)
                                uv = new int[]{7, 5};
                            else if(upLeft && !upRight && bottomLeft && bottomRight)
                                uv = new int[]{6, 5};
                            else if(upLeft && upRight && !bottomLeft && bottomRight)
                                uv = new int[]{7, 4};
                            else if(upLeft && upRight && bottomLeft && !bottomRight)
                                uv = new int[]{6, 4};
                            else{
                                if(!upLeft && upRight && !bottomRight && bottomLeft)
                                    uv = new int[]{0, 4};
                                else if(upLeft && !upRight && bottomRight && !bottomLeft)
                                    uv = new int[]{0, 5};
                                else if(!upLeft && !upRight && bottomRight && bottomLeft)
                                    uv = new int[]{3, 4};
                                else if(upLeft && !upRight && !bottomRight && bottomLeft)
                                    uv = new int[]{3, 5};
                                else if(upLeft && upRight && !bottomRight && !bottomLeft)
                                    uv = new int[]{2, 5};
                                else if(!upLeft && upRight && bottomRight && !bottomLeft)
                                    uv = new int[]{2, 4};
                                else{
                                    if(upLeft)
                                        uv = new int[]{5, 5};
                                    else if(upRight)
                                        uv = new int[]{4, 5};
                                    else if(bottomRight)
                                        uv = new int[]{4, 4};
                                    else if(bottomLeft)
                                        uv = new int[]{5, 4};
                                    else
                                        uv = new int[]{1, 4};
                                }
                            }
                        }
                    }
                }
            }
        }

        return uv;
    }

    public static int[] getStatePositionSimple(boolean up, boolean right, boolean bottom, boolean left){
        int[] uv;

        if(!left && !up && !right && !bottom) // none
            uv = new int[]{0, 0};
        else{ // one direction
            if(left && !up && !right && !bottom)
                uv = new int[]{3, 0};
            else if(!left && up && !right && !bottom)
                uv = new int[]{3, 1};
            else if(!left && !up && right && !bottom)
                uv = new int[]{2, 1};
            else if(!left && !up && !right && bottom)
                uv = new int[]{2, 0};
            else{ // two directions
                if(left && !up && right && !bottom)
                    uv = new int[]{0, 1};
                else if(!left && up && !right && bottom)
                    uv = new int[]{1, 1};
                else if(left && up && !right && !bottom)
                    uv = new int[]{3, 3};
                else if(!left && up && right && !bottom)
                    uv = new int[]{2, 3};
                else if(!left && !up && right && bottom)
                    uv = new int[]{2, 2};
                else if(left && !up && !right && bottom)
                    uv = new int[]{3, 2};
                else{ // three directions
                    if(!left)
                        uv = new int[]{0, 2};
                    else if(!up)
                        uv = new int[]{1, 2};
                    else if(!right)
                        uv = new int[]{1, 3};
                    else if(!bottom)
                        uv = new int[]{0, 3};
                    else // four directions
                        uv = new int[]{1, 0};
                }
            }
        }

        return uv;
    }
}
