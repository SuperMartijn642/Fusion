package com.supermartijn642.fusion.model.types.base;

import com.google.common.base.Suppliers;
import com.supermartijn642.fusion.api.model.custom.quad.EmittableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.texture.custom.BlockStateQuadProcessor;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.util.PropertyStore;
import com.supermartijn642.fusion.util.ChunkRenderTypeMap;
import com.supermartijn642.fusion.util.CullingHelper;
import com.supermartijn642.fusion.util.FallbackPropertyStore;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseBlockStateModel implements BlockStateModel {

    private static final ModelProperty<RenderData> RENDER_DATA = new ModelProperty<>();
    private static final ModelProperty<LazyQuadProcessor[]> QUAD_PROCESSORS = new ModelProperty<>();

    private final List<Part> parts;
    private final TextureAtlasSprite particleSprite;
    private final PropertyStore propertyStore;

    public BaseBlockStateModel(List<Part> parts, TextureAtlasSprite particleSprite, PropertyStore propertyStore){
        this.parts = parts;
        this.particleSprite = particleSprite;
        this.propertyStore = propertyStore;
    }

    private RenderData getRenderData(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state){
        // Create random supplier
        Supplier<RandomSource> randomSupplier = Suppliers.memoize(() -> {
            long seed = state == null ?
                pos == null ? 0 : pos.asLong() :
                pos == null ? state.getSeed(BlockPos.ZERO) : state.getSeed(pos);
            RandomSource random = RandomSource.createNewThreadLocalInstance();
            random.setSeed(seed);
            return random;
        });

        // Collect texture states
        //noinspection unchecked
        List<Object>[][] combinedStates = new List[this.parts.size()][];
        PropertyStore propertyStore = FallbackPropertyStore.create(this.propertyStore);
        for(int i = 0; i < this.parts.size(); i++){
            Part part = this.parts.get(i);
            // Extract state for all the textures that need processing
            //noinspection unchecked
            List<Object>[] extractStates = new List[7];
            for(Direction cullDirection : CullingHelper.cullDirections()){
                int cullIndex = CullingHelper.cullIndex(cullDirection);
                for(Quad quad : part.quads().get(cullDirection)){
                    // Ignore quads that don't need processing
                    if(quad.processor() == null)
                        continue;
                    if(extractStates[cullIndex] == null)
                        extractStates[cullIndex] = new ArrayList<>();
                    extractStates[cullIndex].add(quad.processor().extractState(level, pos, state, randomSupplier, propertyStore));
                }
            }
            combinedStates[i] = extractStates;
        }
        return new RenderData(state, combinedStates, propertyStore);
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData){
        return ModelData.builder()
            .with(RENDER_DATA, this.getRenderData(level, pos, state))
            .with(QUAD_PROCESSORS, new LazyQuadProcessor[this.parts.size()])
            .build();
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> parts, ModelData modelData, @Nullable RenderType renderType){
        // Read render data
        RenderData renderData = modelData.get(RENDER_DATA);
        if(renderData == null)
            renderData = this.getRenderData(null, null, null);
        PropertyStore propertyStore = renderData.propertyStore;

        // Get quad processor caches
        LazyQuadProcessor[] lazyQuadProcessors = modelData.get(QUAD_PROCESSORS);

        // Get whether the giving render type is the default render type
        RenderType defaultRenderType = renderData.state == null ?
            RenderType.solid() :
            ItemBlockRenderTypes.getChunkRenderType(renderData.state);

        // Handle each part
        for(int i = 0; i < this.parts.size(); i++){
            Part part = this.parts.get(i);
            // Get texture states
            List<Object>[] extractStates = renderData.statesForPart(i);

            // Get quad processor cache
            LazyQuadProcessor lazyQuadProcessor = lazyQuadProcessors == null ? new LazyQuadProcessor()
                : lazyQuadProcessors[i] == null ? lazyQuadProcessors[i] = new LazyQuadProcessor()
                : lazyQuadProcessors[i];
            lazyQuadProcessor.setCalculator(cullDirection -> processQuads(
                part.quads().get(cullDirection),
                extractStates[CullingHelper.cullIndex(cullDirection)],
                propertyStore,
                defaultRenderType
            ));

            // Create model parts
            parts.add(new BlockModelPart() {
                @Override
                public List<BakedQuad> getQuads(@Nullable Direction cullDirection){
                    return lazyQuadProcessor.get(cullDirection, renderType);
                }

                @Override
                public boolean useAmbientOcclusion(){
                    return true;
                }

                @Override
                public TextureAtlasSprite particleIcon(){
                    return part.particleSprite();
                }
            });
        }
    }

    private static Map<RenderType,List<BakedQuad>> processQuads(List<Quad> quads, List<Object> states, PropertyStore propertyStore, RenderType defaultRenderType){
        // Group quads by render type
        Map<RenderType,List<BakedQuad>> quadsByRenderType = new ChunkRenderTypeMap<>();
        Consumer<QuadAccess> submitter = quad -> {
            RenderType quadRenderType = quad.chunkRenderType();
            if(quadRenderType == null)
                quadRenderType = defaultRenderType;
            quadsByRenderType.computeIfAbsent(quadRenderType, r -> new ArrayList<>(8))
                .add(quad.toBakedQuad());
        };

        // Convert all quads to baked quads
        int stateIndex = 0;
        EmittableQuad mutableQuad = null;
        for(Quad quad : quads){
            // Simply add quads that don't need further processing
            if(quad.processor() == null){
                submitter.accept(quad.quad());
                continue;
            }

            // Create mutable quad
            if(mutableQuad == null)
                mutableQuad = EmittableQuad.create(submitter::accept);
            mutableQuad.copyFrom(quad.quad());

            // Process special texture type quads
            Object state = states.get(stateIndex++);
            quad.processor().processQuad(mutableQuad, quad.sprite(), state, propertyStore);
        }
        return quadsByRenderType;
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> parts){
        this.collectParts(random, parts, ModelData.EMPTY, null);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data){
        return ChunkRenderTypeSet.all();
    }

    @Override
    public TextureAtlasSprite particleIcon(){
        return this.particleSprite;
    }

    public record Part(Quads quads, TextureAtlasSprite particleSprite) {
    }

    public record Quads(List<Quad>[] quads) {
        List<Quad> get(Direction cullDirection){
            return this.quads[CullingHelper.cullIndex(cullDirection)];
        }
    }

    public record Quad(QuadAccess quad, SpriteInstance sprite, BlockStateQuadProcessor<Object> processor) {
    }

    private record RenderData(BlockState state, List<Object>[][] combinedTextureStates, PropertyStore propertyStore) {
        List<Object>[] statesForPart(int index){
            return this.combinedTextureStates[index];
        }
    }

    private static class LazyQuadProcessor {
        @SuppressWarnings("unchecked")
        private final Map<RenderType,List<BakedQuad>>[] byDirection = new Map[7];
        private Function<Direction,Map<RenderType,List<BakedQuad>>> calculator;

        List<BakedQuad> get(Direction cullDirection, RenderType renderType){
            Map<RenderType,List<BakedQuad>> byRenderType = this.byDirection[CullingHelper.cullIndex(cullDirection)];
            if(byRenderType == null)
                byRenderType = this.byDirection[CullingHelper.cullIndex(cullDirection)] = this.calculator.apply(cullDirection);
            if(renderType == null){
                List<BakedQuad> allQuads = new ArrayList<>();
                byRenderType.values().forEach(allQuads::addAll);
                return allQuads;
            }
            return byRenderType.getOrDefault(renderType, Collections.emptyList());
        }

        void setCalculator(Function<Direction,Map<RenderType,List<BakedQuad>>> calculator){
            this.calculator = calculator;
        }
    }
}
