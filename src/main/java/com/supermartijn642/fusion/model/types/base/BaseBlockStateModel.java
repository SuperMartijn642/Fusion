package com.supermartijn642.fusion.model.types.base;

import com.google.common.base.Suppliers;
import com.supermartijn642.fusion.api.model.custom.quad.EmittableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.api.texture.custom.BlockStateQuadProcessor;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.util.PropertyStore;
import com.supermartijn642.fusion.util.ChunkRenderTypeMap;
import com.supermartijn642.fusion.util.CullingHelper;
import com.supermartijn642.fusion.util.FallbackPropertyStore;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.UnbakedModel;
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
public class BaseBlockStateModel implements BakedModel {

    private static final ModelProperty<RenderData> RENDER_DATA = new ModelProperty<>();
    private static final ModelProperty<LazyQuadProcessor> QUAD_PROCESSORS = new ModelProperty<>();

    private final Quads quads;
    private final ModelPredicate conditions;
    private final PropertyStore propertyStore;
    private final UnbakedModel.GuiLight guiLight;
    private final TextureAtlasSprite particleSprite;
    private final ItemTransforms itemTransforms;

    public BaseBlockStateModel(Quads quads, ModelPredicate conditions, PropertyStore propertyStore, UnbakedModel.GuiLight guiLight, TextureAtlasSprite particleSprite, ItemTransforms itemTransforms){
        this.quads = quads;
        this.conditions = conditions;
        this.propertyStore = propertyStore;
        this.guiLight = guiLight;
        this.particleSprite = particleSprite;
        this.itemTransforms = itemTransforms;
    }

    private RenderData getRenderData(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state){
        // Check conditions
        if(this.conditions != null && !this.conditions.testForBlockState(level, pos, state))
            return RenderData.FAILED_CONDITIONS;

        PropertyStore propertyStore = FallbackPropertyStore.create(this.propertyStore);

        // Create random supplier
        Supplier<RandomSource> randomSupplier = Suppliers.memoize(() -> {
            long seed = state == null ?
                pos == null ? 0 : pos.asLong() :
                pos == null ? state.getSeed(BlockPos.ZERO) : state.getSeed(pos);
            RandomSource random = RandomSource.createNewThreadLocalInstance();
            random.setSeed(seed);
            return random;
        });

        // Extract state for all the textures that need processing
        //noinspection unchecked
        List<Object>[] extractStates = new List[7];
        for(Direction cullDirection : CullingHelper.cullDirections()){
            int cullIndex = CullingHelper.cullIndex(cullDirection);
            for(Quad quad : this.quads.get(cullDirection)){
                // Ignore quads that don't need processing
                if(quad.processor() == null)
                    continue;
                if(extractStates[cullIndex] == null)
                    extractStates[cullIndex] = new ArrayList<>();
                extractStates[cullIndex].add(quad.processor().extractState(level, pos, state, randomSupplier, propertyStore));
            }
        }
        return new RenderData(true, extractStates, propertyStore);
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData){
        return ModelData.builder()
            .with(RENDER_DATA, this.getRenderData(level, pos, state))
            .with(QUAD_PROCESSORS, new LazyQuadProcessor())
            .build();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, RandomSource random, ModelData modelData, @Nullable RenderType renderType){
        // Read render data
        RenderData renderData = modelData.get(RENDER_DATA);
        if(renderData == null)
            renderData = this.getRenderData(null, null, null);

        // Check conditions
        if(!renderData.conditions)
            return List.of();

        PropertyStore propertyStore = renderData.propertyStore;

        // Get whether the giving render type is the default render type
        RenderType defaultRenderType = state == null ?
            RenderType.solid() :
            ItemBlockRenderTypes.getChunkRenderType(state);

        // Get texture states
        List<Object>[] extractStates = renderData.combinedTextureStates;

        // Get quad processor cache
        LazyQuadProcessor lazyQuadProcessor = modelData.get(QUAD_PROCESSORS) == null ?
            new LazyQuadProcessor() :
            modelData.get(QUAD_PROCESSORS);
        assert lazyQuadProcessor != null;
        lazyQuadProcessor.setCalculator(c -> processQuads(
            this.quads.get(c),
            extractStates[CullingHelper.cullIndex(c)],
            propertyStore,
            defaultRenderType
        ));

        // Get baked quads
        return lazyQuadProcessor.get(cullDirection, renderType);
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
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, RandomSource random){
        return this.getQuads(state, cullDirection, random, ModelData.EMPTY, null);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data){
        return ChunkRenderTypeSet.all();
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
        return true;
    }

    @Override
    public boolean usesBlockLight(){
        return this.guiLight.lightLikeBlock();
    }

    @Override
    public ItemTransforms getTransforms(){
        return this.itemTransforms;
    }

    public record Quads(List<Quad>[] quads) {
        List<Quad> get(Direction cullDirection){
            return this.quads[CullingHelper.cullIndex(cullDirection)];
        }
    }

    public record Quad(QuadAccess quad, SpriteInstance sprite, BlockStateQuadProcessor<Object> processor) {
    }

    private record RenderData(boolean conditions, List<Object>[] combinedTextureStates, PropertyStore propertyStore) {
        static final RenderData FAILED_CONDITIONS = new RenderData(false, null, null);
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
