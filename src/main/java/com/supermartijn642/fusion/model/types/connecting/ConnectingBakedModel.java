package com.supermartijn642.fusion.model.types.connecting;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.predicate.ConnectionDirection;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureLayout;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.texture.types.connecting.ConnectingTextureSprite;
import com.supermartijn642.fusion.texture.types.connecting.TextureConnections;
import com.supermartijn642.fusion.texture.types.connecting.layouts.ConnectingTextureLayoutHandler;
import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureSprite;
import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureType;
import com.supermartijn642.fusion.texture.types.random.RandomTextureSprite;
import com.supermartijn642.fusion.texture.types.random.RandomTextureType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class ConnectingBakedModel implements BlockStateModel {

    private static final int VERTEX_SIZE, VERTEX_UV_OFFSET, VERTEX_POSITION_OFFSET;
    /**
     * Stores world space vector point in the up and right direction of the default texture orientation for each face
     */
    private static final int[][] DEFAULT_TEXTURE_ROTATIONS_UP = new int[6][];
    private static final int[][] DEFAULT_TEXTURE_ROTATIONS_RIGHT = new int[6][];
    private static final Direction[] CULL_DIRECTIONS = {null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    static{
        // Amounts are in bytes, as a baked quad stores the data as ints and an int is 4 bytes, we divide by 4
        VERTEX_SIZE = DefaultVertexFormat.BLOCK.getVertexSize() / 4;
        VERTEX_UV_OFFSET = DefaultVertexFormat.BLOCK.getOffset(VertexFormatElement.UV) / 4;
        VERTEX_POSITION_OFFSET = DefaultVertexFormat.BLOCK.getOffset(VertexFormatElement.POSITION) / 4;

        for(Direction direction : Direction.values()){
            int upX = 0, upY = 0, upZ = 0, rightX = 0, rightY = 0, rightZ = 0;
            if(direction == Direction.DOWN){
                upZ = 1;
                rightX = 1;
            }else if(direction == Direction.UP){
                upZ = -1;
                rightX = 1;
            }else if(direction == Direction.NORTH){
                upY = 1;
                rightX = -1;
            }else if(direction == Direction.SOUTH){
                upY = 1;
                rightX = 1;
            }else if(direction == Direction.WEST){
                upY = 1;
                rightZ = 1;
            }else if(direction == Direction.EAST){
                upY = 1;
                rightZ = -1;
            }
            DEFAULT_TEXTURE_ROTATIONS_UP[direction.ordinal()] = new int[]{upX, upY, upZ};
            DEFAULT_TEXTURE_ROTATIONS_RIGHT[direction.ordinal()] = new int[]{rightX, rightY, rightZ};
        }
    }

    private static float[] getUV(BakedQuad quad, int vertexIndex){
        int offset = vertexIndex * VERTEX_SIZE + VERTEX_UV_OFFSET;
        return new float[]{Float.intBitsToFloat(quad.vertices()[offset]), Float.intBitsToFloat(quad.vertices()[offset + 1])};
    }

    private static float[] getPosition(BakedQuad quad, int vertexIndex){
        int offset = vertexIndex * VERTEX_SIZE + VERTEX_POSITION_OFFSET;
        return new float[]{
            Float.intBitsToFloat(quad.vertices()[offset]),
            Float.intBitsToFloat(quad.vertices()[offset + 1]),
            Float.intBitsToFloat(quad.vertices()[offset + 2])
        };
    }

    private final List<BlockModelPart> completeBlockMesh;
    private final Map<Optional<ChunkSectionLayer>,List<TaggedBakedQuad>[]> blockMesh;
    private final List<Optional<ChunkSectionLayer>> blockRenderTypes;
    private final List<QuadPredicates> predicates;
    private final List<TextureAtlasSprite> sprites;
    private final boolean hasSpecialQuads;
    private final boolean hasAmbientOcclusion;
    private final TextureAtlasSprite particleIcon;

    public ConnectingBakedModel(List<ConnectingModelQuad> quads, boolean hasAmbientOcclusion, TextureAtlasSprite particleIcon, ChunkSectionLayer neoforgeRenderType){
        this.hasAmbientOcclusion = hasAmbientOcclusion;
        this.particleIcon = particleIcon;

        // Create block and item meshes from the quads
        Map<Optional<ChunkSectionLayer>,List<TaggedBakedQuad>[]> blockMesh = new HashMap<>();
        Set<Optional<ChunkSectionLayer>> blockRenderTypes = new HashSet<>();
        HashMap<QuadPredicates,Integer> predicates = new HashMap<>();
        HashMap<TextureAtlasSprite,Integer> sprites = new HashMap<>();
        boolean hasSpecialQuads = false;
        OrientedMutableQuad mutableQuad = new OrientedMutableQuad();
        for(ConnectingModelQuad quad : quads){
            TextureType<?> textureType = quad.textureType();
            int spriteIndex = -1;
            int predicateIndex = -1;
            // Some layouts need auxiliary quads, hence simply repeat the quad that many times
            int auxiliaryQuadCount = 0;
            if(quad.hasConnectingTexture()){
                Direction direction = quad.bakedQuad().direction();
                TextureOrientation orientation = findOrientation(quad.bakedQuad());
                ConnectionPredicate predicate = quad.connectionPredicate();
                // Get the number of auxiliary quads needed
                auxiliaryQuadCount = ConnectingTextureLayoutHandler.get(quad.getLayout()).getAuxiliaryQuadCount();
                // Give each combination of direction, orientation, and predicate a unique index
                predicateIndex = predicates.computeIfAbsent(new QuadPredicates(direction, orientation, predicate), o -> predicates.size());
                // Give each sprite a unique index
                spriteIndex = sprites.computeIfAbsent(quad.bakedQuad().sprite(), o -> sprites.size());
            }
            // Tag quads which need additional processing
            if(quad.textureType() == DefaultTextureTypes.RANDOM || quad.textureType() == DefaultTextureTypes.CONTINUOUS){
                // Give each sprite a unique index
                spriteIndex = sprites.computeIfAbsent(quad.bakedQuad().sprite(), o -> sprites.size());
                hasSpecialQuads = true;
            }
            // Submit the quads
            for(int quadIndex = 0; quadIndex < auxiliaryQuadCount + 1; quadIndex++){
                mutableQuad.fillFromBakedQuad(quad.bakedQuad());
                mutableQuad.ambientOcclusion(!quad.emissive() && hasAmbientOcclusion);
                mutableQuad.emissive(quad.emissive());

                // Add the block quad
                TaggedBakedQuad finishedQuad = new TaggedBakedQuad(mutableQuad.toBakedQuad(), textureType, spriteIndex, predicateIndex, quadIndex);
                Optional<ChunkSectionLayer> layer = FusionClient.getChunkLayer(quad.renderType());
                if(layer.isEmpty() && neoforgeRenderType != null)
                    layer = Optional.of(neoforgeRenderType);
                blockRenderTypes.add(layer);
                int cullIndex = cullIndex(quad.cullDirection());
                //noinspection unchecked
                List<TaggedBakedQuad>[] mesh = blockMesh.computeIfAbsent(layer, r -> new List[7]);
                if(mesh[cullIndex] == null)
                    mesh[cullIndex] = new ArrayList<>();
                mesh[cullIndex].add(finishedQuad);
            }
        }
        this.blockMesh = Map.copyOf(blockMesh);
        this.blockRenderTypes = List.copyOf(blockRenderTypes);
        this.predicates = predicates.entrySet().stream().sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey).toList();
        this.sprites = sprites.entrySet().stream().sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey).toList();
        this.hasSpecialQuads = hasSpecialQuads;

        // Create a mesh of unprocessed quads
        List<BlockModelPart> parts = new ArrayList<>();
        for(Map.Entry<Optional<ChunkSectionLayer>,List<TaggedBakedQuad>[]> entry : blockMesh.entrySet()){
            Optional<ChunkSectionLayer> layer = entry.getKey();
            //noinspection unchecked
            List<BakedQuad>[] mesh = Arrays.stream(entry.getValue())
                .map(l -> l == null ? List.of() : l.stream().map(q -> q.bakedQuad).toList())
                .toArray(List[]::new);
            parts.add(new BlockModelPart() {
                @Override
                public List<BakedQuad> getQuads(@Nullable Direction cullDirection){
                    return mesh[cullIndex(cullDirection)];
                }

                @Override
                public boolean useAmbientOcclusion(){
                    return hasAmbientOcclusion;
                }

                @Override
                public TextureAtlasSprite particleIcon(){
                    return particleIcon;
                }

                @Override
                public ChunkSectionLayer getRenderType(BlockState state){
                    //noinspection deprecation
                    return layer.orElseGet(() -> ItemBlockRenderTypes.getChunkRenderType(state));
                }
            });
        }
        this.completeBlockMesh = List.copyOf(parts);
    }

    private static TextureOrientation findOrientation(BakedQuad quad){
        // First determine the texture orientation relative to the vertex indices
        float[][] uvs = {getUV(quad, 0), getUV(quad, 1), getUV(quad, 2), getUV(quad, 3)};
        // Compare the angle between directions v1 to v2 and v1 to v3, to check whether the texture is flipped
        double angle1to2 = Math.atan2(uvs[1][1] - uvs[0][1], uvs[1][0] - uvs[0][0]), angle1to3 = Math.atan2(uvs[2][1] - uvs[0][1], uvs[2][0] - uvs[0][0]);
        boolean textureFlipped = (angle1to2 - angle1to3 + 4 * Math.PI) % (2 * Math.PI) < Math.PI;
        // Find the top-left-most-ish index, if we assume the uvs form an axis-aligned square this should work
        int topLeftMostIndex = 0;
        for(int i = 1; i < 4; i++){
            float[] best = uvs[topLeftMostIndex], current = uvs[i];
            if(current[0] + current[1] < best[0] + best[1])
                topLeftMostIndex = i;
        }
        int textureRotation = textureFlipped ? topLeftMostIndex : (4 - topLeftMostIndex) % 4;

        // Determine the vertex indices rotation relative to the block face
        float[][] positions3d = {getPosition(quad, 0), getPosition(quad, 1), getPosition(quad, 2), getPosition(quad, 3)};
        // Project the 3d positions onto the plane perpendicular to the facing of the quad
        float[][] pos = new float[4][2];
        Direction direction = quad.direction();
        for(int i = 0; i < 4; i++){
            if(direction == Direction.DOWN){
                pos[i][0] = positions3d[i][0];
                pos[i][1] = -positions3d[i][2];
            }else if(direction == Direction.UP){
                pos[i][0] = positions3d[i][0];
                pos[i][1] = positions3d[i][2];
            }else if(direction == Direction.NORTH){
                pos[i][0] = -positions3d[i][0];
                pos[i][1] = -positions3d[i][1];
            }else if(direction == Direction.SOUTH){
                pos[i][0] = positions3d[i][0];
                pos[i][1] = -positions3d[i][1];
            }else if(direction == Direction.WEST){
                pos[i][0] = positions3d[i][2];
                pos[i][1] = -positions3d[i][1];
            }else if(direction == Direction.EAST){
                pos[i][0] = -positions3d[i][2];
                pos[i][1] = -positions3d[i][1];
            }
        }
        // Compare the angle between directions v1 to v2 and v1 to v3, to check whether the texture is flipped
        angle1to2 = Math.atan2(pos[1][1] - pos[0][1], pos[1][0] - pos[0][0]);
        angle1to3 = Math.atan2(pos[2][1] - pos[0][1], pos[2][0] - pos[0][0]);
        boolean quadFlipped = (angle1to2 - angle1to3 + 4 * Math.PI) % (2 * Math.PI) < Math.PI;
        // Find the top-left-most-ish index, if we assume the uvs form an axis-aligned square this should work
        topLeftMostIndex = 0;
        for(int i = 1; i < 4; i++){
            float[] best = pos[topLeftMostIndex], current = pos[i];
            if(current[0] + current[1] < best[0] + best[1])
                topLeftMostIndex = i;
        }
        int quadRotation = textureFlipped ? topLeftMostIndex : (4 - topLeftMostIndex);

        // Combine the two, to get the in-world orientation of the texture
        boolean flipped = textureFlipped ^ quadFlipped;
        int rotation = quadFlipped ? (4 - textureRotation + quadRotation) % 4 : (textureRotation + quadRotation) % 4;
        return TextureOrientation.of(flipped, rotation);
    }

    @Override
    public void collectParts(BlockAndTintGetter blockView, BlockPos pos, BlockState state, RandomSource random, List<BlockModelPart> parts){
        // If either level or position is not given, the connected textures cannot be updated, so don't process them
        boolean processConnectingTextures = blockView != null && pos != null && !this.predicates.isEmpty();
        if(!processConnectingTextures && !this.hasSpecialQuads){
            parts.addAll(this.completeBlockMesh);
            return;
        }

        // Only compute connections for each predicate once
        TextureConnections[] connectionsCache = null;
        // Store a cache of the surrounding blocks
        SurroundingBlockCache blockCache = null;
        if(processConnectingTextures){
            connectionsCache = new TextureConnections[this.predicates.size()];
            blockCache = new SurroundingBlockCache(blockView, pos, state);
            // Make sure to use the block state argument for the model's own block
            if(state != null)
                blockCache.setSelf(state);
        }

        // Create one model part for each render type
        OrientedMutableQuad mutableQuad = new OrientedMutableQuad();
        for(Optional<ChunkSectionLayer> layer : this.blockRenderTypes){
            List<TaggedBakedQuad>[] mesh = this.blockMesh.get(layer);
            //noinspection unchecked
            List<BakedQuad>[] processedMesh = new List[mesh.length];
            for(Direction cullDirection : CULL_DIRECTIONS){
                List<TaggedBakedQuad> quads = this.blockMesh.get(layer)[cullIndex(cullDirection)];
                if(quads == null){
                    processedMesh[cullIndex(cullDirection)] = List.of();
                    continue;
                }
                ArrayList<BakedQuad> bakedQuads = new ArrayList<>(quads.size());

                for(TaggedBakedQuad quad : quads){
                    // Process special texture type quads
                    if(pos != null && (quad.textureType == DefaultTextureTypes.RANDOM || quad.textureType == DefaultTextureTypes.CONTINUOUS)){
                        // Get the sprite
                        TextureAtlasSprite sprite = this.sprites.get(quad.spriteIndex);

                        mutableQuad.fillFromBakedQuad(quad.bakedQuad);
                        mutableQuad.resetPermutation();
                        if(quad.textureType == DefaultTextureTypes.RANDOM)
                            // Handle random texture type
                            RandomTextureType.processQuad(mutableQuad, pos, quad.bakedQuad.direction(), random, (RandomTextureSprite)sprite);
                        else
                            // Handle continuous texture type
                            ContinuousTextureType.processQuad(mutableQuad, pos, quad.bakedQuad.direction(), (ContinuousTextureSprite)sprite);
                        bakedQuads.add(mutableQuad.toBakedQuad());
                    }
                    // Process connecting textures
                    else if(blockCache != null && quad.textureType == DefaultTextureTypes.CONNECTING){
                        // Get the quad index, predicate index, and sprite index
                        int quadIndex = quad.quadIndex;
                        int predicateIndex = quad.predicateIndex;
                        int spriteIndex = quad.spriteIndex;

                        // Get the connection predicate
                        QuadPredicates predicate = this.predicates.get(predicateIndex);
                        // Check if the connections have already been computed, otherwise compute them
                        TextureConnections connections = connectionsCache[predicateIndex];
                        if(connections == null){
                            // Compute the connections
                            connections = connectionsCache[predicateIndex] = computeConnections(predicate, blockCache);
                        }

                        // Get the sprite and the texture layout
                        TextureAtlasSprite sprite = this.sprites.get(spriteIndex);
                        ConnectingTextureLayout layout = ((ConnectingTextureSprite)sprite).data().getLayout();

                        // Remap the quad's uv
                        mutableQuad.fillFromBakedQuad(quad.bakedQuad);
                        mutableQuad.set(predicate.orientation.vertexIndexPermutation);
                        boolean keepQuad = ConnectingTextureLayoutHandler.get(layout).processBlockQuad(quadIndex, mutableQuad, (ConnectingTextureSprite)sprite, connections);
                        if(keepQuad)
                            bakedQuads.add(mutableQuad.toBakedQuad());
                    }else
                        bakedQuads.add(quad.bakedQuad);
                }

                processedMesh[cullIndex(cullDirection)] = bakedQuads;
            }

            // Creat the model part for the mesh
            parts.add(new BlockModelPart() {
                @Override
                public List<BakedQuad> getQuads(@Nullable Direction cullDirection){
                    return processedMesh[cullIndex(cullDirection)];
                }

                @Override
                public boolean useAmbientOcclusion(){
                    return ConnectingBakedModel.this.hasAmbientOcclusion;
                }

                @Override
                public TextureAtlasSprite particleIcon(){
                    return ConnectingBakedModel.this.particleIcon;
                }

                @Override
                public ChunkSectionLayer getRenderType(BlockState state){
                    //noinspection deprecation
                    return layer.orElseGet(() -> ItemBlockRenderTypes.getChunkRenderType(state));
                }
            });
        }
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> parts){
        this.collectParts(null, null, null, random, parts);
    }

    private static TextureConnections computeConnections(QuadPredicates predicates, SurroundingBlockCache blocks){
        ConnectionPredicate predicate = predicates.predicate;
        Direction face = predicates.direction;
        TextureOrientation orientation = predicates.orientation;

        // Get the up and right vectors for the way textures are rotated by default for quad's facing
        int[] up = orientation.transformWorldVector(DEFAULT_TEXTURE_ROTATIONS_UP[face.ordinal()], face);
        int[] right = orientation.transformWorldVector(DEFAULT_TEXTURE_ROTATIONS_RIGHT[face.ordinal()], face);

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
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

    private static boolean shouldConnect(ConnectionPredicate predicate, SurroundingBlockCache blocks, Direction face, ConnectionDirection direction, int neighborX, int neighborY, int neighborZ, BlockPos.MutableBlockPos mutablePos){
        BlockAndTintGetter level = blocks.getLevel();
        BlockPos position = blocks.getRealPos();
        BlockState self = blocks.getCenter();
        BlockState neighborState = blocks.getState(neighborX, neighborY, neighborZ);
        mutablePos.set(position.getX() + neighborX, position.getY() + neighborY, position.getZ() + neighborZ);
        BlockState selfAppearance = self.getAppearance(level, position, face, neighborState, mutablePos);
        BlockState otherStateAppearance = neighborState.getAppearance(level, mutablePos, face, self, position);
        BlockState stateInFront = blocks.getState(neighborX + face.getStepX(), neighborY + face.getStepY(), neighborZ + face.getStepZ());
        return predicate.shouldConnect(level, position, face, selfAppearance, otherStateAppearance, stateInFront, direction);
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random){
        if(this.predicates.isEmpty() && !this.hasSpecialQuads)
            return this;
        List<Object> modelData = new ArrayList<>(2);
        // Add position of the block
        if(this.hasSpecialQuads)
            modelData.add(pos);
        // Evaluate predicates for connecting textures
        if(!this.predicates.isEmpty()){
            SurroundingBlockCache blockCache = new SurroundingBlockCache(level, pos, state);
            TextureConnections[] evaluations = new TextureConnections[this.predicates.size()];
            for(int i = 0; i < this.predicates.size(); i++)
                evaluations[i] = computeConnections(this.predicates.get(i), blockCache);
            modelData.add(Arrays.asList(evaluations));
        }
        return Pair.of(this, modelData);
    }

    @Override
    public TextureAtlasSprite particleIcon(){
        return this.particleIcon;
    }

    private static int cullIndex(Direction cullDirection){
        return cullDirection == null ? 0 : cullDirection.ordinal() + 1;
    }

    private static class QuadPredicates {
        public final Direction direction;
        public final TextureOrientation orientation;
        public final ConnectionPredicate predicate;

        private QuadPredicates(Direction direction, TextureOrientation orientation, ConnectionPredicate predicate){
            this.direction = direction;
            this.orientation = orientation;
            this.predicate = predicate;
        }

        @Override
        public final boolean equals(Object o){
            if(this == o) return true;
            if(!(o instanceof QuadPredicates that)) return false;

            return this.direction == that.direction && this.orientation == that.orientation && this.predicate.equals(that.predicate);
        }

        @Override
        public int hashCode(){
            int result = this.direction.hashCode();
            result = 31 * result + this.orientation.hashCode();
            result = 31 * result + this.predicate.hashCode();
            return result;
        }
    }

    enum TextureOrientation {
        NORMAL_0(false, 0), NORMAL_90(false, 1), NORMAL_180(false, 2), NORMAL_270(false, 3),
        FLIPPED_0(true, 0), FLIPPED_90(true, 1), FLIPPED_180(true, 2), FLIPPED_270(true, 3);

        public static TextureOrientation of(boolean flipped, int rotations){
            return TextureOrientation.values()[flipped ? 4 + rotations : rotations];
        }

        public final boolean flipped;
        public final int rotations;
        /**
         * If {@code dir} is the in-world direction, {@code worldToTexture[dir.ordinal()]} is the texture space direction
         */
        public final ConnectionDirection[] worldToTexture;
        public final int[] vertexIndexPermutation;

        TextureOrientation(boolean flipped, int rotations){
            this.flipped = flipped;
            this.rotations = rotations;

            this.worldToTexture = ConnectionDirection.values();
            this.vertexIndexPermutation = new int[]{0, 3, 2, 1};
            // First apply flip
            if(flipped){
                this.worldToTexture[ConnectionDirection.TOP.ordinal()] = ConnectionDirection.LEFT;
                this.worldToTexture[ConnectionDirection.TOP_RIGHT.ordinal()] = ConnectionDirection.BOTTOM_LEFT;
                this.worldToTexture[ConnectionDirection.RIGHT.ordinal()] = ConnectionDirection.BOTTOM;
                this.worldToTexture[ConnectionDirection.LEFT.ordinal()] = ConnectionDirection.TOP;
                this.worldToTexture[ConnectionDirection.BOTTOM_LEFT.ordinal()] = ConnectionDirection.TOP_RIGHT;
                this.worldToTexture[ConnectionDirection.BOTTOM.ordinal()] = ConnectionDirection.RIGHT;
                this.vertexIndexPermutation[1] = 1;
                this.vertexIndexPermutation[3] = 3;
            }
            // Then apply rotation
            if(rotations != 0){
                ConnectionDirection[] old = Arrays.copyOf(this.worldToTexture, this.worldToTexture.length);
                for(int i = 0; i < 8; i++)
                    this.worldToTexture[i] = old[(i - rotations * 2 + 8) % 8];
                int[] old2 = Arrays.copyOf(this.vertexIndexPermutation, this.vertexIndexPermutation.length);
                for(int i = 0; i < 4; i++)
                    this.vertexIndexPermutation[i] = old2[(i + rotations + 4) % 4];
            }
        }

        public int[] transformWorldVector(int[] vector, Direction face){ // TODO improve this
            if(!this.flipped && this.rotations == 0)
                return vector;
            int[] newVector = Arrays.copyOf(vector, vector.length);
            Direction.Axis axis = face.getAxis();
            boolean positive = face.getAxisDirection() == Direction.AxisDirection.POSITIVE;
            if(this.flipped){
                if(face.getAxis() == Direction.Axis.X){
                    newVector[1] = positive ? vector[2] : -vector[2];
                    newVector[2] = positive ? vector[1] : -vector[1];
                }
                if(face.getAxis() == Direction.Axis.Y){
                    newVector[0] = positive ? vector[2] : -vector[2];
                    newVector[2] = positive ? vector[0] : -vector[0];
                }
                if(face.getAxis() == Direction.Axis.Z){
                    newVector[0] = positive ? vector[1] : -vector[1];
                    newVector[1] = positive ? vector[0] : -vector[0];
                }
            }
            if(this.rotations > 0){
                if(this.rotations == 2){
                    if(axis != Direction.Axis.X)
                        newVector[0] = -newVector[0];
                    if(axis != Direction.Axis.Y)
                        newVector[1] = -newVector[1];
                    if(axis != Direction.Axis.Z)
                        newVector[2] = -newVector[2];
                }else{
                    int oldX = newVector[0];
                    int oldY = newVector[1];
                    if(axis != Direction.Axis.X)
                        newVector[0] = ((positive ^ this.rotations == 3 ^ this.flipped) ? 1 : -1) * (axis == Direction.Axis.Y ? -newVector[2] : newVector[1]);
                    if(axis != Direction.Axis.Y)
                        newVector[1] = ((positive ^ this.rotations == 3 ^ this.flipped) ? 1 : -1) * (axis == Direction.Axis.Z ? -oldX : newVector[2]);
                    if(axis != Direction.Axis.Z)
                        newVector[2] = ((positive ^ this.rotations == 3 ^ this.flipped) ? 1 : -1) * (axis == Direction.Axis.X ? -oldY : oldX);
                }
            }
            return newVector;
        }
    }

    private static class TaggedBakedQuad {
        final BakedQuad bakedQuad;
        final TextureType<?> textureType;
        final int spriteIndex;
        final int predicateIndex;
        final int quadIndex;

        private TaggedBakedQuad(BakedQuad bakedQuad, TextureType<?> textureType, int spriteIndex, int predicateIndex, int quadIndex){
            this.bakedQuad = bakedQuad;
            this.textureType = textureType;
            this.spriteIndex = spriteIndex;
            this.predicateIndex = predicateIndex;
            this.quadIndex = quadIndex;
        }
    }
}
