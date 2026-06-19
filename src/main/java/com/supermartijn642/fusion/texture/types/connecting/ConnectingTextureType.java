package com.supermartijn642.fusion.texture.types.connecting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.model.custom.DefaultModelProperties;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.quad.EmittableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.model.types.connecting.ConnectingModelData;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.RawTextureInstance;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.*;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.api.texture.types.connecting.ConnectingTextureData;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionDirection;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionPredicate;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.DefaultConnectionPredicates;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.FusionConnectionPredicateRegistry;
import com.supermartijn642.fusion.api.util.*;
import com.supermartijn642.fusion.texture.DummyTextureSpriteContents;
import com.supermartijn642.fusion.texture.TextureTypeRegistryImpl;
import com.supermartijn642.fusion.texture.types.base.BaseTextureType;
import com.supermartijn642.fusion.texture.types.connecting.layouts.ConnectingTextureLayoutHandler;
import com.supermartijn642.fusion.util.Triple;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
public class ConnectingTextureType implements TextureType<ConnectingTextureData,StitchedConnectingTextureData> {

    private static final ConnectionPredicate FALLBACK_PREDICATE = DefaultConnectionPredicates.isSameState();

    private static final Property<SurroundingBlockCache,Void> SURROUNDING_BLOCKS = Property.create();
    private static final Property<TextureConnections,QuadPredicatesKey> PREDICATES_CACHE = Property.create(QuadPredicatesKey.class);

