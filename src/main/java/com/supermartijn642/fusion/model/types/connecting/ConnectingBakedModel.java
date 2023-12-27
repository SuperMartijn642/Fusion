package com.supermartijn642.fusion.model.types.connecting;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.math.Transformation;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureData;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureLayout;
import com.supermartijn642.fusion.model.WrappedBakedModel;
import com.supermartijn642.fusion.texture.types.connecting.ConnectingTextureLayoutHelper;
import com.supermartijn642.fusion.texture.types.connecting.ConnectingTextureSprite;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class ConnectingBakedModel extends WrappedBakedModel {

    private static final int BLOCK_VERTEX_DATA_UV_OFFSET = findUVOffset(DefaultVertexFormat.BLOCK);
    public static final ModelProperty<SurroundingBlockData> SURROUNDING_BLOCK_DATA_MODEL_PROPERTY = new ModelProperty<>();

    private final Transformation modelRotation;
    private final Map<ResourceLocation,ConnectionPredicate> predicates;
    // [cullface][hashcode * 6]
    private final Map<RenderKey,List<BakedQuad>> quadCache = new HashMap<>();
    private final RenderKey mutableKey = new RenderKey(0, null, null);
    private List<RenderType> customRenderTypes;

    public ConnectingBakedModel(BakedModel original, Transformation modelRotation, Map<ResourceLocation,ConnectionPredicate> predicates){
        super(original);
        this.modelRotation = modelRotation;
        this.predicates = predicates;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource random, @NotNull ModelData modelData, @Nullable RenderType renderType){
        SurroundingBlockData data = modelData.has(SURROUNDING_BLOCK_DATA_MODEL_PROPERTY) ? modelData.get(SURROUNDING_BLOCK_DATA_MODEL_PROPERTY) : null;
        int hashCode = data == null ? 0 : data.hashCode();

        // Get the correct cache and quads
        List<BakedQuad> quads;
        synchronized(this.quadCache){
            this.mutableKey.update(hashCode, side, renderType);
            quads = this.quadCache.get(this.mutableKey);
        }

        // Compute the quads if they don't exist yet
        if(quads == null){
            boolean isOriginalRenderType = state == null || renderType == null || super.getRenderTypes(state, random, modelData).contains(renderType);
            quads = this.remapQuads(this.original.getQuads(state, side, random, modelData, renderType), data, renderType, isOriginalRenderType);
            synchronized(this.quadCache){
                this.mutableKey.update(hashCode, side, renderType);
                if(!this.quadCache.containsKey(this.mutableKey)){
                    RenderKey key = new RenderKey(hashCode, side, renderType);
                    this.quadCache.put(key, quads);
                }else
                    quads = this.quadCache.get(this.mutableKey);
            }
        }

        // Safety check even though this should never happen
        if(quads == null)
            throw new IllegalStateException("Tried returning null list from ConnectingBakedModel#getQuads for side '" + side + "'!");

        return quads;
    }

    private List<BakedQuad> remapQuads(List<BakedQuad> originalQuads, SurroundingBlockData surroundingBlocks, RenderType renderType, boolean originalRenderType){
        if(surroundingBlocks == null)
            return originalQuads;
        return originalQuads.stream().map(quad -> this.remapQuad(quad, surroundingBlocks, renderType, originalRenderType)).filter(Objects::nonNull).collect(Collectors.toList());
    }

    protected BakedQuad remapQuad(BakedQuad quad, SurroundingBlockData surroundingBlocks, RenderType renderType, boolean originalRenderType){
        TextureAtlasSprite sprite = quad.getSprite();
        if(SpriteHelper.getTextureType(sprite) != DefaultTextureTypes.CONNECTING)
            return originalRenderType ? quad : null;

        ConnectingTextureData.RenderType spriteRenderType = ((ConnectingTextureSprite)sprite).getRenderType();
        if(spriteRenderType == null ? !originalRenderType : FusionClient.getRenderTypeMaterial(spriteRenderType) != renderType)
            return null;

        ConnectingTextureLayout layout = ((ConnectingTextureSprite)sprite).getLayout();

        int[] vertexData = quad.getVertices();
        // Make sure we don't change the original quad
        vertexData = Arrays.copyOf(vertexData, vertexData.length);

        // Adjust the uv
        ResourceLocation spriteIdentifier = sprite.contents() == null || sprite.contents().name() == null ? ConnectingModelType.DEFAULT_CONNECTION_KEY : sprite.contents().name();
        if(!this.predicates.containsKey(spriteIdentifier))
            spriteIdentifier = ConnectingModelType.DEFAULT_CONNECTION_KEY;
        SurroundingBlockData.SideConnections connections = surroundingBlocks.getConnections(spriteIdentifier, quad.getDirection());
        int[] uv = ConnectingTextureLayoutHelper.getStatePosition(layout, connections);
        adjustVertexDataUV(vertexData, uv[0], uv[1], sprite);

        // Create a new quad
        return new BakedQuad(vertexData, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade());
    }

    private static int[] adjustVertexDataUV(int[] vertexData, int newU, int newV, TextureAtlasSprite sprite){
        int vertexSize = DefaultVertexFormat.BLOCK.getIntegerSize();
        int vertices = vertexData.length / vertexSize;
        int uvOffset = BLOCK_VERTEX_DATA_UV_OFFSET / 4;


        for(int i = 0; i < vertices; i++){
            int offset = i * vertexSize + uvOffset;

            float width = sprite.getU1() - sprite.getU0();
            float u = Float.intBitsToFloat(vertexData[offset]) + width * newU;
            vertexData[offset] = Float.floatToRawIntBits(u);

            float height = sprite.getV1() - sprite.getV0();
            float v = Float.intBitsToFloat(vertexData[offset + 1]) + height * newV;
            vertexData[offset + 1] = Float.floatToRawIntBits(v);
        }
        return vertexData;
    }

    private static int findUVOffset(VertexFormat vertexFormat){
        int index;
        VertexFormatElement element = null;
        for(index = 0; index < vertexFormat.getElements().size(); index++){
            VertexFormatElement el = vertexFormat.getElements().get(index);
            if(el.getUsage() == VertexFormatElement.Usage.UV){
                element = el;
                break;
            }
        }
        if(index == vertexFormat.getElements().size() || element == null)
            throw new RuntimeException("Expected vertex format to have a UV attribute");
        if(element.getType() != VertexFormatElement.Type.FLOAT)
            throw new RuntimeException("Expected UV attribute to have data type FLOAT");
        if(element.getByteSize() < 4)
            throw new RuntimeException("Expected UV attribute to have at least 4 dimensions");
        return vertexFormat.offsets.getInt(index);
    }

    public SurroundingBlockData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state){
        return SurroundingBlockData.create(level, pos, this.modelRotation, this.predicates);
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData){
        return ModelData.builder().with(SURROUNDING_BLOCK_DATA_MODEL_PROPERTY, this.getModelData(level, pos, state)).build();
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data){
        if(this.customRenderTypes == null)
            this.calculateCustomRenderTypes();
        return ChunkRenderTypeSet.union(ChunkRenderTypeSet.of(this.customRenderTypes), super.getRenderTypes(state, rand, data));
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous){
        if(this.customRenderTypes == null)
            this.calculateCustomRenderTypes();
        List<RenderType> originalRenderTypes = super.getRenderTypes(stack, fabulous);
        List<RenderType> renderTypes = new ArrayList<>(this.customRenderTypes.size() + originalRenderTypes.size());
        renderTypes.addAll(originalRenderTypes);
        renderTypes.addAll(this.customRenderTypes);
        return renderTypes;
    }

    private void calculateCustomRenderTypes(){
        Set<RenderType> renderTypes = new HashSet<>();
        for(Direction cullFace : new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, null}){
            this.original.getQuads(null, cullFace, RandomSource.create(42)).stream()
                .map(BakedQuad::getSprite)
                .filter(sprite -> SpriteHelper.getTextureType(sprite) == DefaultTextureTypes.CONNECTING)
                .map(sprite -> ((ConnectingTextureSprite)sprite).getRenderType())
                .filter(Objects::nonNull)
                .map(FusionClient::getRenderTypeMaterial)
                .forEach(renderTypes::add);
        }
        this.customRenderTypes = Arrays.asList(renderTypes.toArray(RenderType[]::new));
    }

    @Override
    public boolean isCustomRenderer(){
        return super.isCustomRenderer();
    }

    @Override
    public ItemTransforms getTransforms(){
        return super.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides(){
        return ItemOverrides.EMPTY;
    }

    private static class RenderKey {
        private int surroundingBlockData;
        private Direction face;
        private RenderType renderType;

        private RenderKey(int surroundingBlockData, Direction face, RenderType renderType){
            this.surroundingBlockData = surroundingBlockData;
            this.face = face;
            this.renderType = renderType;
        }

        void update(int surroundingBlockData, Direction face, RenderType renderType){
            this.surroundingBlockData = surroundingBlockData;
            this.face = face;
            this.renderType = renderType;
        }

        @Override
        public boolean equals(Object o){
            if(this == o) return true;
            if(o == null || this.getClass() != o.getClass()) return false;

            RenderKey renderKey = (RenderKey)o;

            if(this.surroundingBlockData != renderKey.surroundingBlockData) return false;
            if(this.face != renderKey.face) return false;
            return Objects.equals(this.renderType, renderKey.renderType);
        }

        @Override
        public int hashCode(){
            int result = this.surroundingBlockData;
            result = 31 * result + (this.face != null ? this.face.hashCode() : 0);
            result = 31 * result + (this.renderType != null ? this.renderType.hashCode() : 0);
            return result;
        }
    }
}
