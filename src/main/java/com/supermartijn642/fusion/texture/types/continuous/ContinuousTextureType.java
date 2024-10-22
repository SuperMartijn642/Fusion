package com.supermartijn642.fusion.texture.types.continuous;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.texture.*;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.api.texture.data.ContinuousTextureData;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.model.MutableQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Created 22/10/2024 by SuperMartijn642
 */
public class ContinuousTextureType implements TextureType<ContinuousTextureData> {

    @Override
    public ContinuousTextureData deserialize(JsonObject json) throws JsonParseException{
        // Deserialize base properties
        BaseTextureData base = DefaultTextureTypes.BASE.deserialize(json);
        // Copy base properties
        ContinuousTextureData.Builder builder = ContinuousTextureData.builder();
        builder.renderType(base.getRenderType());
        builder.emissive(base.isEmissive());
        builder.tinting(base.getTinting());
        // Rows
        if(json.has("rows")){
            if(!json.get("rows").isJsonPrimitive() || !json.getAsJsonPrimitive("rows").isNumber())
                throw new JsonParseException("Property 'rows' must be a number!");
            int rows = json.get("rows").getAsInt();
            if(rows <= 0 || rows > 10)
                throw new JsonParseException("Property 'rows' must be greater than zero and less than 10!");
            builder.rows(rows);
        }
        // Columns
        if(json.has("columns")){
            if(!json.get("columns").isJsonPrimitive() || !json.getAsJsonPrimitive("columns").isNumber())
                throw new JsonParseException("Property 'columns' must be a number!");
            int columns = json.get("columns").getAsInt();
            if(columns <= 0 || columns > 10)
                throw new JsonParseException("Property 'columns' must be greater than zero and less than 10!");
            builder.columns(columns);
        }
        return builder.build();
    }

    @Override
    public JsonObject serialize(ContinuousTextureData data){
        // Serialize base properties
        JsonObject json = DefaultTextureTypes.BASE.serialize(data);
        // Rows
        if(data.getRows() != 1)
            json.addProperty("rows", data.getRows());
        // Columns
        if(data.getColumns() != 1)
            json.addProperty("columns", data.getColumns());
        return json;
    }

    @Override
    public Pair<Integer,Integer> getFrameSize(SpritePreparationContext context, ContinuousTextureData data){
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
                int height = Math.min(context.getTextureWidth() * data.getRows() / data.getColumns(), context.getTextureHeight());
                //noinspection SuspiciousNameCombination
                frameSize = Pair.of(context.getTextureWidth(), height);
            }
            // Do vanilla frame size check
            if(context.getTextureWidth() % frameSize.left() != 0
                || context.getTextureHeight() % frameSize.right() != 0)
                throw new TextureErrorException("Image size " + context.getTextureWidth() + "x" + context.getTextureHeight() + " is not a multiple of frame size " + frameSize.left() + "x" + frameSize.right() + "!");
            return frameSize;
        }

        // Verify aspect ratio corresponds to row/column count
        int width = context.getTextureWidth();
        int height = context.getTextureHeight();
        if(width * data.getRows() / data.getColumns() != height)
            throw new TextureErrorException("Image aspect ratio does not match row/column aspect ratio!");
        //noinspection SuspiciousNameCombination
        return Pair.of(width, height);
    }

    @Override
    public TextureAtlasSprite createSprite(SpriteCreationContext context, ContinuousTextureData data){
        TextureAtlasSprite sprite = context.createOriginalSprite();
        sprite.u1 = sprite.u0 + (sprite.u1 - sprite.u0) / data.getColumns();
        sprite.v1 = sprite.v0 + (sprite.v1 - sprite.v0) / data.getRows();
        return new ContinuousTextureSprite(sprite, data);
    }

    public static void processQuad(MutableQuad quad, BlockPos pos, Direction side, ContinuousTextureSprite sprite){
        if(side == null)
            return;
        ContinuousTextureData data = sprite.data();
        // TODO account for the orientation of the texture and quad
        // Determine which tile to use
        Direction.Axis axis = side.getAxis();
        int x = axis == Direction.Axis.X ? pos.getZ() : pos.getX();
        int y = axis == Direction.Axis.X ? pos.getY() : axis == Direction.Axis.Y ? pos.getZ() : pos.getY();
        if(side == Direction.NORTH || side == Direction.EAST)
            x = -x - 1;
        x = x < 0 ? ((x % data.getColumns()) + data.getColumns()) % data.getColumns() : x % data.getColumns();
        y = y < 0 ? ((y % data.getRows()) + data.getRows()) % data.getRows() : y % data.getRows();
        // Adjust the quad's uv
        if(x > 0 || y > 0){
            float width = sprite.getU1() - sprite.getU0();
            float height = sprite.getV1() - sprite.getV0();
            for(int i = 0; i < 4; i++){
                quad.uv(
                    i,
                    quad.u(i) + x * width,
                    quad.v(i) + y * height
                );
            }
        }
    }
}
