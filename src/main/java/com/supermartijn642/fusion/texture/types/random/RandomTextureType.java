package com.supermartijn642.fusion.texture.types.random;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.platform.NativeImage;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.*;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.api.texture.data.RandomTextureData;
import com.supermartijn642.fusion.model.MutableQuad;
import com.supermartijn642.fusion.texture.DummyTextureSpriteContents;
import net.minecraft.client.resources.metadata.animation.AnimationFrame;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created 22/10/2024 by SuperMartijn642
 */
public class RandomTextureType implements TextureType<RandomTextureData,RandomTextureData> {

    @Override
    public void createTexture(TextureOutput<RandomTextureData> output, TextureCreationContext context, RandomTextureData data) throws TextureErrorException{
        // Calculate frame size
        int frameWidth = context.getImageWidth(), frameHeight = context.getImageHeight();
        int defaultTileSize = Math.min(context.getImageWidth() / data.getColumns(), context.getImageHeight() / data.getRows());
        AnimationMetadataSection animationMetadata = context.getAnimationMetadata();
        if(animationMetadata != null){
            if(animationMetadata.frameWidth < 0 && animationMetadata.frameHeight < 0){
                // Use the expected aspect ratio for the layout
                frameWidth = data.getColumns() * defaultTileSize;
                frameHeight = data.getRows() * defaultTileSize;
            }else{
                if(animationMetadata.frameWidth >= 0)
                    frameWidth = animationMetadata.frameWidth;
                if(animationMetadata.frameHeight >= 0)
                    frameHeight = animationMetadata.frameHeight;
            }
        }

        // Do frame size checks
        if(frameWidth == 0 || frameHeight == 0)
            throw new TextureErrorException("Image must not be empty!");
        if(context.getImageWidth() % frameWidth != 0 || context.getImageHeight() % frameHeight != 0)
            throw new TextureErrorException("Image size " + context.getImageWidth() + "x" + context.getImageHeight() + " is not a multiple of frame size " + frameWidth + "x" + frameHeight + "!");
        if(frameWidth % data.getColumns() != 0 || frameHeight % data.getRows() != 0)
            throw new TextureErrorException("Image/frame size " + context.getImageWidth() + "x" + context.getImageHeight() + " is not a multiple of number of columns " + data.getColumns() + " and rows " + data.getRows() + "!");

        // Create animation data
        int frameColumns = context.getImageWidth() / frameWidth;
        int frameRows = context.getImageHeight() / frameHeight;
        int tileWidth = frameWidth / data.getColumns();
        int tileHeight = frameHeight / data.getRows();
        List<SpriteImageSource.AnimationFrame> frames = null;
        if(animationMetadata != null){
            if(!animationMetadata.frames.isEmpty()){
                frames = new ArrayList<>(animationMetadata.frames.size());
                for(AnimationFrame frame : animationMetadata.frames){
                    int index = frame.getIndex();
                    if(index >= frameRows * frameColumns)
                        throw new TextureErrorException("Frame index " + index + " is greater than the number of frames in the image!");
                    int x = tileWidth * (index % frameColumns);
                    int y = tileHeight * (index / frameColumns);
                    frames.add(SpriteImageSource.AnimationFrame.of(x, y, frame.getTime(animationMetadata.getDefaultFrameTime())));
                }
            }else{
                frames = new ArrayList<>(frameRows * frameColumns);
                for(int row = 0; row < frameRows; row++){
                    for(int column = 0; column < frameColumns; column++){
                        frames.add(SpriteImageSource.AnimationFrame.of(column * tileWidth, row * tileHeight, animationMetadata.getDefaultFrameTime()));
                    }
                }
            }
            if(frameRows == 1 && frameColumns == 1) // If there is only a single frame, ignore the animation data but still validate it
                frames = null;
        }

        // Create sprites
        int tiles = 0;
        try(NativeImage image = context.getImage()){
            for(int y = 0; y < data.getRows(); y++){
                for(int x = 0; x < data.getColumns(); x++){
                    // Skip empty tiles
                    if(DummyTextureSpriteContents.isSubImageEmpty(context.getImage(), x * tileWidth, y * tileHeight, tileWidth, tileHeight))
                        continue;
                    NativeImage subImage = ImageHelper.createCropFramed(image, x * tileWidth, y * tileHeight, tileWidth, tileHeight, frameWidth, frameHeight, false);
                    SpriteImageSource imageSource = frames == null ?
                        SpriteImageSource.constant(subImage) :
                        SpriteImageSource.animated(subImage, tileWidth, tileHeight, frames, animationMetadata.isInterpolatedFrames());
                    output.createSprite()
                        .image(imageSource)
                        .submit();
                    tiles++;
                }
            }
        }
        if(tiles == 0)
            throw new TextureErrorException("Image is completely empty!");

        // Set custom texture data
        output.setCustomData(data);
    }

    public static final int MAX_SIZE = 10;

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
        int rows;
        if(json.has("rows")){
            if(!json.get("rows").isJsonPrimitive() || !json.getAsJsonPrimitive("rows").isNumber())
                throw new JsonParseException("Property 'rows' must be a number!");
            rows = json.get("rows").getAsInt();
            if(rows < 1 || rows > MAX_SIZE)
                throw new JsonParseException("Property 'rows' must be a number between 1 and 10!");
            builder.rows(rows);
        }
        // Columns
        int columns;
        if(json.has("columns")){
            if(!json.get("columns").isJsonPrimitive() || !json.getAsJsonPrimitive("columns").isNumber())
                throw new JsonParseException("Property 'columns' must be a number!");
            columns = json.get("columns").getAsInt();
            if(columns < 1 || columns > MAX_SIZE)
                throw new JsonParseException("Property 'columns' must be a number between 1 and 10!");
            builder.columns(columns);
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
        // Seed
        if(data.getSeed() != null)
            json.addProperty("seed", data.getSeed());
        return json;
    }

    public static void processQuad(MutableQuad quad, BlockPos pos, Direction side, Random random, SpriteInstance sprite){
        if(side == null)
            return;
        RandomTextureData data = (RandomTextureData)sprite.getTexture().getCustomData();
        // Determine which tile to use based on position and side
        if(data.getSeed() != null)
            random.setSeed(data.getSeed() ^ ((pos.asLong() + 1) * (side.ordinal() + 1)));
        else
            random.setSeed((pos.asLong() + 1) * (side.ordinal() + 1));
        random.nextLong(); // Neighboring blocks may lead to similar seeds, hence generate long first to increase randomness
        int index = random.nextInt(sprite.getTexture().getSprites().size());
        // Adjust the quad's uv
        SpriteInstance newSprite = sprite.getTexture().getSprites().get(index);
        for(int i = 0; i < 4; i++){
            quad.uv(
                i,
                newSprite.getU0() + (quad.u(i) - sprite.getU0()) / (sprite.getU1() - sprite.getSprite().getU0()) * (newSprite.getU1() - newSprite.getU0()),
                newSprite.getV0() + (quad.v(i) - sprite.getV0()) / (sprite.getV1() - sprite.getSprite().getV0()) * (newSprite.getV1() - newSprite.getV0())
            );
        }
    }
}
