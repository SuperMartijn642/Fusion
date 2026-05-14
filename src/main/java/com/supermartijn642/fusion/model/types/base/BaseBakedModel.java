package com.supermartijn642.fusion.model.types.base;

import com.google.common.base.Suppliers;
import com.supermartijn642.fusion.api.model.custom.quad.EmittableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.texture.custom.QuadProcessor;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.util.PropertyStore;
import com.supermartijn642.fusion.model.WrappedBakedModel;
import com.supermartijn642.fusion.util.ChunkRenderTypeMap;
import com.supermartijn642.fusion.util.CullingHelper;
import com.supermartijn642.fusion.util.FallbackPropertyStore;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
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
public class BaseBakedModel implements BakedModel {

    private static final ModelProperty<RenderData> RENDER_DATA = new ModelProperty<>();
    private static final ModelProperty<LazyQuadProcessor> QUAD_PROCESSORS = new ModelProperty<>();

    private final List<Part> parts;
    private final TextureAtlasSprite particleSprite;
    private final BlockModel.GuiLight guiLight;
    private final boolean isGui3d;
    private final ItemTransforms transforms;
    private final PropertyStore propertyStore;

    public BaseBakedModel(List<Part> parts, TextureAtlasSprite particleSprite, BlockModel.GuiLight guiLight, boolean isGui3d, ItemTransforms transforms, PropertyStore propertyStore){
        this.parts = parts;
        this.particleSprite = particleSprite;
        this.guiLight = guiLight;
        this.isGui3d = isGui3d;
        this.transforms = transforms;
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

        // Check whether we should use block context
        boolean hasBlockContext = level != null || pos != null || state != null;

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
                    Object s = hasBlockContext ?
                        quad.processor().extractState(level, pos, state, randomSupplier, propertyStore) :
                        quad.processor().extractState(randomSupplier, propertyStore);
                    extractStates[cullIndex].add(s);
                }
            }
            combinedStates[i] = extractStates;
        }
        return new RenderData(combinedStates, propertyStore);
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
        RenderData renderData = modelData.has(RENDER_DATA) ?
            modelData.get(RENDER_DATA) :
            this.getRenderData(null, null, null);
        assert renderData != null;

        // Get whether the giving render type is the default render type
        RenderType defaultRenderType = state == null ?
            RenderType.solid() :
            ItemBlockRenderTypes.getChunkRenderType(state);

        // Get quad processor cache
        LazyQuadProcessor lazyQuadProcessor = modelData.get(QUAD_PROCESSORS);
        if(lazyQuadProcessor == null)
            lazyQuadProcessor = new LazyQuadProcessor();
        lazyQuadProcessor.setCalculator(side -> this.processQuads(
            cullDirection,
            renderData,
            defaultRenderType
        ));

        // Get baked quads
        return lazyQuadProcessor.get(cullDirection, renderType);
    }

    private Map<RenderType,List<BakedQuad>> processQuads(Direction cullDirection, RenderData renderData, RenderType defaultRenderType){
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
        for(int i = 0; i < this.parts.size(); i++){
            Part part = this.parts.get(i);
            // Get texture states for part
            List<Object> states = renderData.statesForPart(i)[CullingHelper.cullIndex(cullDirection)];
            // Process quads
            for(Quad quad : part.quads().get(cullDirection)){
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
                quad.processor().processQuad(mutableQuad, quad.sprite(), state, renderData.propertyStore);
            }
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
    public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous){
        // Create random supplier
        Supplier<RandomSource> randomSupplier = Suppliers.memoize(() -> {
            RandomSource random = RandomSource.createNewThreadLocalInstance();
            random.setSeed(0);
            return random;
        });

        // Get default render type to use for the item
        RenderType defaultRenderType;
        if(stack.getItem() instanceof BlockItem && ItemBlockRenderTypes.getChunkRenderType(((BlockItem)stack.getItem()).getBlock().defaultBlockState()) != RenderType.translucent())
            defaultRenderType = Sheets.cutoutBlockSheet();
        else
            defaultRenderType = Sheets.translucentItemSheet();

        // Handle each part
        PropertyStore propertyStore = FallbackPropertyStore.create(this.propertyStore);
        List<BakedModel> models = new ArrayList<>(this.parts.size());
        for(Part part : this.parts){
            // Collect quads by render type
            List<RenderType> renderTypes = new ArrayList<>(4);
            List<List<BakedQuad>> quadsByRenderType = new ArrayList<>(4);
            Consumer<QuadAccess> submitter = quad -> {
                // Get render type
                RenderType renderType = quad.itemRenderType();
                if(renderType == null)
                    renderType = defaultRenderType;
                // Get or quad list
                int i = renderTypes.indexOf(renderType);
                List<BakedQuad> bakedQuads;
                if(i == -1){
                    renderTypes.add(renderType);
                    bakedQuads = new ArrayList<>();
                    quadsByRenderType.add(bakedQuads);
                }else
                    bakedQuads = quadsByRenderType.get(i);
                // Add the quad to the list
                bakedQuads.add(quad.toBakedQuad());
            };

            // Process all quads
            EmittableQuad mutableQuad = null;
            for(Quad quad : part.quads().all()){
                // Simply add quads that don't need further processing
                if(quad.processor() == null){
                    submitter.accept(quad.quad());
                    continue;
                }

                // Create mutable quad
                if(mutableQuad == null)
                    mutableQuad = EmittableQuad.create(submitter::accept);
                mutableQuad.copyFrom(quad.quad());

                // Process quad
                Object state = quad.processor().extractState(stack, randomSupplier, propertyStore);
                quad.processor().processQuad(mutableQuad, quad.sprite(), state, propertyStore);
            }

            // Create model for each render type
            for(int i = 0; i < renderTypes.size(); i++){
                RenderType renderType = renderTypes.get(i);
                List<BakedQuad> bakedQuads = quadsByRenderType.get(i);
                models.add(new WrappedBakedModel(this) {
                    @Override
                    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, @NotNull RandomSource random, @NotNull ModelData data, @Nullable RenderType renderType){
                        return this.getQuads(state, cullDirection, random);
                    }

                    @Override
                    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, RandomSource random){
                        return bakedQuads;
                    }

                    @Override
                    public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous){
                        // Despite requesting a list of render types here, NeoForge doesn't actually call their #getQuads override with the render type argument.
                        // Hence, there is no way to know what to return for #getQuads as the arguments are always the same.
                        // Thus, we have to create separate models for a single render type each.
                        return List.of(renderType);
                    }
                });
            }
        }
        return models;
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

    public record Part(Quads quads) {
    }

    public record Quads(List<Quad>[] quads, List<Quad> all) {
        public static Quads create(List<Quad>[] quads){
            List<Quad> combined = new ArrayList<>();
            for(List<Quad> l : quads)
                combined.addAll(l);
            return new Quads(quads, List.copyOf(combined));
        }

        List<Quad> get(Direction cullDirection){
            return this.quads[CullingHelper.cullIndex(cullDirection)];
        }
    }

    public record Quad(QuadAccess quad, SpriteInstance sprite, QuadProcessor<Object> processor) {
    }

    private record RenderData(List<Object>[][] combinedTextureStates, PropertyStore propertyStore) {
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
