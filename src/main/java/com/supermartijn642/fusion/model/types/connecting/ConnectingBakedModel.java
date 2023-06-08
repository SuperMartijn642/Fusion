package com.supermartijn642.fusion.model.types.connecting;

import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureLayout;
import com.supermartijn642.fusion.model.WrappedBakedModel;
import com.supermartijn642.fusion.texture.types.connecting.ConnectingTextureSprite;
import com.supermartijn642.fusion.texture.types.connecting.ConnectingTextureType;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.TransformationMatrix;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;
import org.antlr.v4.runtime.misc.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class ConnectingBakedModel extends WrappedBakedModel {

    private static final int BLOCK_VERTEX_DATA_UV_OFFSET = findUVOffset(DefaultVertexFormats.BLOCK);
    private static final ModelProperty<SurroundingBlockData> SURROUNDING_BLOCK_DATA_MODEL_PROPERTY = new ModelProperty<>();

    private final TransformationMatrix modelRotation;
    private final List<ConnectionPredicate> predicates;
    // [cullface][hashcode * 6]
    private final Map<Direction,Map<Integer,List<BakedQuad>>> quadCache = new HashMap<>();
    private final Map<Integer,List<BakedQuad>> directionlessQuadCache = new HashMap<>();

    public ConnectingBakedModel(IBakedModel original, TransformationMatrix modelRotation, List<ConnectionPredicate> predicates){
        super(original);
        this.modelRotation = modelRotation;
        this.predicates = predicates;
        for(Direction direction : Direction.values())
            this.quadCache.put(direction, new HashMap<>());
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull Random random, @NotNull IModelData modelData){
        SurroundingBlockData data = modelData.hasProperty(SURROUNDING_BLOCK_DATA_MODEL_PROPERTY) ? modelData.getData(SURROUNDING_BLOCK_DATA_MODEL_PROPERTY) : null;
        int hashCode = data == null ? 0 : data.hashCode();

        // Compute the quads if they aren't in the cache yet
        Map<Integer,List<BakedQuad>> cache = side == null ? this.directionlessQuadCache : this.quadCache.get(side);
        if(!cache.containsKey(hashCode)){
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized(cache){
                if(!cache.containsKey(hashCode))
                    cache.put(hashCode, this.remapQuads(this.original.getQuads(state, side, random), data));
            }
        }

        return cache.get(hashCode);
    }

    private List<BakedQuad> remapQuads(List<BakedQuad> originalQuads, SurroundingBlockData surroundingBlocks){
        if(surroundingBlocks == null)
            return originalQuads;
        return originalQuads.stream().map(quad -> this.remapQuad(quad, surroundingBlocks)).filter(Objects::nonNull).collect(Collectors.toList());
    }

    protected BakedQuad remapQuad(BakedQuad quad, SurroundingBlockData surroundingBlocks){
        TextureAtlasSprite sprite = quad.getSprite();
        if(SpriteHelper.getTextureType(sprite) != DefaultTextureTypes.CONNECTING)
            return quad;
        ConnectingTextureLayout layout = ((ConnectingTextureSprite)sprite).getLayout();

        int[] vertexData = quad.getVertices();
        // Make sure we don't change the original quad
        vertexData = Arrays.copyOf(vertexData, vertexData.length);

        // Adjust the uv
        SurroundingBlockData.SideConnections connections = surroundingBlocks.getConnections(quad.getDirection());
        int[] uv = ConnectingTextureType.getStatePosition(layout, connections.top, connections.topRight, connections.right, connections.bottomRight, connections.bottom, connections.bottomLeft, connections.left, connections.topLeft);
        adjustVertexDataUV(vertexData, uv[0], uv[1], sprite);

        // Create a new quad
        return new BakedQuad(vertexData, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade());
    }

    private static int[] adjustVertexDataUV(int[] vertexData, int newU, int newV, TextureAtlasSprite sprite){
        int vertexSize = DefaultVertexFormats.BLOCK.getIntegerSize();
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

    public SurroundingBlockData getModelData(IBlockDisplayReader level, BlockPos pos, BlockState state){
        return SurroundingBlockData.create(level, pos, this.modelRotation, this.predicates);
    }

    @Override
    public @Nonnull IModelData getModelData(@Nonnull IBlockDisplayReader level, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nonnull IModelData modelData){
        return new ModelDataMap.Builder().withInitial(SURROUNDING_BLOCK_DATA_MODEL_PROPERTY, this.getModelData(level, pos, state)).build();
    }

    @Override
    public boolean isCustomRenderer(){
        return super.isCustomRenderer();
    }

    @Override
    public ItemCameraTransforms getTransforms(){
        return super.getTransforms();
    }

    @Override
    public ItemOverrideList getOverrides(){
        return ItemOverrideList.EMPTY;
    }
}
