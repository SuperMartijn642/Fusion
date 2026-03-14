package com.supermartijn642.fusion.texture.types.biome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.SpriteCreationContext;
import com.supermartijn642.fusion.api.texture.SpritePreparationContext;
import com.supermartijn642.fusion.api.texture.TextureErrorException;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.api.texture.data.BiomeTextureData;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.model.MutableQuad;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.biome.Biome;

import java.util.Comparator;
import java.util.Map;

public class BiomeTextureType implements TextureType<BiomeTextureData> {

    @Override
    public BiomeTextureData deserialize(JsonObject json) throws JsonParseException{
        // Deserialize base properties
        BaseTextureData base = DefaultTextureTypes.BASE.deserialize(json);
        // Copy base properties
        BiomeTextureData.Builder builder = BiomeTextureData.builder();
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
        // Default tile
        if(json.has("default")){
            builder.defaultTile(parseTileIndex(json.get("default"), rows, columns, "default"));
        }
        // Biomes mapping
        if(json.has("biomes")){
            if(!json.get("biomes").isJsonObject())
                throw new JsonParseException("Property 'biomes' must be an object!");
            for(Map.Entry<String,JsonElement> entry : json.getAsJsonObject("biomes").entrySet()){
                String key = entry.getKey();
                if(!IdentifierUtil.isValidIdentifier(key))
                    throw new JsonParseException("Biome entries must be a valid identifier, not '" + key + "'!");
                int tile = parseTileIndex(entry.getValue(), rows, columns, "biomes." + key);
                builder.biomeTile(Identifier.parse(key), tile);
            }
        }
        return builder.build();
    }

    @Override
    public JsonObject serialize(BiomeTextureData data){
        // Serialize base properties
        JsonObject json = DefaultTextureTypes.BASE.serialize(data);
        // Rows
        if(data.getRows() != 1)
            json.addProperty("rows", data.getRows());
        // Columns
        if(data.getColumns() != 1)
            json.addProperty("columns", data.getColumns());
        // Default tile
        if(data.getDefaultTile() != 0)
            json.addProperty("default", data.getDefaultTile());
        // Biomes mapping
        if(!data.getBiomeTiles().isEmpty()){
            JsonObject biomes = new JsonObject();
            data.getBiomeTiles().entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> biomes.addProperty(entry.getKey().toString(), entry.getValue()));
            json.add("biomes", biomes);
        }
        return json;
    }

    @Override
    public Pair<Integer,Integer> getFrameSize(SpritePreparationContext context, BiomeTextureData data){
        // Handle animation metadata
        if(context.getAnimationMetadata() != null){
            AnimationMetadataSection animation = context.getAnimationMetadata();
            Pair<Integer,Integer> frameSize;
            if(animation.frameWidth().isPresent() && animation.frameHeight().isPresent())
                frameSize = Pair.of(animation.frameWidth().get(), animation.frameHeight().get());
            else if(animation.frameWidth().isPresent())
                frameSize = Pair.of(animation.frameWidth().get(), context.getTextureHeight());
            else if(animation.frameHeight().isPresent())
                frameSize = Pair.of(context.getTextureWidth(), animation.frameHeight().get());
            else{
                // Use the expected aspect ratio for the layout
                int height = Math.min(context.getTextureWidth() * data.getRows() / data.getColumns(), context.getTextureHeight());
                //noinspection SuspiciousNameCombination
                frameSize = Pair.of(context.getTextureWidth(), height);
            }
            // Do vanilla frame size check
            if(!Mth.isMultipleOf(context.getTextureWidth(), frameSize.left())
                || !Mth.isMultipleOf(context.getTextureHeight(), frameSize.right()))
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
    public TextureAtlasSprite createSprite(SpriteCreationContext context, BiomeTextureData data){
        TextureAtlasSprite sprite = context.createOriginalSprite();
        sprite.u1 = sprite.u0 + (sprite.u1 - sprite.u0) / data.getColumns();
        sprite.v1 = sprite.v0 + (sprite.v1 - sprite.v0) / data.getRows();
        return new BiomeTextureSprite(sprite, data);
    }

    public static void processQuad(MutableQuad quad, BlockAndTintGetter blockView, BlockPos pos, Direction side, BiomeTextureSprite sprite){
        if(side == null)
            return;
        BiomeTextureData data = sprite.data();
        int tile = data.getDefaultTile();
        if(blockView != null && pos != null){
            Holder<Biome> biome = blockView.getBiomeFabric(pos);
            if(biome != null && biome.isBound() && biome.unwrapKey().isPresent()){
                Identifier id = biome.unwrapKey().get().identifier();
                Integer biomeTile = data.getBiomeTiles().get(id);
                if(biomeTile != null)
                    tile = biomeTile;
            }
        }
        int x = tile % data.getColumns();
        int y = tile / data.getColumns();
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

    private static int parseTileIndex(JsonElement element, int rows, int columns, String name){
        int max = rows * columns;
        if(element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()){
            int index = element.getAsInt();
            if(index < 0 || index >= max)
                throw new JsonParseException("Tile index for '" + name + "' must be between 0 and " + (max - 1) + "!");
            return index;
        }
        if(element.isJsonArray()){
            JsonArray array = element.getAsJsonArray();
            if(array.size() != 2)
                throw new JsonParseException("Tile coordinates for '" + name + "' must be an array of two numbers!");
            if(!array.get(0).isJsonPrimitive() || !array.get(0).getAsJsonPrimitive().isNumber()
                || !array.get(1).isJsonPrimitive() || !array.get(1).getAsJsonPrimitive().isNumber())
                throw new JsonParseException("Tile coordinates for '" + name + "' must be an array of two numbers!");
            int x = array.get(0).getAsInt();
            int y = array.get(1).getAsInt();
            if(x < 0 || x >= columns || y < 0 || y >= rows)
                throw new JsonParseException("Tile coordinates for '" + name + "' must be within the bounds of the grid!");
            return y * columns + x;
        }
        throw new JsonParseException("Tile for '" + name + "' must be a number or an array of two numbers!");
    }
}
