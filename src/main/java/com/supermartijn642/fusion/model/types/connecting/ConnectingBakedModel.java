package com.supermartijn642.fusion.model.types.connecting;

import com.mojang.math.Vector3f;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.model.types.connecting.predicates.ConnectionDirection;
import com.supermartijn642.fusion.api.model.types.connecting.predicates.ConnectionPredicate;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.texture.types.connecting.StitchedConnectingTextureData;
import com.supermartijn642.fusion.texture.types.connecting.TextureConnections;
import com.supermartijn642.fusion.texture.types.connecting.layouts.ConnectingTextureLayoutHandler;
import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureType;
import com.supermartijn642.fusion.texture.types.random.RandomTextureType;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
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

    /**
     * Stores world space vector point in the up and right direction of the default texture orientation for each face
     */
    private static final int[][] DEFAULT_TEXTURE_ROTATIONS_UP = new int[6][];
    private static final int[][] DEFAULT_TEXTURE_ROTATIONS_RIGHT = new int[6][];

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

    public static final Direction[] CULL_DIRECTIONS = {null, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private final List<Part> parts;
    private final TextureAtlasSprite particleSprite;
    private final BlockModel.GuiLight guiLight;
    private final boolean isGui3d;
    private final ItemTransforms transforms;

    public ConnectingBakedModel(List<Part> parts, TextureAtlasSprite particleSprite, BlockModel.GuiLight guiLight, boolean isGui3d, ItemTransforms transforms){
        this.parts = parts;
        this.particleSprite = particleSprite;
        this.guiLight = guiLight;
        this.isGui3d = isGui3d;
        this.transforms = transforms;
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter level, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context){
        // Cache computed connections
        Map<QuadPredicatesKey,TextureConnections> computedConnections = new HashMap<>();
        // Cache the surrounding blocks
        SurroundingBlockCache blockCache = new SurroundingBlockCache(level, pos, state);
        // Create the model parts
        QuadEmitter emitter = context.getEmitter();
        for(Part part : this.parts){
            for(Direction cullDirection : CULL_DIRECTIONS){
                // Set cull direction
                emitter.cullFace(cullDirection);

                OrientedMutableQuad mutableQuad = null;
                for(ConnectingModelQuads.Entry entry : part.quads().get(cullDirection)){
                    QuadAccess quad = entry.quad();
                    // Get the sprite instance
                    SpriteInstance sprite = SpriteHelper.getSpriteInstance(quad.sprite());
                    if(sprite == null){
                        quad.toFrapiQuad(emitter);
                        emitter.emit();
                        continue;
                    }

                    // Process special texture type quads
                    TextureType<?,?> textureType = sprite.getTexture().getTextureType();
                    if(textureType == DefaultTextureTypes.CONNECTING){
                        // Check if the connections have already been computed, otherwise compute them
                        TextureConnections connections = computedConnections.computeIfAbsent(
                            entry.predicateKey(),
                            key -> computeConnections(key, blockCache)
                        );

                        // Get the sprite data
                        StitchedConnectingTextureData data = (StitchedConnectingTextureData)sprite.getTexture().getCustomData();
                        ConnectingTextureLayoutHandler layoutHandler = ConnectingTextureLayoutHandler.get(data.getLayout());

                        // Create quads
                        if(mutableQuad == null)
                            mutableQuad = new OrientedMutableQuad();
                        for(int i = 0; i < layoutHandler.getAuxiliaryQuadCount() + 1; i++){
                            mutableQuad.copyFrom(quad);
                            mutableQuad.setPermutation(entry.orientation().vertexIndexPermutation);
                            boolean keepQuad = layoutHandler.processBlockQuad(i, mutableQuad, sprite, data, connections);
                            mutableQuad.resetPermutation();
                            if(keepQuad){
                                mutableQuad.toFrapiQuad(emitter);
                                emitter.emit();
                            }
                        }
                    }else if(textureType == DefaultTextureTypes.RANDOM){
                        if(mutableQuad == null)
                            mutableQuad = new OrientedMutableQuad();
                        mutableQuad.copyFrom(quad);
                        RandomTextureType.processQuad(mutableQuad, pos, quad.facing(), randomSupplier, sprite);
                        mutableQuad.toFrapiQuad(emitter);
                        emitter.emit();
                    }else if(textureType == DefaultTextureTypes.CONTINUOUS){
                        if(mutableQuad == null)
                            mutableQuad = new OrientedMutableQuad();
                        mutableQuad.copyFrom(quad);
                        ContinuousTextureType.processQuad(mutableQuad, pos, quad.facing(), sprite);
                        mutableQuad.toFrapiQuad(emitter);
                        emitter.emit();
                    }else{
                        quad.toFrapiQuad(emitter);
                        emitter.emit();
                    }
                }
            }
        }
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context){
        // Emit quads
        QuadEmitter emitter = context.getEmitter();
        for(Part part : this.parts){
            for(ConnectingModelQuads.Entry entry : part.quads().all()){
                entry.quad().toFrapiQuad(emitter);
                emitter.emit();
            }
        }
    }

    @Override
    public boolean isVanillaAdapter(){
        return false;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction direction, RandomSource randomSource){
        return List.of();
    }

    private static TextureConnections computeConnections(QuadPredicatesKey predicates, SurroundingBlockCache blocks){
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
    public TextureAtlasSprite getParticleIcon(){
        return this.particleSprite;
    }

    @Override
    public boolean useAmbientOcclusion(){
        return true; // Ambient occlusion is handled by quads themselves
    }

    @Override
    public boolean isGui3d(){
        return this.isGui3d;
    }

    @Override
    public boolean usesBlockLight(){
        return this.guiLight.lightLikeBlock();
    }

    @Override
    public ItemTransforms getTransforms(){
        return this.transforms;
    }

    @Override
    public boolean isCustomRenderer(){
        return false;
    }

    @Override
    public ItemOverrides getOverrides(){
        return ItemOverrides.EMPTY;
    }

    static TextureOrientation findOrientation(QuadAccess quad){
        // First determine the texture orientation relative to the vertex indices
        // Compare the angle between directions v1 to v2 and v1 to v3, to check whether the texture is flipped
        double angle1to2 = Math.atan2(quad.v(1) - quad.v(0), quad.u(1) - quad.u(0)), angle1to3 = Math.atan2(quad.v(2) - quad.v(0), quad.u(2) - quad.u(0));
        boolean textureFlipped = (angle1to2 - angle1to3 + 4 * Math.PI) % (2 * Math.PI) < Math.PI;
        // Find the top-left-most-ish index, if we assume the uvs form a grid-aligned square this should work
        int topLeftMostIndex = 0;
        float minUV = quad.u(0) + quad.v(0);
        for(int i = 1; i < 4; i++){
            if(quad.u(i) + quad.v(i) < minUV){
                minUV = quad.u(i) + quad.v(i);
                topLeftMostIndex = i;
            }
        }
        int textureRotation = textureFlipped ? topLeftMostIndex : (4 - topLeftMostIndex) % 4;

        // Determine the vertex indices rotation relative to the block face
        Vector3f[] positions3d = {quad.position(0), quad.position(1), quad.position(2), quad.position(3)};
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

    static class QuadPredicatesKey {
        public final Direction direction;
        public final TextureOrientation orientation;
        public final ConnectionPredicate predicate;

        QuadPredicatesKey(Direction direction, TextureOrientation orientation, ConnectionPredicate predicate){
            this.direction = direction;
            this.orientation = orientation;
            this.predicate = predicate;
        }

        @Override
        public final boolean equals(Object o){
            if(this == o) return true;
            if(!(o instanceof QuadPredicatesKey that)) return false;

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

    public record Part(ConnectingModelQuads quads) {
    }
}
