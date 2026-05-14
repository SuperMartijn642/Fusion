package com.supermartijn642.fusion.texture.types.connecting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.custom.quad.EmittableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.*;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.api.texture.types.connecting.ConnectingTextureData;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionDirection;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionPredicate;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.DefaultConnectionPredicates;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.FusionConnectionPredicateRegistry;
import com.supermartijn642.fusion.api.util.Property;
import com.supermartijn642.fusion.api.util.PropertyStore;
import com.supermartijn642.fusion.api.util.UserErrorException;
import com.supermartijn642.fusion.model.types.connecting.ConnectingModelType;
import com.supermartijn642.fusion.texture.DummyTextureSpriteContents;
import com.supermartijn642.fusion.texture.types.base.BaseTextureType;
import com.supermartijn642.fusion.texture.types.connecting.layouts.ConnectingTextureLayoutHandler;
import com.supermartijn642.fusion.util.Triple;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.resources.data.AnimationFrame;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
public class ConnectingTextureType implements TextureType<ConnectingTextureData,StitchedConnectingTextureData> {

    private static final ConnectionPredicate FALLBACK_PREDICATE = DefaultConnectionPredicates.isSameState();

    private static final Property<SurroundingBlockCache,Void> SURROUNDING_BLOCKS = Property.create();
    private static final Property<TextureConnections,QuadPredicatesKey> PREDICATES_CACHE = Property.create(QuadPredicatesKey.class);
    private static final Property<OrientedMutableQuad,Void> ORIENTED_QUAD = Property.create();
    private static final Property<MutableQuad,Void> DUMMY_QUAD = Property.create();

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

        // Create animation data
        int frameColumns = imageWidth / frameWidth;
        int frameRows = imageHeight / frameHeight;
        int tileWidth = frameWidth / layout.getWidth();
        int tileHeight = frameHeight / layout.getHeight();
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
                    frames.add(SpriteImageSource.AnimationFrame.of(x, y, frame.isTimeUnknown() ? animationMetadata.getDefaultFrameTime() : frame.getTime()));
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
        List<SpriteInstance> tiles = new ArrayList<>(layout.getWidth() * layout.getHeight());
        try(NativeImage n = image){
            for(int y = 0; y < layout.getHeight(); y++){
                for(int x = 0; x < layout.getWidth(); x++){
                    tiles.add(null);
                    // Skip empty tiles
                    if((x != layout.defaultTileX() || y != layout.defaultTileY()) &&
                        DummyTextureSpriteContents.isSubImageEmpty(image, x * tileWidth, y * tileHeight, tileWidth, tileHeight)){
                        continue;
                    }
                    NativeImage subImage = ImageHelper.createCropFramed(image, x * tileWidth, y * tileHeight, tileWidth, tileHeight, frameWidth, frameHeight, false);
                    SpriteImageSource imageSource = frames == null ?
                        SpriteImageSource.constant(subImage) :
                        SpriteImageSource.animated(subImage, tileWidth, tileHeight, frames, animationMetadata.isInterpolatedFrames());
                    int index = x + y * layout.getWidth();
                    output.createSprite()
                        .image(imageSource)
                        .markDefaultSprite(x == layout.defaultTileX() && y == layout.defaultTileY())
                        .setCreationCallback(s -> tiles.set(index, s))
                        .submit();
                }
            }
        }

        // Set custom texture data
        output.setCustomData(new StitchedConnectingTextureData(data, tiles));
    }

    @Override
    public @Nullable QuadProcessor<?> initializeModelQuad(MutableQuad quad, SpriteInstance sprite, StitchedConnectingTextureData data, PropertyStore properties){
        // Apply base texture properties
        BaseTextureType.applyProperties(quad, data);

        // Get connections predicate
        ConnectionPredicate predicate = properties.getProperty(ConnectingModelType.FACE_CONNECTION_PREDICATE).orElse(null);
        if(predicate == null){
            predicate = data.getConnectionPredicate();
            if(predicate == null)
                predicate = FALLBACK_PREDICATE;
        }
        // Get quad orientation
        TextureOrientation orientation = TextureOrientation.findOrientation(quad);
        // Get layout handler
        ConnectingTextureLayoutHandler layoutHandler = ConnectingTextureLayoutHandler.get(data.getLayout());

        // Create predicates key
        QuadPredicatesKey predicatesKey = new QuadPredicatesKey(
            quad.facing(),
            orientation,
            predicate
        );

        // Create processor
        return new QuadProcessor<TextureConnections>() {
            @Override
            public TextureConnections extractState(Supplier<Random> randomSupplier, PropertyStore properties){
                return this.extractState(null, null, null, randomSupplier, properties);
            }

            @Override
            public TextureConnections extractState(@Nullable IBlockDisplayReader level, @Nullable BlockPos pos, @Nullable BlockState state, Supplier<Random> randomSupplier, PropertyStore properties){
                // Check whether the predicate has already been evaluated
                Optional<TextureConnections> evaluation = properties.getProperty(PREDICATES_CACHE, predicatesKey);
                if(evaluation.isPresent())
                    return evaluation.get();

                // Get surrounding blocks
                SurroundingBlockCache surroundingBlocks = properties.getProperty(SURROUNDING_BLOCKS).orElseGet(() -> {
                    SurroundingBlockCache c = level == null || pos == null ?
                        SurroundingBlockCache.EMPTY :
                        new SurroundingBlockCache(level, pos, state);
                    properties.setProperty(SURROUNDING_BLOCKS, c);
                    return c;
                });

                // Evaluate predicate
                TextureConnections connections = computeConnections(predicatesKey, surroundingBlocks);
                properties.setProperty(PREDICATES_CACHE, predicatesKey, connections);
                return connections;
            }

            @Override
            public TextureConnections extractState(ItemStack stack, Supplier<Random> randomSupplier, PropertyStore properties){
                return null;
            }

            @Override
            public Object createGeometryKey(TextureConnections state, PropertyStore properties){
                return Triple.of(DefaultTextureTypes.CONNECTING, sprite, state);
            }

            @Override
            public void processQuad(EmittableQuad quad, SpriteInstance sprite, TextureConnections state, PropertyStore properties){
                // Create oriented quad, so the quad always has the same orientation for the layout handlers
                OrientedMutableQuad orientedQuad = properties.getProperty(ORIENTED_QUAD).orElseGet(() -> {
                    OrientedMutableQuad q = new OrientedMutableQuad();
                    properties.setProperty(ORIENTED_QUAD, q);
                    return q;
                });
                // Get dummy quad
                MutableQuad dummyQuad = properties.getProperty(DUMMY_QUAD).orElseGet(() -> {
                    MutableQuad q = MutableQuad.create();
                    properties.setProperty(DUMMY_QUAD, q);
                    return q;
                });
                dummyQuad.copyFrom(quad);
                // Process quads
                for(int i = 0; i < layoutHandler.getAuxiliaryQuadCount() + 1; i++){
                    orientedQuad.copyFrom(dummyQuad);
                    orientedQuad.setPermutation(predicatesKey.orientation.vertexIndexPermutation);
                    boolean keepQuad = state == null ?
                        layoutHandler.processItemQuad(i, orientedQuad, sprite, data) :
                        layoutHandler.processBlockQuad(i, orientedQuad, sprite, data, state);
                    orientedQuad.resetPermutation();
                    if(keepQuad){
                        quad.copyFrom(orientedQuad);
                        quad.emit();
                    }
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
