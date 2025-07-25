package com.supermartijn642.fusion.model.types.connecting;

import com.google.common.collect.ImmutableList;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.predicate.ConnectionDirection;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureLayout;
import com.supermartijn642.fusion.model.types.base.CustomRenderTypeBakedModel;
import com.supermartijn642.fusion.texture.types.connecting.ConnectingTextureSprite;
import com.supermartijn642.fusion.texture.types.connecting.TextureConnections;
import com.supermartijn642.fusion.texture.types.connecting.layouts.ConnectingTextureLayoutHandler;
import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureSprite;
import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureType;
import com.supermartijn642.fusion.texture.types.random.RandomTextureSprite;
import com.supermartijn642.fusion.texture.types.random.RandomTextureType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.MinecraftForgeClient;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class ConnectingBakedModel implements IBakedModel, CustomRenderTypeBakedModel {

    public static final ThreadLocal<SurroundingBlockCache> BLOCK_CACHE = new ThreadLocal<>();
    /**
     * Stores world space vector point in the up and right direction of the default texture orientation for each face
     */
    private static final int[][] DEFAULT_TEXTURE_ROTATIONS_UP = new int[6][];
    private static final int[][] DEFAULT_TEXTURE_ROTATIONS_RIGHT = new int[6][];

    static{
        for(EnumFacing direction : EnumFacing.values()){
            int upX = 0, upY = 0, upZ = 0, rightX = 0, rightY = 0, rightZ = 0;
            if(direction == EnumFacing.DOWN){
                upZ = 1;
                rightX = 1;
            }else if(direction == EnumFacing.UP){
                upZ = -1;
                rightX = 1;
            }else if(direction == EnumFacing.NORTH){
                upY = 1;
                rightX = -1;
            }else if(direction == EnumFacing.SOUTH){
                upY = 1;
                rightX = 1;
            }else if(direction == EnumFacing.WEST){
                upY = 1;
                rightZ = 1;
            }else if(direction == EnumFacing.EAST){
                upY = 1;
                rightZ = -1;
            }
            DEFAULT_TEXTURE_ROTATIONS_UP[direction.ordinal()] = new int[]{upX, upY, upZ};
            DEFAULT_TEXTURE_ROTATIONS_RIGHT[direction.ordinal()] = new int[]{rightX, rightY, rightZ};
        }
    }

    private static float[] getUV(BakedQuad quad, int vertexIndex){
        VertexFormat format = quad.getFormat();
        int offset = vertexIndex * format.getIntegerSize() + format.getUvOffsetById(0) / 4;
        return new float[]{Float.intBitsToFloat(quad.getVertexData()[offset]), Float.intBitsToFloat(quad.getVertexData()[offset + 1])};
    }

    private static float[] getPosition(BakedQuad quad, int vertexIndex){
        VertexFormat format = quad.getFormat();
        int offset = vertexIndex * format.getIntegerSize() + format.getOffset(format.getElements().indexOf(DefaultVertexFormats.POSITION_3F));
        return new float[]{
            Float.intBitsToFloat(quad.getVertexData()[offset]),
            Float.intBitsToFloat(quad.getVertexData()[offset + 1]),
            Float.intBitsToFloat(quad.getVertexData()[offset + 2])
        };
    }

    private final List<TaggedBakedQuad>[] completeBlockMesh;
    private final List<TaggedBakedQuad>[][] blockMesh; // indexed by render layer ordinal, cull direction
    private final List<BakedQuad> itemMesh;
    private final List<BlockRenderLayer> blockRenderTypes;
    private final boolean shouldCheckOriginalBlockRenderTypes;
    private final List<QuadPredicates> predicates;
    private final List<TextureAtlasSprite> sprites;
    private final boolean hasSpecialQuads;
    private final boolean hasAmbientOcclusion;
    private final boolean isGui3d;
    private final TextureAtlasSprite particleIcon;
    private final ItemCameraTransforms transforms;
    private final ItemOverrideList overrides;

    public ConnectingBakedModel(List<ConnectingModelQuad> quads, boolean hasAmbientOcclusion, boolean isGui3d, TextureAtlasSprite particleIcon, ItemCameraTransforms transforms, ItemOverrideList overrides){
        this.hasAmbientOcclusion = hasAmbientOcclusion;
        this.isGui3d = isGui3d;
        this.particleIcon = particleIcon;
        this.transforms = transforms;
        this.overrides = overrides;

        // Create block and item meshes from the quads
        //noinspection unchecked
        List<TaggedBakedQuad>[][] blockMesh = new List[BlockRenderLayer.values().length + 1][];
        Set<BlockRenderLayer> blockRenderTypes = new LinkedHashSet<>();
        List<BakedQuad> itemMesh = new ArrayList<>();
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
                EnumFacing direction = quad.bakedQuad().getFace();
                TextureOrientation orientation = findOrientation(quad.bakedQuad());
                ConnectionPredicate predicate = quad.connectionPredicate();
                // Get the number of auxiliary quads needed
                auxiliaryQuadCount = ConnectingTextureLayoutHandler.get(quad.getLayout()).getAuxiliaryQuadCount();
                // Give each combination of direction, orientation, and predicate a unique index
                predicateIndex = predicates.computeIfAbsent(new QuadPredicates(direction, orientation, predicate), o -> predicates.size());
                // Give each sprite a unique index
                spriteIndex = sprites.computeIfAbsent(quad.bakedQuad().getSprite(), o -> sprites.size());
            }
            // Tag quads which need additional processing
            if(quad.textureType() == DefaultTextureTypes.RANDOM || quad.textureType() == DefaultTextureTypes.CONTINUOUS){
                // Give each sprite a unique index
                spriteIndex = sprites.computeIfAbsent(quad.bakedQuad().getSprite(), o -> sprites.size());
                hasSpecialQuads = true;
            }
            // Submit the quads
            for(int quadIndex = 0; quadIndex < auxiliaryQuadCount + 1; quadIndex++){
                mutableQuad.fillFromBakedQuad(quad.bakedQuad());
                mutableQuad.emissive(quad.emissive());
                if(quad.lightEmission() != null){
                    for(int i = 0; i < 4; i++){
                        int sky = Math.max(quad.lightEmission(), mutableQuad.lightmap(i) >> 20 & 0xffff);
                        int block = Math.max(quad.lightEmission(), (mutableQuad.lightmap(i) & 0xffff) >> 4);
                        mutableQuad.lightmap(i, (sky << 20 | block << 4));
                    }
                }

                // Add the block quad
                TaggedBakedQuad finishedQuad = new TaggedBakedQuad(mutableQuad.toBakedQuad(), textureType, spriteIndex, predicateIndex, quadIndex);
                BlockRenderLayer renderType = FusionClient.getRenderTypeMaterial(quad.renderType());
                blockRenderTypes.add(renderType);
                int cullIndex = cullIndex(quad.cullDirection());
                List<TaggedBakedQuad>[] mesh = blockMesh[renderType == null ? 0 : renderType.ordinal() + 1];
                if(mesh == null){
                    // noinspection unchecked
                    mesh = new List[7];
                    blockMesh[renderType == null ? 0 : renderType.ordinal() + 1] = mesh;
                }
                if(mesh[cullIndex] == null)
                    mesh[cullIndex] = new ArrayList<>();
                mesh[cullIndex].add(finishedQuad);

                // Add the item quad
                // Process the quad if it has a connecting texture
                // As item mesh does not depend on state, we can run the connecting texture processing immediately
                if(quad.hasConnectingTexture()){
                    mutableQuad.set(TextureOrientation.NORMAL_0.vertexIndexPermutation);
                    boolean keepQuad = ConnectingTextureLayoutHandler.get(quad.getLayout()).processItemQuad(quadIndex, mutableQuad, (ConnectingTextureSprite)quad.bakedQuad().getSprite());
                    mutableQuad.resetPermutation();
                    if(!keepQuad)
                        continue;
                }
                itemMesh.add(mutableQuad.toBakedQuad());
            }
        }
        this.blockMesh = blockMesh;
        this.blockRenderTypes = blockRenderTypes.stream().filter(Objects::nonNull).collect(Collectors.toList());
        this.shouldCheckOriginalBlockRenderTypes = blockRenderTypes.contains(null);
        this.itemMesh = ImmutableList.copyOf(itemMesh);
        this.predicates = predicates.entrySet().stream().sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey).collect(Collectors.toList());
        this.sprites = sprites.entrySet().stream().sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey).collect(Collectors.toList());
        this.hasSpecialQuads = hasSpecialQuads;

        //noinspection unchecked
        this.completeBlockMesh = new List[7];
        for(int i = 0; i < 7; i++){
            int cullIndex = i;
            this.completeBlockMesh[i] = Arrays.stream(this.blockMesh).filter(Objects::nonNull).map(arr -> arr[cullIndex]).filter(Objects::nonNull).flatMap(List::stream).collect(Collectors.toList());
        }
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
        EnumFacing direction = quad.getFace();
        for(int i = 0; i < 4; i++){
            if(direction == EnumFacing.DOWN){
                pos[i][0] = positions3d[i][0];
                pos[i][1] = -positions3d[i][2];
            }else if(direction == EnumFacing.UP){
                pos[i][0] = positions3d[i][0];
                pos[i][1] = positions3d[i][2];
            }else if(direction == EnumFacing.NORTH){
                pos[i][0] = -positions3d[i][0];
                pos[i][1] = -positions3d[i][1];
            }else if(direction == EnumFacing.SOUTH){
                pos[i][0] = positions3d[i][0];
                pos[i][1] = -positions3d[i][1];
            }else if(direction == EnumFacing.WEST){
                pos[i][0] = positions3d[i][2];
                pos[i][1] = -positions3d[i][1];
            }else if(direction == EnumFacing.EAST){
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

    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing cullDirection, long seed, @Nullable BlockRenderLayer renderType){
        // If the block state is null, assume this call is intended for item rendering
        if(state == null)
            return cullDirection == null ? this.itemMesh : Collections.emptyList();

        // If render type is not set, use all block quads
        List<TaggedBakedQuad> quads;
        if(renderType == null)
            quads = this.completeBlockMesh[cullIndex(cullDirection)];
        else{
            List<TaggedBakedQuad>[] mesh = this.blockMesh[renderType.ordinal() + 1];
            quads = mesh == null ? null : mesh[cullIndex(cullDirection)];
            if(this.shouldCheckOriginalBlockRenderTypes && state.getBlock().getBlockLayer() == renderType){
                mesh = this.blockMesh[0];
                List<TaggedBakedQuad> additionalQuads = mesh == null ? null : mesh[cullIndex(cullDirection)];
                if(additionalQuads != null){
                    if(quads == null)
                        quads = additionalQuads;
                    else{
                        List<TaggedBakedQuad> combined = new ArrayList<>(quads.size() + additionalQuads.size());
                        combined.addAll(quads);
                        combined.addAll(additionalQuads);
                        quads = combined;
                    }
                }
            }
            if(quads == null)
                quads = Collections.emptyList();
        }

        // If there's no connecting textures and no special quads, just return the quads as is
        if(this.predicates.isEmpty() && !this.hasSpecialQuads){
            List<BakedQuad> bakedQuads = new ArrayList<>(quads.size());
            for(TaggedBakedQuad quad : quads)
                bakedQuads.add(quad.bakedQuad);
            return bakedQuads;
        }

        // Get the block cache from the model data
        SurroundingBlockCache blockCache = BLOCK_CACHE.get();
        // If the block cache is absent, the connected textures cannot be updated, so just push the mesh
        if(blockCache == null){
            List<BakedQuad> bakedQuads = new ArrayList<>(quads.size());
            for(TaggedBakedQuad quad : quads)
                bakedQuads.add(quad.bakedQuad);
            return bakedQuads;
        }
        // Get the position from the model data
        BlockPos pos = blockCache.getRealPos();
        // Make sure to use the block state argument for the model's own block
        blockCache.setSelf(state);

        // Only compute connections for each predicate once
        TextureConnections[] connectionsCache = new TextureConnections[this.predicates.size()];

        // Push a transform which maps any connecting texture quads to the correct uv
        ArrayList<BakedQuad> bakedQuads = new ArrayList<>(quads.size());
        Random random = null;
        OrientedMutableQuad mutableQuad = new OrientedMutableQuad();
        for(TaggedBakedQuad quad : quads){
            // Process special texture type quads
            if(pos != null && (quad.textureType == DefaultTextureTypes.RANDOM || quad.textureType == DefaultTextureTypes.CONTINUOUS)){
                // Get the sprite
                TextureAtlasSprite sprite = this.sprites.get(quad.spriteIndex);

                mutableQuad.fillFromBakedQuad(quad.bakedQuad);
                mutableQuad.resetPermutation();
                if(quad.textureType == DefaultTextureTypes.RANDOM){
                    // Handle random texture type
                    if(random == null) random = new Random();
                    RandomTextureType.processQuad(mutableQuad, pos, quad.bakedQuad.getFace(), random, (RandomTextureSprite)sprite);
                }else
                    // Handle continuous texture type
                    ContinuousTextureType.processQuad(mutableQuad, pos, quad.bakedQuad.getFace(), (ContinuousTextureSprite)sprite);
                bakedQuads.add(mutableQuad.toBakedQuad());
            }
            // Process connecting textures
            else if(quad.textureType == DefaultTextureTypes.CONNECTING){
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
        return bakedQuads;
    }

    private static TextureConnections computeConnections(QuadPredicates predicates, SurroundingBlockCache blocks){
        ConnectionPredicate predicate = predicates.predicate;
        EnumFacing face = predicates.direction;
        TextureOrientation orientation = predicates.orientation;

        // Get the up and right vectors for the way textures are rotated by default for quad's facing
        int[] up = orientation.transformWorldVector(DEFAULT_TEXTURE_ROTATIONS_UP[face.ordinal()], face);
        int[] right = orientation.transformWorldVector(DEFAULT_TEXTURE_ROTATIONS_RIGHT[face.ordinal()], face);

        boolean connectTop = shouldConnect(predicate, blocks, face, orientation.worldToTexture[0], up[0], up[1], up[2]);
        boolean connectTopRight = shouldConnect(predicate, blocks, face, orientation.worldToTexture[1], up[0] + right[0], up[1] + right[1], up[2] + right[2]);
        boolean connectRight = shouldConnect(predicate, blocks, face, orientation.worldToTexture[2], right[0], right[1], right[2]);
        boolean connectBottomRight = shouldConnect(predicate, blocks, face, orientation.worldToTexture[3], -up[0] + right[0], -up[1] + right[1], -up[2] + right[2]);
        boolean connectBottom = shouldConnect(predicate, blocks, face, orientation.worldToTexture[4], -up[0], -up[1], -up[2]);
        boolean connectBottomLeft = shouldConnect(predicate, blocks, face, orientation.worldToTexture[5], -up[0] - right[0], -up[1] - right[1], -up[2] - right[2]);
        boolean connectLeft = shouldConnect(predicate, blocks, face, orientation.worldToTexture[6], -right[0], -right[1], -right[2]);
        boolean connectTopLeft = shouldConnect(predicate, blocks, face, orientation.worldToTexture[7], up[0] - right[0], up[1] - right[1], up[2] - right[2]);
        return new TextureConnections(connectTop, connectTopRight, connectRight, connectBottomRight, connectBottom, connectBottomLeft, connectLeft, connectTopLeft);
    }

    private static boolean shouldConnect(ConnectionPredicate predicate, SurroundingBlockCache blocks, EnumFacing face, ConnectionDirection direction, int neighborX, int neighborY, int neighborZ){
        IBlockAccess level = blocks.getLevel();
        BlockPos position = blocks.getRealPos();
        IBlockState self = blocks.getCenter();
        IBlockState neighborState = blocks.getState(neighborX, neighborY, neighborZ);
        IBlockState stateInFront = blocks.getState(neighborX + face.getFrontOffsetX(), neighborY + face.getFrontOffsetY(), neighborZ + face.getFrontOffsetZ());
        return predicate.shouldConnect(level, position, face, self, neighborState, stateInFront, direction);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing cullDirection, long seed){
        return this.getQuads(state, cullDirection, seed, MinecraftForgeClient.getRenderLayer());
    }

    @Override
    public List<BlockRenderLayer> getBlockRenderTypes(){
        return this.blockRenderTypes;
    }

    @Override
    public boolean isAmbientOcclusion(){
        return this.hasAmbientOcclusion;
    }

    @Override
    public boolean isGui3d(){
        return this.isGui3d;
    }

    @Override
    public boolean isBuiltInRenderer(){
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture(){
        return this.particleIcon;
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms(){
        return this.transforms;
    }

    @Override
    public ItemOverrideList getOverrides(){
        return this.overrides;
    }

    private static int cullIndex(EnumFacing cullDirection){
        return cullDirection == null ? 0 : cullDirection.ordinal() + 1;
    }

    private static class QuadPredicates {
        public final EnumFacing direction;
        public final TextureOrientation orientation;
        public final ConnectionPredicate predicate;

        private QuadPredicates(EnumFacing direction, TextureOrientation orientation, ConnectionPredicate predicate){
            this.direction = direction;
            this.orientation = orientation;
            this.predicate = predicate;
        }

        @Override
        public final boolean equals(Object o){
            if(this == o) return true;
            if(!(o instanceof QuadPredicates)) return false;

            QuadPredicates that = (QuadPredicates)o;
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

    private enum TextureOrientation {
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

        public int[] transformWorldVector(int[] vector, EnumFacing face){ // TODO improve this
            if(!this.flipped && this.rotations == 0)
                return vector;
            int[] newVector = Arrays.copyOf(vector, vector.length);
            EnumFacing.Axis axis = face.getAxis();
            boolean positive = face.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE;
            if(this.flipped){
                if(face.getAxis() == EnumFacing.Axis.X){
                    newVector[1] = positive ? vector[2] : -vector[2];
                    newVector[2] = positive ? vector[1] : -vector[1];
                }
                if(face.getAxis() == EnumFacing.Axis.Y){
                    newVector[0] = positive ? vector[2] : -vector[2];
                    newVector[2] = positive ? vector[0] : -vector[0];
                }
                if(face.getAxis() == EnumFacing.Axis.Z){
                    newVector[0] = positive ? vector[1] : -vector[1];
                    newVector[1] = positive ? vector[0] : -vector[0];
                }
            }
            if(this.rotations > 0){
                if(this.rotations == 2){
                    if(axis != EnumFacing.Axis.X)
                        newVector[0] = -newVector[0];
                    if(axis != EnumFacing.Axis.Y)
                        newVector[1] = -newVector[1];
                    if(axis != EnumFacing.Axis.Z)
                        newVector[2] = -newVector[2];
                }else{
                    int oldX = newVector[0];
                    int oldY = newVector[1];
                    if(axis != EnumFacing.Axis.X)
                        newVector[0] = ((positive ^ this.rotations == 3) ? 1 : -1) * (axis == EnumFacing.Axis.Y ? -newVector[2] : newVector[1]);
                    if(axis != EnumFacing.Axis.Y)
                        newVector[1] = ((positive ^ this.rotations == 3) ? 1 : -1) * (axis == EnumFacing.Axis.Z ? -oldX : newVector[2]);
                    if(axis != EnumFacing.Axis.Z)
                        newVector[2] = ((positive ^ this.rotations == 3) ? 1 : -1) * (axis == EnumFacing.Axis.X ? -oldY : oldX);
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
