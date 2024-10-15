package com.supermartijn642.fusion.texture.types.connecting;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.texture.*;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureData;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureLayout;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.texture.types.connecting.layouts.ConnectingTextureLayoutHandler;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.util.Mth;

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
    public Pair<Integer,Integer> getFrameSize(SpritePreparationContext context, ConnectingTextureData data){
        // Legacy full layout was a square image, so change the framing to the new aspect ratio
        if(data.getLayout() == ConnectingTextureLayout.FULL && context.getTextureWidth() == context.getTextureHeight())
            return Pair.of(context.getTextureWidth(), context.getTextureHeight() * 6 / 8);

        // Handle animation metadata
        if(context.getAnimationMetadata() != null){
            AnimationMetadataSection animation = context.getAnimationMetadata();
            Pair<Integer,Integer> frameSize;
            if(animation.frameWidth != -1 && animation.frameHeight != -1)
                //noinspection SuspiciousNameCombination
                frameSize = Pair.of(animation.frameWidth, animation.frameHeight);
            else if(animation.frameWidth != -1)
                frameSize = Pair.of(animation.frameWidth, context.getTextureHeight());
            else if(animation.frameHeight != -1)
                //noinspection SuspiciousNameCombination
                frameSize = Pair.of(context.getTextureWidth(), animation.frameHeight);
            else{
                // Use the expected aspect ratio for the layout
                ConnectingTextureLayoutHandler handler = ConnectingTextureLayoutHandler.get(data.getLayout());
                int height = Math.min(context.getTextureWidth() / handler.getWidth() * handler.getHeight(), context.getTextureHeight());
                //noinspection SuspiciousNameCombination
                frameSize = Pair.of(context.getTextureWidth(), height);
            }
            // Do vanilla frame size check
            if(!Mth.isDivisionInteger(context.getTextureWidth(), frameSize.left())
                || !Mth.isDivisionInteger(context.getTextureHeight(), frameSize.right()))
                throw new TextureErrorException("Image size {},{} is not a multiple of frame size {},{}");
            return frameSize;
        }

        return Pair.of(context.getTextureWidth(), context.getTextureHeight());
    }

    @Override
    public TextureAtlasSprite createSprite(SpriteCreationContext context, ConnectingTextureData data){
        ConnectingTextureLayoutHandler layoutHandler = ConnectingTextureLayoutHandler.get(data.getLayout());
        TextureAtlasSprite sprite = context.createOriginalSprite();
        float startU = sprite.u0, startV = sprite.v0;
        float tileWidth = (sprite.u1 - sprite.u0) / layoutHandler.getWidth();
        float tileHeight = (sprite.v1 - sprite.v0) / layoutHandler.getHeight();
        sprite.u1 = sprite.u0 + tileWidth * (layoutHandler.defaultTileX() + 1);
        sprite.v1 = sprite.v0 + tileHeight * (layoutHandler.defaultTileY() + 1);
        sprite.u0 = sprite.u0 + tileWidth * layoutHandler.defaultTileX();
        sprite.v0 = sprite.v0 + tileHeight * layoutHandler.defaultTileY();
        return new ConnectingTextureSprite(sprite, data, startU, startV);
    }
}
