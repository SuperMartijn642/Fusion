package com.supermartijn642.fusion.texture.types.random;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.platform.NativeImage;
import com.supermartijn642.fusion.api.model.custom.quad.EmittableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.*;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.api.texture.types.random.RandomTextureData;
import com.supermartijn642.fusion.api.util.PropertyStore;
import com.supermartijn642.fusion.api.util.UserErrorException;
import com.supermartijn642.fusion.texture.DummyTextureSpriteContents;
import com.supermartijn642.fusion.texture.types.base.BaseTextureType;
import com.supermartijn642.fusion.util.Triple;
import net.minecraft.client.resources.metadata.animation.AnimationFrame;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

/**
 * Created 22/10/2024 by SuperMartijn642
 */
public class RandomTextureType implements TextureType<RandomTextureData,RandomTextureData> {

    @Override
    public void createTexture(TextureOutput<RandomTextureData> output, TextureCreationContext context, RandomTextureData data) throws UserErrorException{
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
            throw new UserErrorException("Image must not be empty!");
        if(context.getImageWidth() % frameWidth != 0 || context.getImageHeight() % frameHeight != 0)
            throw new UserErrorException("Image size " + context.getImageWidth() + "x" + context.getImageHeight() + " is not a multiple of frame size " + frameWidth + "x" + frameHeight + "!");
        if(frameWidth % data.getColumns() != 0 || frameHeight % data.getRows() != 0)
            throw new UserErrorException("Image/frame size " + context.getImageWidth() + "x" + context.getImageHeight() + " is not a multiple of number of columns " + data.getColumns() + " and rows " + data.getRows() + "!");

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
                        throw new UserErrorException("Frame index " + index + " is greater than the number of frames in the image!");
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
            throw new UserErrorException("Image is completely empty!");

        // Set custom texture data
        output.setCustomData(data);
    }

    @Override
    public @Nullable QuadProcessor<?> initializeModelQuad(MutableQuad quad, SpriteInstance sprite, RandomTextureData data, PropertyStore properties){
        // Apply base texture properties
        BaseTextureType.applyProperties(quad, data);

        // Calculate default index
        Direction side = quad.facing();
        int defaultIndex = calculateTileIndex(sprite, side, null, new Random(), data);

        // Create processor
        return new QuadProcessor<Integer>() {
            @Override
            public Integer extractState(Supplier<Random> randomSupplier, PropertyStore properties){
                return defaultIndex;
            }

            @Override
            public Integer extractState(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state, Supplier<Random> randomSupplier, PropertyStore properties){
                return calculateTileIndex(sprite, side, pos, randomSupplier.get(), data);
            }

            @Override
            public Integer extractState(ItemStack stack, Supplier<Random> randomSupplier, PropertyStore properties){
                return defaultIndex;
            }

            @Override
            public Object createGeometryKey(Integer state, PropertyStore properties){
                return Triple.of(DefaultTextureTypes.RANDOM, sprite, state);
            }

            @Override
            public void processQuad(EmittableQuad quad, SpriteInstance sprite, Integer state, PropertyStore properties){
                // Adjust the quad's uv
                SpriteInstance newSprite = sprite.getTexture().getSprites().get(state);
                for(int i = 0; i < 4; i++){
                    quad.uv(
                        i,
                        newSprite.getU0() + (quad.u(i) - sprite.getU0()) / (sprite.getU1() - sprite.getSprite().getU0()) * (newSprite.getU1() - newSprite.getU0()),
                        newSprite.getV0() + (quad.v(i) - sprite.getV0()) / (sprite.getV1() - sprite.getSprite().getV0()) * (newSprite.getV1() - newSprite.getV0())
                    );
                }
                quad.emit();
            }
        };
    }

    private static int calculateTileIndex(SpriteInstance sprite, Direction side, @Nullable BlockPos pos, Random random, RandomTextureData data){
        if(pos == null)
            pos = BlockPos.ZERO;
        // Calculate seed
        long seed = 1;
        if(data.getRandomSource() == RandomTextureData.RandomnessSource.POSITION)
            seed = pos.asLong() + 1;
        else if(data.getRandomSource() == RandomTextureData.RandomnessSource.POSITION_FACING)
            seed = (pos.asLong() + 1) * (side.ordinal() + 1);
        else if(data.getRandomSource() == RandomTextureData.RandomnessSource.POSITION_AXIS)
            seed = (pos.asLong() + 1) * (side.getAxis().ordinal() + 1);
        if(data.getSeed() != null)
            seed ^= data.getSeed();
        // Pick which tile to use
        random.setSeed(seed);
        random.nextLong(); // Neighboring blocks may lead to similar seeds, hence generate long first to increase randomness
        return random.nextInt(sprite.getTexture().getSprites().size());
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
        if(json.has("rows")){
            if(!json.get("rows").isJsonPrimitive() || !json.getAsJsonPrimitive("rows").isNumber())
                throw new JsonParseException("Property 'rows' must be a number!");
            int rows = json.get("rows").getAsInt();
            if(rows < 1 || rows > MAX_SIZE)
                throw new JsonParseException("Property 'rows' must be a number between 1 and 10!");
            builder.rows(rows);
        }
        // Columns
        if(json.has("columns")){
            if(!json.get("columns").isJsonPrimitive() || !json.getAsJsonPrimitive("columns").isNumber())
                throw new JsonParseException("Property 'columns' must be a number!");
            int columns = json.get("columns").getAsInt();
            if(columns < 1 || columns > MAX_SIZE)
                throw new JsonParseException("Property 'columns' must be a number between 1 and 10!");
            builder.columns(columns);
        }
        // Randomness source
        if(json.has("random_source")){
            if(!json.get("random_source").isJsonPrimitive() || !json.getAsJsonPrimitive("random_source").isString())
                throw new JsonParseException("Property 'random_source' must be a string!");
            String randomSourceString = json.get("random_source").getAsString();
            RandomTextureData.RandomnessSource randomSource;
            try{
                randomSource = RandomTextureData.RandomnessSource.valueOf(randomSourceString.toUpperCase(Locale.ROOT));
            }catch(IllegalArgumentException e){
                throw new JsonParseException("Property 'random_source' must be one of " + Arrays.toString(RandomTextureData.RandomnessSource.values()).toLowerCase(Locale.ROOT) + ", not '" + randomSourceString + "'!");
            }
            builder.randomSource(randomSource);
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
        if(data.getRandomSource() != RandomTextureData.RandomnessSource.POSITION_FACING)
            json.addProperty("random_source", data.getRandomSource().toString().toLowerCase(Locale.ROOT));
        // Seed
        if(data.getSeed() != null)
            json.addProperty("seed", data.getSeed());
        return json;
    }
}
