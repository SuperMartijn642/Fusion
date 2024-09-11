package com.supermartijn642.fusion.model.types.connecting;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.predicate.ConnectionDirection;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureLayout;
import com.supermartijn642.fusion.texture.types.connecting.ConnectingTextureSprite;
import com.supermartijn642.fusion.texture.types.connecting.TextureConnections;
import com.supermartijn642.fusion.texture.types.connecting.layouts.ConnectingTextureLayoutHandler;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class ConnectingBakedModel implements BakedModel, FabricBakedModel {

    private static final int VERTEX_SIZE, VERTEX_UV_OFFSET, VERTEX_POSITION_OFFSET;
    /**
     * Stores world space vector point in the up and right direction of the default texture orientation for each face
     */
    private static final int[][] DEFAULT_TEXTURE_ROTATIONS_UP = new int[6][];
    private static final int[][] DEFAULT_TEXTURE_ROTATIONS_RIGHT = new int[6][];

    static{
        // Amounts are in bytes, as a baked quad stores the data as ints and an int is 4 bytes, we divide by 4
        VertexFormat blockFormat = DefaultVertexFormat.BLOCK;
        VERTEX_SIZE = blockFormat.getVertexSize() / 4;
        VERTEX_UV_OFFSET = blockFormat.offsets.getInt(blockFormat.getElements().indexOf(DefaultVertexFormat.ELEMENT_UV)) / 4;
        VERTEX_POSITION_OFFSET = blockFormat.offsets.getInt(blockFormat.getElements().indexOf(DefaultVertexFormat.ELEMENT_POSITION)) / 4;

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
        return new float[]{Float.intBitsToFloat(quad.getVertices()[offset]), Float.intBitsToFloat(quad.getVertices()[offset + 1])};
    }

    private static float[] getPosition(BakedQuad quad, int vertexIndex){
        int offset = vertexIndex * VERTEX_SIZE + VERTEX_POSITION_OFFSET;
        return new float[]{
            Float.intBitsToFloat(quad.getVertices()[offset]),
            Float.intBitsToFloat(quad.getVertices()[offset + 1]),
            Float.intBitsToFloat(quad.getVertices()[offset + 2])
        };
    }

    private final Mesh blockMesh, itemMesh;
    private final List<QuadPredicates> predicates;
    private final List<TextureAtlasSprite> sprites;
    private final boolean hasAmbientOcclusion;
    private final boolean isGui3d;
    private final boolean usesBlockLight;
    private final TextureAtlasSprite particleIcon;
    private final ItemTransforms transforms;
    private final ItemOverrides overrides;

    public ConnectingBakedModel(List<ConnectingModelQuad> quads, boolean hasAmbientOcclusion, boolean isGui3d, boolean usesBlockLight, TextureAtlasSprite particleIcon, ItemTransforms transforms, ItemOverrides overrides){
        this.hasAmbientOcclusion = hasAmbientOcclusion;
        this.isGui3d = isGui3d;
        this.usesBlockLight = usesBlockLight;
        this.particleIcon = particleIcon;
        this.transforms = transforms;
        this.overrides = overrides;

        // Create block mesh from the quads
        MeshBuilder builder = RendererAccess.INSTANCE.getRenderer().meshBuilder();
        QuadEmitter emitter = builder.getEmitter();
        HashMap<QuadPredicates,Integer> predicates = new HashMap<>();
        HashMap<TextureAtlasSprite,Integer> sprites = new HashMap<>();
        for(ConnectingModelQuad quad : quads){
            // Some layouts need auxiliary quads, hence simply repeat the quad that many times
            Integer tag = null;
            int auxiliaryQuadCount = 0;
            if(quad.hasConnectingTexture()){
                Direction direction = quad.bakedQuad().getDirection();
                TextureOrientation orientation = findOrientation(quad.bakedQuad());
                ConnectionPredicate predicate = quad.connectionPredicate();
                // Get the number of auxiliary quads needed
                auxiliaryQuadCount = ConnectingTextureLayoutHandler.get(quad.getLayout()).getAuxiliaryQuadCount();
                // Give each combination of direction, orientation, and predicate a unique index
                int predicateIndex = predicates.computeIfAbsent(new QuadPredicates(direction, orientation, predicate), o -> predicates.size());
                // Give each sprite a unique index
                int spriteIndex = sprites.computeIfAbsent(quad.bakedQuad().getSprite(), o -> sprites.size());
                // Pack the predicate index and sprite index into the tag int
                tag = 1 | (predicateIndex << 5) | (spriteIndex << 16);
            }
            // Submit the quads
            RenderMaterial material = FusionClient.getRenderTypeMaterial(hasAmbientOcclusion, quad.renderType(), quad.emissive());
            for(int quadIndex = 0; quadIndex < auxiliaryQuadCount + 1; quadIndex++){
                emitter.fromVanilla(quad.bakedQuad(), material, quad.cullDirection());
                if(quad.lightEmission() != null){
                    for(int i = 0; i < 4; i++){
                        int sky = Math.max(quad.lightEmission(), LightTexture.sky(emitter.lightmap(i)));
                        int block = Math.max(quad.lightEmission(), LightTexture.block(emitter.lightmap(i)));
                        emitter.lightmap(i, LightTexture.pack(sky, block));
                    }
                }
                if(tag != null)
                    emitter.tag(tag | (quadIndex << 1)); // Add the quad index to the tag
                emitter.emit();
            }
        }
        this.blockMesh = builder.build();
        this.predicates = predicates.entrySet().stream().sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey).toList();
        this.sprites = sprites.entrySet().stream().sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey).toList();

        // Create the item mesh
        emitter = builder.getEmitter();
        MutableQuad mutableQuad = new MutableQuad();
        for(ConnectingModelQuad quad : quads){
            // Some layouts need auxiliary quads, hence simply repeat the quad that many times
            int auxiliaryQuadCount = 0;
            ConnectingTextureLayoutHandler layoutHandler = null;
            if(quad.hasConnectingTexture()){
                layoutHandler = ConnectingTextureLayoutHandler.get(quad.getLayout());
                // Get the number of auxiliary quads needed
                auxiliaryQuadCount = ConnectingTextureLayoutHandler.get(quad.getLayout()).getAuxiliaryQuadCount();
            }
            // Process and submit the quads
            RenderMaterial material = FusionClient.getRenderTypeMaterial(null, null, quad.emissive());
            for(int quadIndex = 0; quadIndex < auxiliaryQuadCount + 1; quadIndex++){
                emitter.fromVanilla(quad.bakedQuad(), material, quad.cullDirection());
                if(quad.lightEmission() != null){
                    for(int i = 0; i < 4; i++){
                        int sky = Math.max(quad.lightEmission(), LightTexture.sky(emitter.lightmap(i)));
                        int block = Math.max(quad.lightEmission(), LightTexture.block(emitter.lightmap(i)));
                        emitter.lightmap(i, LightTexture.pack(sky, block));
                    }
                }
                // Process the quad if it has a connecting texture
                // As item mesh does not depend on state, we can run the connecting texture processing immediately
                if(layoutHandler != null){
                    mutableQuad.set(emitter, TextureOrientation.NORMAL_0.vertexIndexPermutation);
                    layoutHandler.processItemQuad(quadIndex, mutableQuad, quad.bakedQuad().getSprite());
                }
                emitter.emit();
            }
        }
        this.itemMesh = builder.build();
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
        Direction direction = quad.getDirection();
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
    public boolean isVanillaAdapter(){
        return false;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random){
        return List.of();
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context){
        // If either level or position is not given, the connected textures cannot be updated, so just push the mesh
        if(blockView == null || pos == null || this.predicates.isEmpty()){
            context.meshConsumer().accept(this.blockMesh);
            return;
        }

        // Only compute connections for each predicate once
        TextureConnections[] connectionsCache = new TextureConnections[this.predicates.size()];
        //
        SurroundingBlockCache blockCache = new SurroundingBlockCache(blockView, pos, state);
        // Push a transform which maps any connecting texture quads to the correct uv
        MutableQuad mutableQuad = new MutableQuad();
        context.pushTransform(quad -> {
            if(quad.tag() != 0){
                // Unpack quad index, predicate index, and sprite index from the tag
                int tag = quad.tag();
                int quadIndex = (tag >> 1) & ((1 << 4) - 1);
                int predicateIndex = (tag >> 5) & ((1 << 10) - 1);
                int spriteIndex = (tag >> 16) & ((1 << 10) - 1);

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
                mutableQuad.set(quad, predicate.orientation.vertexIndexPermutation);
                return ConnectingTextureLayoutHandler.get(layout).processBlockQuad(quadIndex, mutableQuad, sprite, connections);
            }
            return true;
        });
        context.meshConsumer().accept(this.blockMesh);
        context.popTransform();
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
    public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context){
        context.meshConsumer().accept(this.itemMesh);
    }

    @Override
    public boolean useAmbientOcclusion(){
        return this.hasAmbientOcclusion;
    }

    @Override
    public boolean isGui3d(){
        return this.isGui3d;
    }

    @Override
    public boolean usesBlockLight(){
        return this.usesBlockLight;
    }

    @Override
    public boolean isCustomRenderer(){
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon(){
        return this.particleIcon;
    }

    @Override
    public ItemTransforms getTransforms(){
        return this.transforms;
    }

    @Override
    public ItemOverrides getOverrides(){
        return this.overrides;
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

    private enum TextureOrientation {
        NORMAL_0(false, 0), NORMAL_90(false, 1), NORMAL_180(false, 2), NORMAL_270(false, 3),
        FLIPPED_0(true, 0), FLIPPED_90(false, 1), FLIPPED_180(true, 2), FLIPPED_270(true, 3);

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
                    this.vertexIndexPermutation[i] = old2[(i - rotations + 4) % 4];
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
                        newVector[0] = ((positive ^ this.rotations == 3) ? 1 : -1) * (axis == Direction.Axis.Y ? -newVector[2] : newVector[1]);
                    if(axis != Direction.Axis.Y)
                        newVector[1] = ((positive ^ this.rotations == 3) ? 1 : -1) * (axis == Direction.Axis.Z ? -oldX : newVector[2]);
                    if(axis != Direction.Axis.Z)
                        newVector[2] = ((positive ^ this.rotations == 3) ? 1 : -1) * (axis == Direction.Axis.X ? -oldY : oldX);
                }
            }
            return newVector;
        }
    }
}
