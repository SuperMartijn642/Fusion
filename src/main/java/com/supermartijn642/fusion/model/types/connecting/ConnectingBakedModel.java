package com.supermartijn642.fusion.model.types.connecting;

import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureData;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureLayout;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.model.WrappedBakedModel;
import com.supermartijn642.fusion.texture.types.connecting.ConnectingTextureLayoutHelper;
import com.supermartijn642.fusion.texture.types.connecting.ConnectingTextureSprite;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.model.TRSRTransformation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class ConnectingBakedModel extends WrappedBakedModel {

    private static final int BLOCK_VERTEX_DATA_UV_OFFSET = findUVOffset(DefaultVertexFormats.BLOCK);
    public static final ThreadLocal<Boolean> ignoreModelRenderTypeCheck = ThreadLocal.withInitial(() -> false);
    public static final ThreadLocal<Pair<IBlockAccess,BlockPos>> levelCapture = new ThreadLocal<>();

    private final TRSRTransformation modelRotation;
    private final Map<ResourceLocation,ConnectionPredicate> predicates;
    // [cullface][hashcode * 6]
    private final Map<RenderKey,List<BakedQuad>> quadCache = new HashMap<>();
    private final RenderKey mutableKey = new RenderKey(0, null, null);
    private List<BlockRenderLayer> customRenderTypes;

    public ConnectingBakedModel(IBakedModel original, TRSRTransformation modelRotation, Map<ResourceLocation,ConnectionPredicate> predicates){
        super(original);
        this.modelRotation = modelRotation;
        this.predicates = predicates;
    }

    @Override
    public @Nonnull List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long random){
        SurroundingBlockData data = levelCapture.get() == null ? null : this.getModelData(levelCapture.get().left(), levelCapture.get().right(), state);
        int hashCode = data == null ? 0 : data.hashCode();

        // Find the current render type
        BlockRenderLayer renderType = MinecraftForgeClient.getRenderLayer();

        // Get the correct cache and quads
        List<BakedQuad> quads;
        synchronized(this.quadCache){
            this.mutableKey.update(hashCode, side, renderType);
            quads = this.quadCache.get(this.mutableKey);
        }

        // Compute the quads if they don't exist yet
        if(quads == null){
            ignoreModelRenderTypeCheck.set(true);
            boolean isOriginalRenderType = state == null || renderType == null || state.getBlock().canRenderInLayer(state, renderType);
            ignoreModelRenderTypeCheck.set(false);
            quads = this.remapQuads(this.original.getQuads(state, side, random), data, renderType, isOriginalRenderType);
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

    private List<BakedQuad> remapQuads(List<BakedQuad> originalQuads, SurroundingBlockData surroundingBlocks, BlockRenderLayer renderType, boolean originalRenderType){
        if(surroundingBlocks == null)
            return originalQuads;
        return originalQuads.stream().map(quad -> this.remapQuad(quad, surroundingBlocks, renderType, originalRenderType)).filter(Objects::nonNull).collect(Collectors.toList());
    }

    protected BakedQuad remapQuad(BakedQuad quad, SurroundingBlockData surroundingBlocks, BlockRenderLayer renderType, boolean originalRenderType){
        TextureAtlasSprite sprite = quad.getSprite();
        if(SpriteHelper.getTextureType(sprite) != DefaultTextureTypes.CONNECTING)
            return originalRenderType ? quad : null;

        ConnectingTextureData.RenderType spriteRenderType = ((ConnectingTextureSprite)sprite).getRenderType();
        if(spriteRenderType == null ? !originalRenderType : FusionClient.getRenderTypeMaterial(spriteRenderType) != renderType)
            return null;

        ConnectingTextureLayout layout = ((ConnectingTextureSprite)sprite).getLayout();

        int[] vertexData = quad.getVertexData();
        // Make sure we don't change the original quad
        vertexData = Arrays.copyOf(vertexData, vertexData.length);

        // Adjust the uv
        ResourceLocation spriteIdentifier = new ResourceLocation(sprite.getIconName());
        if(!this.predicates.containsKey(spriteIdentifier))
            spriteIdentifier = ConnectingModelType.DEFAULT_CONNECTION_KEY;
        SurroundingBlockData.SideConnections connections = surroundingBlocks.getConnections(spriteIdentifier, quad.getFace());
        int[] uv = ConnectingTextureLayoutHelper.getStatePosition(layout, connections);
        if(ConnectingTextureLayoutHelper.shouldBeRotated(layout)){
            int u = uv[0];
            uv[0] = uv[1];
            uv[1] = u;
        }
        adjustVertexDataUV(vertexData, uv[0], uv[1], sprite);

        // Create a new quad
        return new BakedQuad(vertexData, quad.getTintIndex(), quad.getFace(), quad.getSprite(), quad.shouldApplyDiffuseLighting(), quad.getFormat());
    }

    private static int[] adjustVertexDataUV(int[] vertexData, int newU, int newV, TextureAtlasSprite sprite){
        int vertexSize = DefaultVertexFormats.BLOCK.getIntegerSize();
        int vertices = vertexData.length / vertexSize;
        int uvOffset = BLOCK_VERTEX_DATA_UV_OFFSET / 4;

        for(int i = 0; i < vertices; i++){
            int offset = i * vertexSize + uvOffset;

            float width = sprite.getMaxU() - sprite.getMinU();
            float u = Float.intBitsToFloat(vertexData[offset]) + width * newU;
            vertexData[offset] = Float.floatToRawIntBits(u);

            float height = sprite.getMaxV() - sprite.getMinV();
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
            if(el.getUsage() == VertexFormatElement.EnumUsage.UV){
                element = el;
                break;
            }
        }
        if(index == vertexFormat.getElements().size() || element == null)
            throw new RuntimeException("Expected vertex format to have a UV attribute");
        if(element.getType() != VertexFormatElement.EnumType.FLOAT)
            throw new RuntimeException("Expected UV attribute to have data type FLOAT");
        if(element.getSize() < 4)
            throw new RuntimeException("Expected UV attribute to have at least 4 dimensions");
        return vertexFormat.offsets.get(index);
    }

    public SurroundingBlockData getModelData(IBlockAccess level, BlockPos pos, IBlockState state){
        return SurroundingBlockData.create(level, pos, this.modelRotation, this.predicates);
    }

    public List<BlockRenderLayer> getCustomRenderTypes(){
        if(this.customRenderTypes == null)
            this.calculateCustomRenderTypes();
        return this.customRenderTypes;
    }

    private void calculateCustomRenderTypes(){
        Set<BlockRenderLayer> renderTypes = new HashSet<>();
        for(EnumFacing cullFace : new EnumFacing[]{EnumFacing.UP, EnumFacing.DOWN, EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST, null}){
            this.original.getQuads(null, cullFace, 42).stream()
                .map(BakedQuad::getSprite)
                .filter(sprite -> SpriteHelper.getTextureType(sprite) == DefaultTextureTypes.CONNECTING)
                .map(sprite -> ((ConnectingTextureSprite)sprite).getRenderType())
                .filter(Objects::nonNull)
                .map(FusionClient::getRenderTypeMaterial)
                .forEach(renderTypes::add);
        }
        this.customRenderTypes = Arrays.asList(renderTypes.toArray(new BlockRenderLayer[0]));
    }

    @Override
    public ItemOverrideList getOverrides(){
        return ItemOverrideList.NONE;
    }

    private static class RenderKey {
        private int surroundingBlockData;
        private EnumFacing face;
        private BlockRenderLayer renderType;

        private RenderKey(int surroundingBlockData, EnumFacing face, BlockRenderLayer renderType){
            this.surroundingBlockData = surroundingBlockData;
            this.face = face;
            this.renderType = renderType;
        }

        void update(int surroundingBlockData, EnumFacing face, BlockRenderLayer renderType){
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
