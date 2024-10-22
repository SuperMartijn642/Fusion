package com.supermartijn642.fusion.texture.types.random;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.texture.*;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.api.texture.data.RandomTextureData;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.model.MutableQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Random;

/**
 * Created 22/10/2024 by SuperMartijn642
 */
public class RandomTextureType implements TextureType<RandomTextureData> {

    @Override
    public RandomTextureData deserialize(JsonObject json) throws JsonParseException{
        // Deserialize base properties
        BaseTextureData base = DefaultTextureTypes.BASE.deserialize(json);
        // Copy base properties
        RandomTextureData.Builder builder = RandomTextureData.builder();
        builder.renderType(base.getRenderType());
        builder.emissive(base.isEmissive());
        builder.tinting(base.getTinting());
        // Rows
        int rows = 1;
        if(json.has("rows")){
            if(!json.get("rows").isJsonPrimitive() || !json.getAsJsonPrimitive("rows").isNumber())
                throw new JsonParseException("Property 'rows' must be a number!");
            rows = json.get("rows").getAsInt();
            if(rows < 1 || rows > 10)
                throw new JsonParseException("Property 'rows' must be a number between 1 and 10!");
            builder.rows(rows);
        }
        // Columns
        int columns = 1;
        if(json.has("columns")){
            if(!json.get("columns").isJsonPrimitive() || !json.getAsJsonPrimitive("columns").isNumber())
                throw new JsonParseException("Property 'columns' must be a number!");
            columns = json.get("columns").getAsInt();
            if(columns < 1 || columns > 10)
                throw new JsonParseException("Property 'columns' must be a number between 1 and 10!");
            builder.columns(columns);
        }
        // Count
        if(json.has("count")){
            if(!json.get("count").isJsonPrimitive() || !json.getAsJsonPrimitive("count").isNumber())
                throw new JsonParseException("Property 'count' must be a number!");
            int count = json.get("count").getAsInt();
            if(count < 1 || count > 100)
                throw new JsonParseException("Property 'count' must be a number between 1 and 100!");
            if(count > rows * columns)
                throw new IllegalArgumentException("Count cannot be greater than rows * columns!");
            builder.count(count);
        }
        // Seed
        if(json.has("seed")){
            if(!json.get("seed").isJsonPrimitive() || !json.getAsJsonPrimitive("seed").isNumber())
                throw new JsonParseException("Property 'seed' must be a number!");
            builder.seed(json.get("seed").getAsLong());
        }
        return builder.build();
    }

    @Override
    public JsonObject serialize(RandomTextureData data){
        // Serialize base properties
        JsonObject json = DefaultTextureTypes.BASE.serialize(data);
        // Rows
        if(data.getRows() != 1)
            json.addProperty("rows", data.getRows());
        // Columns
        if(data.getColumns() != 1)
            json.addProperty("columns", data.getColumns());
        // Count
        if(data.getCount() != data.getRows() * data.getColumns())
            json.addProperty("count", data.getCount());
        // Seed
        if(data.getSeed() != null)
            json.addProperty("seed", data.getSeed());
        return json;
    }

    @Override
    public Pair<Integer,Integer> getFrameSize(SpritePreparationContext context, RandomTextureData data){
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
    public TextureAtlasSprite createSprite(SpriteCreationContext context, RandomTextureData data){
        TextureAtlasSprite sprite = context.createOriginalSprite();
        sprite.u1 = sprite.u0 + (sprite.u1 - sprite.u0) / data.getColumns();
        sprite.v1 = sprite.v0 + (sprite.v1 - sprite.v0) / data.getRows();
        return new RandomTextureSprite(sprite, data);
    }

    public static void processQuad(MutableQuad quad, BlockPos pos, Direction side, Random random, RandomTextureSprite sprite){
        if(side == null)
            return;
        RandomTextureData data = sprite.data();
        // Determine which tile to use based on position and side
        if(data.getSeed() != null)
            random.setSeed(data.getSeed() ^ ((pos.asLong() + 1) * side.ordinal()));
        else
            random.setSeed((pos.asLong() + 1) * side.ordinal());
        random.nextLong(); // Neighboring blocks may lead to similar seeds, hence generate long first to increase randomness
        int x = random.nextInt(data.getColumns());
        int y = random.nextInt(data.getRows());
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