    @Override
    public void createTexture(TextureOutput<StitchedConnectingTextureData> output, TextureCreationContext context, ConnectingTextureData data) throws UserErrorException{
        ConnectingTextureLayoutHandler layout = ConnectingTextureLayoutHandler.get(data.getLayout());
        int imageWidth = context.getImageWidth(), imageHeight = context.getImageHeight();
        NativeImage image = context.getImage();

        // Calculate frame size
        int frameWidth = context.getImageWidth(), frameHeight = context.getImageHeight();
        int defaultTileSize = Math.min(frameWidth / layout.getWidth(), frameHeight / layout.getHeight());
        AnimationMetadataSection animationMetadata = context.getAnimationMetadata();
        if(data.getLayout() == ConnectingTextureData.Layout.FULL && frameWidth == frameHeight){ // Legacy full layout was a square image, so change the framing to the new aspect ratio
            if(animationMetadata != null)
                throw new UserErrorException("Image must use the 'full' layout's 6 : 8 aspect ratio to support animation!");
            frameHeight = frameHeight * 6 / 8;
            imageHeight = imageHeight * 6 / 8;
            image = ImageHelper.createCrop(image, 0, 0, imageWidth, imageHeight, true);
        }else if(animationMetadata != null){
            if(animationMetadata.frameWidth < 0 && animationMetadata.frameHeight < 0){
                // Use the expected aspect ratio for the layout
                frameWidth = layout.getWidth() * defaultTileSize;
                frameHeight = layout.getHeight() * defaultTileSize;
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
        if(imageWidth % frameWidth != 0 || imageHeight % frameHeight != 0)
            throw new UserErrorException("Image size " + imageWidth + "x" + imageHeight + " is not a multiple of frame size " + frameWidth + "x" + frameHeight + "!");
        if(frameWidth % layout.getWidth() != 0 || frameHeight % layout.getHeight() != 0)
            throw new UserErrorException("Image/frame size " + frameWidth + "x" + frameHeight + " is not a multiple of '" + data.getLayout().name().toLowerCase(Locale.ROOT) + "' layout's " + layout.getWidth() + " : " + layout.getHeight() + " aspect ratio!");

        // Convert animation data for tiles
        if(animationMetadata != null){
            if(data.perTileAnimation()){
                frameWidth = imageWidth;
                frameHeight = imageHeight;
            }
            animationMetadata = new AnimationMetadataSection(
                animationMetadata.frames,
                frameWidth / layout.getWidth(),
                frameHeight / layout.getHeight(),
                animationMetadata.getDefaultFrameTime(),
                animationMetadata.isInterpolatedFrames()
            );
        }

        // Get sub-texture
        RawTextureInstance<?,?> rawSubTexture = data.subTexture();
        if(rawSubTexture == null)
            rawSubTexture = RawTextureInstance.of(DefaultTextureTypes.BASE, data);

        // Create sprites
        int tileWidth = frameWidth / layout.getWidth();
        int tileHeight = frameHeight / layout.getHeight();
        boolean isEmpty = true;
        List<TextureInstance<?>> tiles = new ArrayList<>(layout.getWidth() * layout.getHeight());
        try(NativeImage n = image){
            for(int y = 0; y < layout.getHeight(); y++){
                for(int x = 0; x < layout.getWidth(); x++){
                    tiles.add(null);
                    // Skip empty tiles
                    if((x != layout.defaultTileX() || y != layout.defaultTileY()) &&
                        DummyTextureSpriteContents.isSubImageEmpty(image, x * tileWidth, y * tileHeight, tileWidth, tileHeight))
                        continue;
                    isEmpty = false;
                    NativeImage subImage = ImageHelper.createCropFramed(image, x * tileWidth, y * tileHeight, tileWidth, tileHeight, frameWidth, frameHeight, false);
                    int index = x + y * layout.getWidth();
                    try(NativeImage n2 = subImage){
                        output.createSubTexture(
                                rawSubTexture,
                                null,
                                subImage,
                                animationMetadata
                            )
                            .markDefault(x == layout.defaultTileX() && y == layout.defaultTileY())
                            .setCreationCallback(t -> tiles.set(index, t))
                            .submit();
                    }
                }
            }
        }
        if(isEmpty)
            throw new UserErrorException("Image is completely empty!");

        // Set custom texture data
        output.setCustomData(new StitchedConnectingTextureData(data, tiles));
    }

    @Override
    public @Nullable QuadProcessor<?> initializeModelQuad(MutableQuad quad, SpriteInstance sprite, StitchedConnectingTextureData data, PropertyStore properties){
        // Apply base texture properties
        BaseTextureType.applyProperties(quad, data);

        // Get connections predicate
        String key = properties.getProperty(DefaultModelProperties.FACE_CONNECTIONS_KEY).orElse(null);
        if(key == null)
            key = properties.getProperty(DefaultModelProperties.FACE_MATERIAL_KEY).orElse(null);
        if(key != null && key.startsWith("#"))
            key = key.substring(1);
        ConnectionPredicate predicate = key == null ? null :
            resolveConnectionsKey(
                key,
                properties,
                keys -> FusionClient.LOGGER.error("Found circular connections key chain ({})!", keys.stream().map(k -> "'#" + k + "'").collect(Collectors.joining(" -> ")))
            );
        if(predicate == null)
            predicate = data.getConnectionPredicate();
        if(predicate == null)
            predicate = FALLBACK_PREDICATE;

        // Create predicates key
        Direction facing = quad.facing();
        TextureOrientation orientation = TextureOrientation.findOrientation(quad);
        QuadPredicatesKey predicatesKey = new QuadPredicatesKey(
            facing,
            orientation,
            predicate
        );

        // Get layout handler
        ConnectingTextureLayoutHandler layoutHandler = ConnectingTextureLayoutHandler.get(data.getLayout());

        // Initialize all tiles
        List<TextureInstance<?>> tiles = data.getTiles();
        QuadAccess[] subQuads = new QuadAccess[tiles.size()];
        SpriteInstance[] subSprites = new SpriteInstance[tiles.size()];
        //noinspection unchecked
        QuadProcessor<Object>[] subProcessors = new QuadProcessor[tiles.size()];
        int processorCount = 0;
        for(int i = 0; i < tiles.size(); i++){
            TextureInstance<?> tile = tiles.get(i);
            if(tile == null)
                continue;
            MutableQuad subQuad = quad.createCopy();
            // Adjust the quad's uv
            SpriteInstance defaultSprite = tile.getDefaultSprite();
            for(int j = 0; j < 4; j++){
                subQuad.uv(
                    j,
                    defaultSprite.getU0() + (quad.u(j) - sprite.getU0()) / (sprite.getU1() - sprite.getSprite().getU0()) * (defaultSprite.getU1() - defaultSprite.getU0()),
                    defaultSprite.getV0() + (quad.v(j) - sprite.getV0()) / (sprite.getV1() - sprite.getSprite().getV0()) * (defaultSprite.getV1() - defaultSprite.getV0())
                );
            }
            subQuad.sprite(defaultSprite.getSprite());
            // Initialize sub quad
            QuadProcessor<?> subProcessor = tile.initializeModelQuad(subQuad, defaultSprite, properties);
            SpriteInstance newSprite = SpriteHelper.getSpriteInstance(subQuad.sprite());
            subSprites[i] = newSprite == null ? defaultSprite : newSprite;
            subQuads[i] = subQuad;
            if(subProcessor != null){
                //noinspection unchecked
                subProcessors[i] = (QuadProcessor<Object>)subProcessor;
                processorCount++;
            }
        }
        int finalProcessorCount = processorCount;

        // Evaluate predicate for 'empty' world
        BlockState air = Blocks.AIR.defaultBlockState();
        TextureConnections contextlessConnections = new TextureConnections(
            predicate.shouldConnect(facing, null, air, air, ConnectionDirection.TOP),
            predicate.shouldConnect(facing, null, air, air, ConnectionDirection.TOP_RIGHT),
            predicate.shouldConnect(facing, null, air, air, ConnectionDirection.RIGHT),
            predicate.shouldConnect(facing, null, air, air, ConnectionDirection.BOTTOM_RIGHT),
            predicate.shouldConnect(facing, null, air, air, ConnectionDirection.BOTTOM),
            predicate.shouldConnect(facing, null, air, air, ConnectionDirection.BOTTOM_LEFT),
            predicate.shouldConnect(facing, null, air, air, ConnectionDirection.LEFT),
            predicate.shouldConnect(facing, null, air, air, ConnectionDirection.TOP_LEFT)
        );

        // Find which tiles get used when there's no context
        BitSet contextlessTiles = new BitSet();
        EmittableQuad dummyEmitter = EmittableQuad.create(q -> {});
        dummyEmitter.copyFrom(quad);
        orientation.applyVertexPermutation(dummyEmitter);
        layoutHandler.processQuad(
            dummyEmitter,
            (tile, emitter) -> contextlessTiles.set(tile),
            contextlessConnections
        );

        // Make sure default quad has default orientation
        orientation.applyVertexPermutation(quad);

        // Create processor
        return new QuadProcessor<Pair<TextureConnections,Object[]>>() {
            @Override
            public Pair<TextureConnections,Object[]> extractState(Supplier<Random> randomSupplier, PropertyStore properties){
                // Extract tile states
                Object[] tileStates = new Object[tiles.size()];
                int i = -1;
                while((i = contextlessTiles.nextSetBit(i + 1)) != -1){
                    QuadProcessor<Object> tileProcessor = subProcessors[i];
                    if(tileProcessor == null)
                        continue;
                    tileStates[i] = tileProcessor.extractState(randomSupplier, properties);
                }
                return Pair.of(contextlessConnections, tileStates);
            }

            @Override
            public Pair<TextureConnections,Object[]> extractState(@Nullable IBlockDisplayReader level, @Nullable BlockPos pos, @Nullable BlockState state, Supplier<Random> randomSupplier, PropertyStore properties){
                // Extract all tile data
                Object[] tileStates = new Object[tiles.size()];
                for(int i = 0; i < tiles.size(); i++){
                    QuadProcessor<Object> tileProcessor = subProcessors[i];
                    if(tileProcessor == null)
                        continue;
                    tileStates[i] = tileProcessor.extractState(level, pos, state, randomSupplier, properties);
                }

                // Check whether the predicate has already been evaluated
                Optional<TextureConnections> evaluation = properties.getProperty(PREDICATES_CACHE, predicatesKey);
                if(evaluation.isPresent())
                    return Pair.of(evaluation.get(), tileStates);

                // Get surrounding blocks
                SurroundingBlockCache surroundingBlocks = properties.getOrCompute(SURROUNDING_BLOCKS, () -> {
                    return level == null || pos == null ?
                        SurroundingBlockCache.EMPTY :
                        new SurroundingBlockCache(level, pos, state);
                });

                // Evaluate predicate
                TextureConnections connections = computeConnections(predicatesKey, surroundingBlocks);
                properties.setProperty(PREDICATES_CACHE, predicatesKey, connections);
                return Pair.of(connections, tileStates);
            }

            @Override
            public Pair<TextureConnections,Object[]> extractState(ItemStack stack, Supplier<Random> randomSupplier, PropertyStore properties){
                // Extract tile states
                Object[] tileStates = new Object[tiles.size()];
                int i = -1;
                while((i = contextlessTiles.nextSetBit(i + 1)) != -1){
                    QuadProcessor<Object> tileProcessor = subProcessors[i];
                    if(tileProcessor == null)
                        continue;
                    tileStates[i] = tileProcessor.extractState(stack, randomSupplier, properties);
                }
                return Pair.of(contextlessConnections, tileStates);
            }

            @Override
            public Object createGeometryKey(Pair<TextureConnections,Object[]> state, PropertyStore properties){
                Object[] tileStates = state.right();
                List<Object> subKeys;
                if(state.left() == contextlessConnections){
                    subKeys = new ArrayList<>(Math.min(contextlessTiles.cardinality(), finalProcessorCount));
                    int i = -1;
                    while((i = contextlessTiles.nextSetBit(i + 1)) != -1){
                        QuadProcessor<Object> tileProcessor = subProcessors[i];
                        if(tileProcessor == null)
                            continue;
                        Object subKey = tileProcessor.createGeometryKey(tileStates[i], properties);
                        if(subKey == null)
                            return null;
                        subKeys.add(subKey);
                    }
                }else{
                    subKeys = new ArrayList<>(finalProcessorCount);
                    for(int i = 0; i < tiles.size(); i++){
                        QuadProcessor<Object> tileProcessor = subProcessors[i];
                        if(tileProcessor == null)
                            continue;
                        Object subKey = tileProcessor.createGeometryKey(tileStates[i], properties);
                        if(subKey == null)
                            return null;
                        subKeys.add(subKey);
                    }
                }
                return Triple.of(Pair.of(sprite, predicatesKey), state.left(), subKeys);
            }

            @Override
            public void processQuad(EmittableQuad quad, SpriteInstance sprite, Pair<TextureConnections,Object[]> state, PropertyStore properties){
                // Create access for tiles
                Object[] tileStates = state.right();
                ConnectingTextureLayoutHandler.TileEmitter tileEmitter = (tile, emitter) -> {
                    QuadAccess tileQuad = subQuads[tile];
                    if(tileQuad == null)
                        return;
                    try(EmittableQuad.Popper p = emitter.pushTransform(orientation.transform)){
                        try(EmittableQuad.Popper p2 = emitter.pushTransform(q -> {
                            q.copyFrom(tileQuad);
                            QuadProcessor<Object> tileProcessor = subProcessors[tile];
                            if(tileProcessor == null){
                                q.emit();
                                return;
                            }
                            tileProcessor.processQuad(q, subSprites[tile], tileStates[tile], properties);
                        })){
                            emitter.emit();
                        }
                    }
                };

                // Process quad
                ConnectingTextureLayoutHandler layoutHandler = ConnectingTextureLayoutHandler.get(data.getLayout());
                try(EmittableQuad.Popper p = quad.pushTransform(orientation.reverseTransform)){
                    layoutHandler.processQuad(quad, tileEmitter, state.left());
                }
            }
        };
    }

    private static TextureConnections computeConnections(QuadPredicatesKey predicates, SurroundingBlockCache blocks){
        ConnectionPredicate predicate = predicates.predicate;
        Direction face = predicates.side;
        TextureOrientation orientation = predicates.orientation;

        // Get the up and right vectors for the way textures are rotated by default for quad's facing
        int[] up = orientation.transformWorldUpVector(face);
        int[] right = orientation.transformWorldRightVector(face);

        BlockPos.Mutable mutablePos = new BlockPos.Mutable();
        boolean connectTop = shouldConnect(predicate, blocks, face, orientation.worldToTexture[0], up[0], up[1], up[2], mutablePos);
        boolean connectTopRight = shouldConnect(predicate, blocks, face, orientation.worldToTexture[1], up[0] + right[0], up[1] + right[1], up[2] + right[2], mutablePos);
        boolean connectRight = shouldConnect(predicate, blocks, face, orientation.worldToTexture[2], right[0], right[1], right[2], mutablePos);
        boolean connectBottomRight = shouldConnect(predicate, blocks, face, orientation.worldToTexture[3], -up[0] + right[0], -up[1] + right[1], -up[2] + right[2], mutablePos);
        boolean connectBottom = shouldConnect(predicate, blocks, face, orientation.worldToTexture[4], -up[0], -up[1], -up[2], mutablePos);
        boolean connectBottomLeft = shouldConnect(predicate, blocks, face, orientation.worldToTexture[5], -up[0] - right[0], -up[1] - right[1], -up[2] - right[2], mutablePos);
        boolean connectLeft = shouldConnect(predicate, blocks, face, orientation.worldToTexture[6], -right[0], -right[1], -right[2], mutablePos);
        boolean connectTopLeft = shouldConnect(predicate, blocks, face, orientation.worldToTexture[7], up[0] - right[0], up[1] - right[1], up[2] - right[2], mutablePos);
        return new TextureConnections(connectTop, connectTopRight, connectRight, connectBottomRight, connectBottom, connectBottomLeft, connectLeft, connectTopLeft);
    }

    private static boolean shouldConnect(ConnectionPredicate predicate, SurroundingBlockCache blocks, Direction face, ConnectionDirection direction, int neighborX, int neighborY, int neighborZ, BlockPos.Mutable mutablePos){
        IBlockDisplayReader level = blocks.getLevel();
        BlockPos position = blocks.getRealPos();
        BlockState self = blocks.getCenter();
        BlockState neighborState = blocks.getState(neighborX, neighborY, neighborZ);
        mutablePos.set(position.getX() + neighborX, position.getY() + neighborY, position.getZ() + neighborZ);
        BlockState stateInFront = blocks.getState(neighborX + face.getStepX(), neighborY + face.getStepY(), neighborZ + face.getStepZ());
        return predicate.shouldConnect(level, position, face, self, neighborState, stateInFront, direction);
    }

    private static ConnectionPredicate resolveConnectionsKey(String key, PropertyGetter properties, Consumer<List<String>> reportCircular){
        // Resolve the key
        List<String> encounteredKeys = new ArrayList<>();
        while(true){
            encounteredKeys.add(key);
            Either<String,ConnectionPredicate> next = properties.getProperty(DefaultModelProperties.CONNECTION_PREDICATE, key).orElse(null);
            if(next != null){
                if(next.isRight())
                    return next.right();
                key = next.left();
            }else{ // Check materials map
                Either<String,ModelMaterial> material = properties.getProperty(DefaultModelProperties.MATERIAL, key).orElse(null);
                if(material == null){
                    if(key.equals(ConnectingModelData.DEFAULT_KEY))
                        break;
                    key = ConnectingModelData.DEFAULT_KEY;
                }else
                    key = material.flatMap(Function.identity(), m -> m.texture().toString());
            }
            if(encounteredKeys.contains(key)){
                encounteredKeys.add(key);
                reportCircular.accept(Collections.unmodifiableList(encounteredKeys));
                break;
            }
        }
        return null;
    }

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
            ConnectingTextureData.Layout layout;
            try{
                layout = ConnectingTextureData.Layout.valueOf(layoutString.toUpperCase(Locale.ROOT));
            }catch(IllegalArgumentException e){
                throw new JsonParseException("Property 'layout' must be one of " + Arrays.toString(ConnectingTextureData.Layout.values()).toLowerCase(Locale.ROOT) + ", not '" + layoutString + "'!");
            }
            builder.layout(layout);
        }
        // Deserialize connection predicate
        if(json.has("connections")){
            if(json.get("connections").isJsonArray()){
                JsonArray array = json.getAsJsonArray("connections");
                List<ConnectionPredicate> subPredicates = new ArrayList<>();
                for(JsonElement predicateElements : array){
                    if(!predicateElements.isJsonObject())
                        throw new JsonParseException("Array property 'connections' must only contain objects!");
                    ConnectionPredicate predicate = FusionConnectionPredicateRegistry.deserializeConnectionPredicate(predicateElements.getAsJsonObject());
                    subPredicates.add(predicate);
                }
                builder.connectionPredicate(DefaultConnectionPredicates.or(subPredicates.toArray(new ConnectionPredicate[0])));
            }else if(json.get("connections").isJsonObject())
                builder.connectionPredicate(FusionConnectionPredicateRegistry.deserializeConnectionPredicate(json.getAsJsonObject("connections")));
            else
                throw new JsonParseException("Property 'connections' must be an object or array of objects!");
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
    public JsonObject serialize(ConnectingTextureData data){
        // Serialize base properties
        JsonObject json = DefaultTextureTypes.BASE.serialize(data);
        // Serialize 'layout'
        if(data.getLayout() != ConnectingTextureData.Layout.FULL)
            json.addProperty("layout", data.getLayout().name().toLowerCase(Locale.ROOT));
        // Serialize connection predicate
        if(data.getConnectionPredicate() != null)
            json.add("connections", FusionConnectionPredicateRegistry.serializeConnectionPredicate(data.getConnectionPredicate()));
        // Sub-texture
        if(data.subTexture() != null && data.subTexture().getTextureType() != DefaultTextureTypes.VANILLA)
            json.add("sub_texture", TextureTypeRegistryImpl.serializeTextureData(data.subTexture()));
        // Per tile animation
        if(data.perTileAnimation())
            json.addProperty("per_tile_animation", true);
        return json.size() == 0 ? null : json;
    }

    private static final class QuadPredicatesKey {
        private final Direction side;
        private final TextureOrientation orientation;
        private final ConnectionPredicate predicate;

        private QuadPredicatesKey(Direction side, TextureOrientation orientation, ConnectionPredicate predicate){
            this.side = side;
            this.orientation = orientation;
            this.predicate = predicate;
        }
    }
}
