package com.supermartijn642.fusion.texture.types.random;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.platform.NativeImage;
import com.supermartijn642.fusion.api.model.custom.quad.EmittableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.RawTextureInstance;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.*;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.api.texture.types.random.RandomTextureData;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.api.util.PropertyStore;
import com.supermartijn642.fusion.api.util.UserErrorException;
import com.supermartijn642.fusion.texture.DummyTextureSpriteContents;
import com.supermartijn642.fusion.texture.TextureTypeRegistryImpl;
import com.supermartijn642.fusion.texture.types.base.BaseTextureType;
import com.supermartijn642.fusion.util.SeedHelper;
import com.supermartijn642.fusion.util.Triple;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Created 22/10/2024 by SuperMartijn642
 */
public class RandomTextureType implements TextureType<RandomTextureData,StitchedRandomTextureData> {

    @Override
    public void createTexture(TextureOutput<StitchedRandomTextureData> output, TextureCreationContext context, RandomTextureData data) throws UserErrorException{
        // Calculate frame size
        int frameWidth = context.getImageWidth(), frameHeight = context.getImageHeight();
        int defaultTileSize = Math.min(context.getImageWidth() / data.getColumns(), context.getImageHeight() / data.getRows());
        AnimationMetadataSection animationMetadata = context.getAnimationMetadata();
        if(animationMetadata != null){
            if(animationMetadata.frameWidth().isEmpty() && animationMetadata.frameHeight().isEmpty()){
                // Use the expected aspect ratio for the layout
                frameWidth = data.getColumns() * defaultTileSize;
                frameHeight = data.getRows() * defaultTileSize;
            }else{
                if(animationMetadata.frameWidth().isPresent())
                    frameWidth = animationMetadata.frameWidth().get();
                if(animationMetadata.frameHeight().isPresent())
                    frameHeight = animationMetadata.frameHeight().get();
            }
        }

        // Do frame size checks
        if(frameWidth == 0 || frameHeight == 0)
            throw new UserErrorException("Image must not be empty!");
        if(context.getImageWidth() % frameWidth != 0 || context.getImageHeight() % frameHeight != 0)
            throw new UserErrorException("Image size " + context.getImageWidth() + "x" + context.getImageHeight() + " is not a multiple of frame size " + frameWidth + "x" + frameHeight + "!");
        if(frameWidth % data.getColumns() != 0 || frameHeight % data.getRows() != 0)
            throw new UserErrorException("Image/frame size " + context.getImageWidth() + "x" + context.getImageHeight() + " is not a multiple of number of columns " + data.getColumns() + " and rows " + data.getRows() + "!");

        // Convert animation data for tiles
        if(animationMetadata != null){
            if(data.perTileAnimation()){
                frameWidth = context.getImageWidth();
                frameHeight = context.getImageHeight();
            }
            animationMetadata = new AnimationMetadataSection(
                animationMetadata.frames(),
                animationMetadata.frameWidth().map(w -> w / data.getColumns()),
                animationMetadata.frameHeight().map(h -> h / data.getRows()),
                animationMetadata.defaultFrameTime(),
                animationMetadata.interpolatedFrames()
            );
        }

        // Get sub-texture
        RawTextureInstance<?,?> rawSubTexture = data.subTexture();
        if(rawSubTexture == null)
            rawSubTexture = RawTextureInstance.of(DefaultTextureTypes.BASE, data);

        // Create tiles
        int tileWidth = frameWidth / data.getColumns();
        int tileHeight = frameHeight / data.getRows();
        boolean isEmpty = true;
        List<TextureInstance<?>> tiles = new ArrayList<>(data.getRows() * data.getColumns());
        try(NativeImage image = context.getImage()){
            for(int y = 0; y < data.getRows(); y++){
                for(int x = 0; x < data.getColumns(); x++){
                    // Skip empty tiles
                    if(DummyTextureSpriteContents.isSubImageEmpty(context.getImage(), x * tileWidth, y * tileHeight, tileWidth, tileHeight))
                        continue;
                    isEmpty = false;
                    NativeImage subImage = ImageHelper.createCropFramed(context.getImage(), x * tileWidth, y * tileHeight, tileWidth, tileHeight, frameWidth, frameHeight, false);
                    try(subImage){
                        output.createSubTexture(
                                rawSubTexture,
                                "tile_" + (y * data.getColumns() + x),
                                subImage,
                                animationMetadata
                            )
                            .setCreationCallback(tiles::add)
                            .submit();
                    }
                }
            }
        }
        if(isEmpty)
            throw new UserErrorException("Image is completely empty!");

        // Set custom texture data
        output.setCustomData(new StitchedRandomTextureData(
            data.getRenderType(),
            data.isEmissive(),
            data.getTinting(),
            data.getRows(),
            data.getColumns(),
            data.getRandomSource(),
            data.getSeed(),
            tiles
        ));
    }

