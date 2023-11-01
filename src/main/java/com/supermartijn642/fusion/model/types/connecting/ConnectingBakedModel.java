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
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.model.WrappedBakedModel;
import com.supermartijn642.fusion.texture.types.connecting.ConnectingTextureLayoutHelper;
import com.supermartijn642.fusion.texture.types.connecting.ConnectingTextureSprite;
import com.supermartijn642.fusion.util.TextureAtlases;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
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
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class ConnectingBakedModel extends WrappedBakedModel {

    private static final int BLOCK_VERTEX_DATA_UV_OFFSET = findUVOffset(DefaultVertexFormat.BLOCK);

    private final Transformation modelRotation;
    private final Map<ResourceLocation,ConnectionPredicate> predicates;
    // [cullface][hashcode * 6]
    private final Map<Direction,Map<Integer,List<BakedQuad>>> quadCache = new HashMap<>();
    private final Map<Integer,List<BakedQuad>> directionlessQuadCache = new HashMap<>();
    private final ThreadLocal<Pair<BlockAndTintGetter,BlockPos>> levelCapture = new ThreadLocal<>();

    public ConnectingBakedModel(BakedModel original, Transformation modelRotation, Map<ResourceLocation,ConnectionPredicate> predicates){
        super(original);
        this.modelRotation = modelRotation;
        this.predicates = predicates;
        for(Direction direction : Direction.values())
            this.quadCache.put(direction, new HashMap<>());
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context){
        this.levelCapture.set(Pair.of(blockView, pos));
        context.pushTransform(quad -> {
            TextureAtlasSprite sprite = SpriteFinder.get(Minecraft.getInstance().getModelManager().getAtlas(TextureAtlases.getBlocks())).find(quad);
            if(SpriteHelper.getTextureType(sprite) != DefaultTextureTypes.CONNECTING)
                return true;

            // Mark the render type for the quad
            ConnectingTextureData.RenderType renderType = ((ConnectingTextureSprite)sprite).getRenderType();
            if(renderType != null)
                quad.material(FusionClient.getRenderTypeMaterial(renderType));

            return true;
        });
        context.bakedModelConsumer().accept(this, state);
        context.popTransform();
        this.levelCapture.set(null);
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context){
        context.bakedModelConsumer().accept(this);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random){
        SurroundingBlockData data = this.levelCapture.get() == null ? null : this.getModelData(this.levelCapture.get().left(), this.levelCapture.get().right(), state);
        int hashCode = data == null ? 0 : data.hashCode();

        // Get the correct cache and quads
        Map<Integer,List<BakedQuad>> cache = side == null ? this.directionlessQuadCache : this.quadCache.get(side);
        List<BakedQuad> quads;
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized(cache){
            quads = cache.get(hashCode);
        }

        // Compute the quads if they don't exist yet
        if(quads == null){
            quads = this.remapQuads(this.original.getQuads(state, side, random), data);
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized(cache){
                if(!cache.containsKey(hashCode))
                    cache.put(hashCode, quads);
                else
                    quads = cache.get(hashCode);
            }
        }

        // Safety check even though this should never happen
        if(quads == null)
            throw new IllegalStateException("Tried returning null list from ConnectingBakedModel#getQuads for side '" + side + "'!");

        return quads;
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
    public boolean isVanillaAdapter(){
        return false;
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
}
