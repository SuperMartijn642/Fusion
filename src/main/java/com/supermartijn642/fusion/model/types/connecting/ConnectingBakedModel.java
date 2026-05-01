package com.supermartijn642.fusion.model.types.connecting;

import com.supermartijn642.fusion.api.predicate.ConnectionDirection;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureLayout;
import com.supermartijn642.fusion.model.quad.QuadAccess;
import com.supermartijn642.fusion.texture.types.connecting.StitchedConnectingTextureData;
import com.supermartijn642.fusion.texture.types.connecting.TextureConnections;
import com.supermartijn642.fusion.texture.types.connecting.layouts.ConnectingTextureLayoutHandler;
import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureType;
import com.supermartijn642.fusion.texture.types.random.RandomTextureType;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;

import java.util.*;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class ConnectingBakedModel implements BlockStateModel {

    public static final ModelProperty<BlockAndTintGetter> LEVEL_PROPERTY = new ModelProperty<>();
    public static final ModelProperty<BlockPos> POSITION_PROPERTY = new ModelProperty<>();
    public static final ModelProperty<BlockState> STATE_PROPERTY = new ModelProperty<>();
    /**
     * Stores world space vector point in the up and right direction of the default texture orientation for each face
     */
    private static final int[][] DEFAULT_TEXTURE_ROTATIONS_UP = new int[6][];
    private static final int[][] DEFAULT_TEXTURE_ROTATIONS_RIGHT = new int[6][];
    private static final Direction[] CULL_DIRECTIONS = {null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    static{
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

    private final List<TaggedBakedQuad>[] mesh;
    private final BlockStateModelPart fallbackMesh;
    private final List<QuadPredicates> predicates;
    private final List<SpriteInstance> sprites;
    private final boolean hasSpecialQuads;
    private final Boolean hasAmbientOcclusion;
    private final Material.Baked particleIcon;
    private final int materialFlags;

    public ConnectingBakedModel(List<ConnectingModelQuad> quads, Boolean hasAmbientOcclusion, Material.Baked particleIcon){
        this.hasAmbientOcclusion = hasAmbientOcclusion;
        this.particleIcon = particleIcon;

        // Create block mesh
        //noinspection unchecked
        List<TaggedBakedQuad>[] mesh = new List[7];
        HashMap<QuadPredicates,Integer> predicates = new HashMap<>();
        HashMap<SpriteInstance,Integer> sprites = new HashMap<>();
        boolean hasSpecialQuads = false;
        int materialFlags = 0;
        for(ConnectingModelQuad quad : quads){
            TextureType<?,?> textureType = quad.textureType();
            int spriteIndex = -1;
            int predicateIndex = -1;
            // Some layouts need auxiliary quads, hence simply repeat the quad that many times
            int auxiliaryQuadCount = 0;
            if(quad.hasConnectingTexture()){
                Direction direction = quad.quad().facing();
                TextureOrientation orientation = findOrientation(quad.quad());
                ConnectionPredicate predicate = quad.connectionPredicate();
                // Get the number of auxiliary quads needed
                auxiliaryQuadCount = ConnectingTextureLayoutHandler.get(quad.getLayout()).getAuxiliaryQuadCount();
                // Give each combination of direction, orientation, and predicate a unique index
                predicateIndex = predicates.computeIfAbsent(new QuadPredicates(direction, orientation, predicate), o -> predicates.size());
                // Give each sprite a unique index
                spriteIndex = sprites.computeIfAbsent(quad.spriteInstance(), o -> sprites.size());
            }
            // Tag quads which need additional processing
            if(quad.textureType() == DefaultTextureTypes.RANDOM || quad.textureType() == DefaultTextureTypes.CONTINUOUS){
                // Give each sprite a unique index
                spriteIndex = sprites.computeIfAbsent(quad.spriteInstance(), o -> sprites.size());
                hasSpecialQuads = true;
            }
            // Submit the quads
            for(int quadIndex = 0; quadIndex < auxiliaryQuadCount + 1; quadIndex++){
                // Add the block quad
                TaggedBakedQuad finishedQuad = new TaggedBakedQuad(quad.quad(), textureType, spriteIndex, predicateIndex, quadIndex);
                int cullIndex = cullIndex(quad.cullDirection());
                if(mesh[cullIndex] == null)
                    mesh[cullIndex] = new ArrayList<>();
                mesh[cullIndex].add(finishedQuad);
            }
            // Update material flags
            if(quad.quad().chunkLayer().translucent())
                materialFlags |= BakedQuad.FLAG_TRANSLUCENT;
            if(quad.quad().sprite().contents().isAnimated())
                materialFlags |= BakedQuad.FLAG_ANIMATED;
        }
        this.mesh = mesh;
        this.predicates = predicates.entrySet().stream().sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey).toList();
        this.sprites = sprites.entrySet().stream().sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey).toList();
        this.hasSpecialQuads = hasSpecialQuads;
        this.materialFlags = materialFlags;

        // Create a mesh of unprocessed quads
        //noinspection unchecked
        List<BakedQuad>[] bakedQuads = Arrays.stream(mesh)
            .map(l -> l == null ? List.of() : l.stream().map(q -> q.quad.toBakedQuad()).toList())
            .toArray(List[]::new);
        this.fallbackMesh = new BlockStateModelPart() {
            @Override
            public List<BakedQuad> getQuads(@Nullable Direction cullDirection){
                return bakedQuads[cullIndex(cullDirection)];
            }

            @Override
            public boolean useAmbientOcclusion(){
                return hasAmbientOcclusion != Boolean.FALSE;
            }

            @Override
            public Material.Baked particleMaterial(){
                return particleIcon;
            }

            @Override
            public @BakedQuad.MaterialFlags int materialFlags(){
                return ConnectingBakedModel.this.materialFlags;
            }
        };
    }

    private static TextureOrientation findOrientation(QuadAccess quad){
        // First determine the texture orientation relative to the vertex indices
        // Compare the angle between directions v1 to v2 and v1 to v3, to check whether the texture is flipped
        double angle1to2 = Math.atan2(quad.v(1) - quad.v(0), quad.u(1) - quad.u(0)), angle1to3 = Math.atan2(quad.v(2) - quad.v(0), quad.u(2) - quad.u(0));
        boolean textureFlipped = (angle1to2 - angle1to3 + 4 * Math.PI) % (2 * Math.PI) < Math.PI;
        // Find the top-left-most-ish index, if we assume the uvs form a grid-aligned square this should work
        int topLeftMostIndex = 0;
        float minUV = quad.u(0) + quad.v(0);
        for(int i = 1; i < 4; i++){
            if(quad.u(i) + quad.v(i) < minUV)
                topLeftMostIndex = i;
        }
        int textureRotation = textureFlipped ? topLeftMostIndex : (4 - topLeftMostIndex) % 4;

        // Determine the vertex indices rotation relative to the block face
        Vector3fc[] positions3d = {quad.position(0), quad.position(1), quad.position(2), quad.position(3)};
        // Project the 3d positions onto the plane perpendicular to the facing of the quad
        float[][] pos = new float[4][2];
        Direction direction = quad.facing();
        for(int i = 0; i < 4; i++){
            if(direction == Direction.DOWN){
                pos[i][0] = positions3d[i].x();
                pos[i][1] = -positions3d[i].z();
            }else if(direction == Direction.UP){
                pos[i][0] = positions3d[i].x();
                pos[i][1] = positions3d[i].z();
            }else if(direction == Direction.NORTH){
                pos[i][0] = -positions3d[i].x();
                pos[i][1] = -positions3d[i].y();
            }else if(direction == Direction.SOUTH){
                pos[i][0] = positions3d[i].x();
                pos[i][1] = -positions3d[i].y();
            }else if(direction == Direction.WEST){
                pos[i][0] = positions3d[i].z();
                pos[i][1] = -positions3d[i].y();
            }else if(direction == Direction.EAST){
                pos[i][0] = -positions3d[i].z();
                pos[i][1] = -positions3d[i].y();
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
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts, ModelData modelData){
        BlockAndTintGetter level = modelData.get(LEVEL_PROPERTY);
        BlockPos pos = modelData.get(POSITION_PROPERTY);
        BlockState state = modelData.get(STATE_PROPERTY);
        this.collectParts(level, pos, state, random, parts);
    }

    public void collectParts(BlockAndTintGetter blockView, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts){
        // If either level or position is not given, the connected textures cannot be updated, so don't process them
        boolean processConnectingTextures = blockView != null && pos != null && !this.predicates.isEmpty();
        if(!processConnectingTextures && !this.hasSpecialQuads){
            parts.add(this.fallbackMesh);
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
        //noinspection unchecked
        List<BakedQuad>[] processedMesh = new List[7];
        for(Direction cullDirection : CULL_DIRECTIONS){
            List<TaggedBakedQuad> quads = this.mesh[cullIndex(cullDirection)];
            if(quads == null){
                processedMesh[cullIndex(cullDirection)] = List.of();
                continue;
            }
            ArrayList<BakedQuad> bakedQuads = new ArrayList<>(quads.size());

            for(TaggedBakedQuad quad : quads){
                // Process special texture type quads
                if(pos != null && (quad.textureType == DefaultTextureTypes.RANDOM || quad.textureType == DefaultTextureTypes.CONTINUOUS)){
                    // Get the sprite
                    SpriteInstance sprite = this.sprites.get(quad.spriteIndex);

                    mutableQuad.copyFrom(quad.quad);
                    if(quad.textureType == DefaultTextureTypes.RANDOM)
                        // Handle random texture type
                        RandomTextureType.processQuad(mutableQuad, pos, quad.quad.facing(), random, sprite);
                    else
                        // Handle continuous texture type
                        ContinuousTextureType.processQuad(mutableQuad, pos, quad.quad.facing(), sprite);
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
                    SpriteInstance sprite = this.sprites.get(spriteIndex);
                    StitchedConnectingTextureData data = (StitchedConnectingTextureData)sprite.getTexture().getCustomData();
                    ConnectingTextureLayout layout = data.getLayout();

                    // Remap the quad's uv
                    mutableQuad.copyFrom(quad.quad);
                    mutableQuad.setPermutation(predicate.orientation.vertexIndexPermutation);
                    boolean keepQuad = ConnectingTextureLayoutHandler.get(layout).processBlockQuad(quadIndex, mutableQuad, sprite, data, connections);
                    mutableQuad.resetPermutation();
                    if(keepQuad)
                        bakedQuads.add(mutableQuad.toBakedQuad());
                }else
                    bakedQuads.add(quad.quad.toBakedQuad());
            }

            processedMesh[cullIndex(cullDirection)] = bakedQuads;
        }

        // Create the model part for the mesh
        parts.add(new BlockStateModelPart() {
            @Override
            public List<BakedQuad> getQuads(@Nullable Direction cullDirection){
                return processedMesh[cullIndex(cullDirection)];
            }

            @Override
            public boolean useAmbientOcclusion(){
                return ConnectingBakedModel.this.hasAmbientOcclusion != Boolean.FALSE;
            }

            @Override
            public Material.Baked particleMaterial(){
                return ConnectingBakedModel.this.particleIcon;
            }

            @Override
            public @BakedQuad.MaterialFlags int materialFlags(){
                return ConnectingBakedModel.this.materialFlags;
            }
        });
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts){
        this.collectParts(random, parts, ModelData.EMPTY);
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
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data){
        return ModelData.builder()
            .with(LEVEL_PROPERTY, level)
            .with(POSITION_PROPERTY, pos)
            .with(STATE_PROPERTY, state)
            .build();
    }

    @Override
    public Material.Baked particleMaterial(){
        return this.particleIcon;
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags(){
        return this.materialFlags;
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
                    newVector[0] = positive ? -vector[1] : vector[1];
                    newVector[1] = positive ? -vector[0] : vector[0];
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
        final QuadAccess quad;
        final TextureType<?,?> textureType;
        final int spriteIndex;
        final int predicateIndex;
        final int quadIndex;

        private TaggedBakedQuad(QuadAccess quad, TextureType<?,?> textureType, int spriteIndex, int predicateIndex, int quadIndex){
            this.quad = quad;
            this.textureType = textureType;
            this.spriteIndex = spriteIndex;
            this.predicateIndex = predicateIndex;
            this.quadIndex = quadIndex;
        }
    }
}