    @Override
    public @Nullable BlockStateQuadProcessor<?> initializeBlockStateModelQuad(MutableQuad quad, SpriteInstance sprite, StitchedRandomTextureData data, PropertyStore properties){
        // Apply base texture properties
        BaseTextureType.applyProperties(quad, data);

        // Initialize each tile
        List<TextureInstance<?>> subTextures = data.getSubTextures();
        QuadAccess[] subQuads = new QuadAccess[subTextures.size()];
        SpriteInstance[] subSprites = new SpriteInstance[subTextures.size()];
        //noinspection unchecked
        BlockStateQuadProcessor<Object>[] subProcessors = new BlockStateQuadProcessor[subTextures.size()];
        for(int i = 0; i < subTextures.size(); i++){
            TextureInstance<?> subTexture = subTextures.get(i);
            MutableQuad subQuad = quad.createCopy();
            // Adjust the quad's uv
            SpriteInstance defaultSprite = subTexture.getDefaultSprite();
            for(int j = 0; j < 4; j++){
                subQuad.uv(
                    j,
                    defaultSprite.getU0() + (quad.u(j) - sprite.getU0()) / (sprite.getU1() - sprite.getSprite().getU0()) * (defaultSprite.getU1() - defaultSprite.getU0()),
                    defaultSprite.getV0() + (quad.v(j) - sprite.getV0()) / (sprite.getV1() - sprite.getSprite().getV0()) * (defaultSprite.getV1() - defaultSprite.getV0())
                );
            }
            subQuad.sprite(defaultSprite.getSprite());
            // Initialize sub quad
            BlockStateQuadProcessor<?> subProcessor = subTexture.initializeBlockStateModelQuad(subQuad, defaultSprite, properties);
            SpriteInstance newSprite = SpriteHelper.getSpriteInstance(subQuad.sprite());
            subSprites[i] = newSprite == null ? defaultSprite : newSprite;
            subQuads[i] = subQuad;
            //noinspection unchecked
            subProcessors[i] = (BlockStateQuadProcessor<Object>)subProcessor;
        }

        // Create processor
        Direction side = quad.facing();
        return new BlockStateQuadProcessor<Pair<Integer,Object>>() {
            @Override
            public Pair<Integer,Object> extractState(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state, Supplier<RandomSource> randomSupplier, PropertyStore properties){
                if(pos == null)
                    pos = BlockPos.ZERO;
                // Calculate seed
                long seed = 1;
                if(data.getRandomSource() == RandomTextureData.RandomnessSource.POSITION)
                    seed = SeedHelper.fromBlockPos(pos);
                else if(data.getRandomSource() == RandomTextureData.RandomnessSource.POSITION_FACING)
                    seed = SeedHelper.fromBlockPos(pos) * (side.ordinal() + 1);
                else if(data.getRandomSource() == RandomTextureData.RandomnessSource.POSITION_AXIS)
                    seed = SeedHelper.fromBlockPos(pos) * (side.getAxis().ordinal() + 1);
                if(data.getSeed() != null)
                    seed ^= data.getSeed();
                // Pick which tile to use
                RandomSource random = randomSupplier.get();
                random.setSeed(seed);
                random.nextLong(); // Neighboring blocks may lead to similar seeds, hence generate long first to increase randomness
                int tile = random.nextInt(subTextures.size());
                // Extract sub-texture state
                BlockStateQuadProcessor<Object> subProcessor = subProcessors[tile];
                Object subState = subProcessor == null ? null : subProcessor.extractState(level, pos, state, randomSupplier, properties);
                return Pair.of(tile, subState);
            }

            @Override
            public Object createGeometryKey(Pair<Integer,Object> state, PropertyStore properties){
                BlockStateQuadProcessor<Object> subProcessor = subProcessors[state.left()];
                if(subProcessor == null)
                    return Pair.of(sprite, state.left());
                Object subKey = subProcessor.createGeometryKey(state.right(), properties);
                return subKey == null ? null : Triple.of(sprite, state.left(), subKey);
            }

            @Override
            public void processQuad(EmittableQuad quad, SpriteInstance sprite, Pair<Integer,Object> state, PropertyStore properties){
                quad.copyFrom(subQuads[state.left()]);
                BlockStateQuadProcessor<Object> subProcessor = subProcessors[state.left()];
                if(subProcessor == null)
                    quad.emit();
                else
                    subProcessor.processQuad(quad, subSprites[state.left()], state, properties);
            }
        };
    }

