package com.supermartijn642.fusion.model.types.base;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.model.custom.quad.EmittableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.api.texture.custom.QuadProcessor;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.util.PropertyStore;
import com.supermartijn642.fusion.model.BlockRenderContext;
import com.supermartijn642.fusion.model.CustomRenderTypeBakedModel;
import com.supermartijn642.fusion.util.CullingHelper;
import com.supermartijn642.fusion.util.FallbackPropertyStore;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.MinecraftForgeClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseBakedModel implements IBakedModel, CustomRenderTypeBakedModel {

    private final List<Part> parts;
    private final TextureAtlasSprite particleSprite;
    private final boolean ambientOcclusion;
    private final boolean isGui3d;
    private final ItemCameraTransforms transforms;
    private final PropertyStore propertyStore;

    public BaseBakedModel(List<Part> parts, TextureAtlasSprite particleSprite, boolean ambientOcclusion, boolean isGui3d, ItemCameraTransforms transforms, PropertyStore propertyStore){
        this.parts = parts;
        this.particleSprite = particleSprite;
        this.ambientOcclusion = ambientOcclusion;
        this.isGui3d = isGui3d;
        this.transforms = transforms;
        this.propertyStore = propertyStore;
    }

    private RenderData getRenderData(@Nullable IBlockAccess level, @Nullable BlockPos pos, @Nullable IBlockState state){
        // Create random supplier
        Supplier<Random> randomSupplier = Suppliers.memoize(() -> {
            long seed = MathHelper.getPositionRandom(pos == null ? BlockPos.ORIGIN : pos);
            return new Random(seed);
        });

        // Check whether we should use block context
        boolean hasBlockContext = level != null || pos != null || state != null;

        // For each part, collect whether the part's conditions are met and collect texture states for the part
        boolean[] partConditions = new boolean[this.parts.size()];

        // Collect texture states
        //noinspection unchecked
        List<Object>[][] combinedStates = new List[this.parts.size()][];
        PropertyStore propertyStore = FallbackPropertyStore.create(this.propertyStore);
        for(int i = 0; i < this.parts.size(); i++){
            Part part = this.parts.get(i);
            // Check part condition
            if(part.conditions != null && !part.conditions.testForBlock(level, pos, state))
                continue;
            partConditions[i] = true;

            // Extract state for all the textures that need processing
            //noinspection unchecked
            List<Object>[] extractStates = new List[7];
            for(EnumFacing cullDirection : CullingHelper.cullDirections()){
                int cullIndex = CullingHelper.cullIndex(cullDirection);
                for(Quad quad : part.quads.get(cullDirection)){
                    // Ignore quads that don't need processing
                    if(quad.processor == null)
                        continue;
                    if(extractStates[cullIndex] == null)
                        extractStates[cullIndex] = new ArrayList<>();
                    Object s = hasBlockContext ?
                        quad.processor.extractState(level, pos, state, randomSupplier, propertyStore) :
                        quad.processor.extractState(randomSupplier, propertyStore);
                    extractStates[cullIndex].add(s);
                }
            }
            combinedStates[i] = extractStates;
        }
        return new RenderData(partConditions, combinedStates, propertyStore);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing cullDirection, long seed){
        // Check whether we are rendering an item
        ItemStack stack = FusionClient.ITEM_STACK_RENDER_CONTEXT.get();
        if(stack != null){
            if(cullDirection != null)
                return Collections.emptyList();
            return this.getItemQuads(stack);
        }

        // Get block render data
        BlockRenderContext blockRenderContext = FusionClient.BLOCK_RENDER_CONTEXT.get();
        RenderData renderData = blockRenderContext == null ?
            this.getRenderData(null, null, null) :
            this.getRenderData(blockRenderContext.level(), blockRenderContext.pos(), state);

        // Get whether the giving render type is the default render type
        BlockRenderLayer defaultRenderType = state == null ?
            BlockRenderLayer.SOLID :
            state.getBlock().getBlockLayer();

        // Get quad processor cache
        LazyQuadProcessor lazyQuadProcessor = new LazyQuadProcessor();
        lazyQuadProcessor.setCalculator(side -> this.processQuads(
            cullDirection,
            renderData,
            defaultRenderType
        ));

        // Get baked quads
        BlockRenderLayer renderType = MinecraftForgeClient.getRenderLayer();
        return lazyQuadProcessor.get(cullDirection, renderType);
    }

    private Map<BlockRenderLayer,List<BakedQuad>> processQuads(EnumFacing cullDirection, RenderData renderData, BlockRenderLayer defaultRenderType){
        // Group quads by render type
        Map<BlockRenderLayer,List<BakedQuad>> quadsByRenderType = new EnumMap<>(BlockRenderLayer.class);
        Consumer<QuadAccess> submitter = quad -> {
            BlockRenderLayer quadRenderType = quad.renderLayer();
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
            // Check part condition
            if(!renderData.partConditions[i])
                continue;

            // Get texture states for part
            List<Object> states = renderData.statesForPart(i)[CullingHelper.cullIndex(cullDirection)];
            // Process quads
            for(Quad quad : part.quads.get(cullDirection)){
                // Simply add quads that don't need further processing
                if(quad.processor == null){
                    submitter.accept(quad.quad);
                    continue;
                }

                // Create mutable quad
                if(mutableQuad == null)
                    mutableQuad = EmittableQuad.create(submitter::accept);
                mutableQuad.copyFrom(quad.quad);

                // Process special texture type quads
                Object state = states.get(stateIndex++);
                quad.processor.processQuad(mutableQuad, quad.sprite, state, renderData.propertyStore);
            }
        }
        return quadsByRenderType;
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer){
        return true;
    }

    private List<BakedQuad> getItemQuads(ItemStack stack){
        // Create random supplier
        Supplier<Random> randomSupplier = Suppliers.memoize(() -> new Random(0));

        // Handle each part
        PropertyStore propertyStore = FallbackPropertyStore.create(this.propertyStore);
        List<BakedQuad> bakedQuads = new ArrayList<>();
        for(Part part : this.parts){
            // Check part condition
            if(part.conditions != null && !part.conditions.testForItem(stack))
                continue;

            // Process all quads
            EmittableQuad mutableQuad = null;
            for(Quad quad : part.quads.all){
                // Simply add quads that don't need further processing
                if(quad.processor == null){
                    bakedQuads.add(quad.quad.toBakedQuad());
                    continue;
                }

                // Create mutable quad
                if(mutableQuad == null)
                    mutableQuad = EmittableQuad.create(q -> bakedQuads.add(q.toBakedQuad()));
                mutableQuad.copyFrom(quad.quad);

                // Process quad
                Object state = quad.processor.extractState(stack, randomSupplier, propertyStore);
                quad.processor.processQuad(mutableQuad, quad.sprite, state, propertyStore);
            }
        }
        return bakedQuads;
    }

    @Override
    public TextureAtlasSprite getParticleTexture(){
        return this.particleSprite;
    }

    @Override
    public boolean isAmbientOcclusion(){
        return this.ambientOcclusion;
    }

    @Override
    public boolean isGui3d(){
        return this.isGui3d;
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms(){
        return this.transforms;
    }

    @Override
    public boolean isBuiltInRenderer(){
        return false;
    }

    @Override
    public ItemOverrideList getOverrides(){
        return ItemOverrideList.NONE;
    }

    public static final class Part {
        private final Quads quads;
        private final ModelPredicate conditions;

        public Part(Quads quads, ModelPredicate conditions){
            this.quads = quads;
            this.conditions = conditions;
        }
    }

    public static final class Quads {
        public static Quads create(List<Quad>[] quads){
            List<Quad> combined = new ArrayList<>();
            for(List<Quad> l : quads)
                combined.addAll(l);
            return new Quads(quads, ImmutableList.copyOf(combined));
        }

        public Quads(List<Quad>[] quads, List<Quad> all){
            this.quads = quads;
            this.all = all;
        }

        private final List<Quad>[] quads;
        private final List<Quad> all;

        List<Quad> get(EnumFacing cullDirection){
            return this.quads[CullingHelper.cullIndex(cullDirection)];
        }
    }

    public static final class Quad {
        private final QuadAccess quad;
        private final SpriteInstance sprite;
        private final QuadProcessor<Object> processor;

        public Quad(QuadAccess quad, SpriteInstance sprite, QuadProcessor<Object> processor){
            this.quad = quad;
            this.sprite = sprite;
            this.processor = processor;
        }
    }

    private static final class RenderData {
        private final boolean[] partConditions;
        private final List<Object>[][] combinedTextureStates;
        private final PropertyStore propertyStore;

        private RenderData(boolean[] partConditions, List<Object>[][] combinedTextureStates, PropertyStore propertyStore){
            this.partConditions = partConditions;
            this.combinedTextureStates = combinedTextureStates;
            this.propertyStore = propertyStore;
        }

        List<Object>[] statesForPart(int index){
            return this.combinedTextureStates[index];
        }
    }

    private static class LazyQuadProcessor {
        @SuppressWarnings("unchecked")
        private final Map<BlockRenderLayer,List<BakedQuad>>[] byDirection = new Map[7];
        private Function<EnumFacing,Map<BlockRenderLayer,List<BakedQuad>>> calculator;

        List<BakedQuad> get(EnumFacing cullDirection, BlockRenderLayer renderType){
            Map<BlockRenderLayer,List<BakedQuad>> byRenderType = this.byDirection[CullingHelper.cullIndex(cullDirection)];
            if(byRenderType == null)
                byRenderType = this.byDirection[CullingHelper.cullIndex(cullDirection)] = this.calculator.apply(cullDirection);
            if(renderType == null){
                List<BakedQuad> allQuads = new ArrayList<>();
                byRenderType.values().forEach(allQuads::addAll);
                return allQuads;
            }
            return byRenderType.getOrDefault(renderType, Collections.emptyList());
        }

        void setCalculator(Function<EnumFacing,Map<BlockRenderLayer,List<BakedQuad>>> calculator){
            this.calculator = calculator;
        }
    }
}