    @Override
    public @Nullable ItemQuadProcessor<?> initializeItemModelQuad(MutableQuad quad, SpriteInstance sprite, StitchedRandomTextureData data, PropertyStore properties){
        // Apply base texture properties
        BaseTextureType.applyProperties(quad, data);

        // Process quad as if at position zero
        BlockPos pos = BlockPos.ZERO;
        Direction side = quad.facing();
        // Calculate seed
        long seed = 1;
        if(data.getRandomSource() == RandomTextureData.RandomnessSource.POSITION)
            seed = SeedHelper.fromBlockPos(pos);
        else if(data.getRandomSource() == RandomTextureData.RandomnessSource.POSITION_FACING)
            seed = SeedHelper.fromBlockPos(pos) * (side.ordinal() + 1);
        else if(data.getRandomSource() == RandomTextureData.RandomnessSource.POSITION_AXIS)
            seed = SeedHelper.fromBlockPos(pos) * (side.getAxis().ordinal() + 1);
        if(data.getSeed() != null)
            seed ^= data.getSeed();
        // Pick which tile to use
        RandomSource random = RandomSource.createThreadLocalInstance(seed);
        random.nextLong(); // Neighboring blocks may lead to similar seeds, hence generate long first to increase randomness
        int index = random.nextInt(data.getSubTextures().size());

        // Initialize the tile
        TextureInstance<?> subTexture = data.getSubTextures().get(index);
        MutableQuad subQuad = quad.createCopy();
        // Adjust the quad's uv
        SpriteInstance defaultSprite = subTexture.getDefaultSprite();
        for(int j = 0; j < 4; j++){
            quad.uv(
                j,
                defaultSprite.getU0() + (quad.u(j) - sprite.getU0()) / (sprite.getU1() - sprite.getSprite().getU0()) * (defaultSprite.getU1() - defaultSprite.getU0()),
                defaultSprite.getV0() + (quad.v(j) - sprite.getV0()) / (sprite.getV1() - sprite.getSprite().getV0()) * (defaultSprite.getV1() - defaultSprite.getV0())
            );
        }
        // Initialize sub quad
        //noinspection unchecked
        ItemQuadProcessor<Object> subProcessor = (ItemQuadProcessor<Object>)subTexture.initializeItemModelQuad(subQuad, defaultSprite, properties);
        if(subProcessor == null)
            return null;
        SpriteInstance newSprite = SpriteHelper.getSpriteInstance(quad.sprite());
        SpriteInstance subSprite = newSprite == null ? defaultSprite : newSprite;
        return new ItemQuadProcessor<>() {
            @Override
            public Object extractState(ItemStack stack, PropertyStore properties){
                return subProcessor.extractState(stack, properties);
            }

            @Override
            public Object createGeometryKey(Object state, PropertyStore properties){
                Object subKey = subProcessor.createGeometryKey(state, properties);
                if(subKey == null)
                    return null;
                return Pair.of(sprite, subKey);
            }

            @Override
            public void processQuad(EmittableQuad quad, SpriteInstance sprite, Object state, PropertyStore properties){
                quad.copyFrom(subQuad);
                subProcessor.processQuad(quad, subSprite, state, properties);
            }
        };
    }

    public static final int MAX_SIZE = 100;

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
        // Sub-texture
        if(json.has("sub_texture")){
            if(!json.get("sub_texture").isJsonObject())
                throw new JsonParseException("Property 'sub_texture' must be an object!");
            try{
                builder.subTexture(TextureTypeRegistryImpl.deserializeTextureData(json.get("sub_texture").getAsJsonObject()));
            }catch(JsonParseException e){
                throw new JsonParseException("Failed to deserialize sub texture!");
            }
        }
        // Per tile animation
        if(json.has("per_tile_animation")){
            if(!json.get("per_tile_animation").isJsonPrimitive() || !json.getAsJsonPrimitive("per_tile_animation").isBoolean())
                throw new JsonParseException("Property 'per_tile_animation' must be a boolean!");
            builder.perTileAnimation(json.get("per_tile_animation").getAsBoolean());
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
        // Sub-texture
        if(data.subTexture() != null && data.subTexture().getTextureType() != DefaultTextureTypes.VANILLA)
            json.add("sub_texture", TextureTypeRegistryImpl.serializeTextureData(data.subTexture()));
        // Per tile animation
        if(data.perTileAnimation())
            json.addProperty("per_tile_animation", true);
        return json;
    }
}
