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
import com.supermartijn642.fusion.model.ModelRenderTypeHelper;
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

    private final Quads quads;
    private final ModelPredicate conditions;
    private final PropertyStore propertyStore;
    private final TextureAtlasSprite particleSprite;
    private final boolean ambientOcclusion;
    private final boolean isGui3d;
    private final ItemCameraTransforms transforms;
    private final ItemOverrideList itemOverrides;

    public BaseBakedModel(Quads quads, ModelPredicate conditions, PropertyStore propertyStore, TextureAtlasSprite particleSprite, boolean ambientOcclusion, boolean isGui3d, ItemCameraTransforms transforms, ItemOverrideList itemOverrides){
        this.quads = quads;
        this.conditions = conditions;
        this.propertyStore = propertyStore;
        this.particleSprite = particleSprite;
        this.ambientOcclusion = ambientOcclusion;
        this.isGui3d = isGui3d;
        this.transforms = transforms;
        this.itemOverrides = itemOverrides;
    }

    private RenderData getRenderData(@Nullable IBlockAccess level, @Nullable BlockPos pos, @Nullable IBlockState state){
        // Check conditions
        if(this.conditions != null && !this.conditions.testForBlockState(level, pos, state))
            return RenderData.FAILED_CONDITIONS;

        PropertyStore propertyStore = FallbackPropertyStore.create(this.propertyStore);

        // Create random supplier
        Supplier<Random> randomSupplier = Suppliers.memoize(() -> {
            long seed = MathHelper.getPositionRandom(pos == null ? BlockPos.ORIGIN : pos);
            return new Random(seed);
        });

        // Check whether we should use block context
        boolean hasBlockContext = level != null || pos != null || state != null;

        // Extract state for all the textures that need processing
        //noinspection unchecked
        List<Object>[] extractStates = new List[7];
        for(EnumFacing cullDirection : CullingHelper.cullDirections()){
            int cullIndex = CullingHelper.cullIndex(cullDirection);
            for(Quad quad : this.quads.get(cullDirection)){
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
        return new RenderData(true, extractStates, propertyStore);
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

        // Check conditions
        if(!renderData.conditions)
            return Collections.emptyList();

        PropertyStore propertyStore = renderData.propertyStore;

        // Get whether the giving render type is the default render type
        BlockRenderLayer defaultRenderType;
        if(state != null){
            if(ModelRenderTypeHelper.couldBlockRenderInLayerOriginally(state, BlockRenderLayer.TRANSLUCENT))
                defaultRenderType = BlockRenderLayer.TRANSLUCENT;
            else if(ModelRenderTypeHelper.couldBlockRenderInLayerOriginally(state, BlockRenderLayer.CUTOUT))
                defaultRenderType = BlockRenderLayer.CUTOUT;
            else if(ModelRenderTypeHelper.couldBlockRenderInLayerOriginally(state, BlockRenderLayer.CUTOUT_MIPPED))
                defaultRenderType = BlockRenderLayer.CUTOUT_MIPPED;
            else
                defaultRenderType = BlockRenderLayer.SOLID;
        }else
            defaultRenderType = BlockRenderLayer.SOLID;

        // Get texture states
        List<Object>[] extractStates = renderData.combinedTextureStates;

        // Get quad processor cache
        LazyQuadProcessor lazyQuadProcessor = new LazyQuadProcessor();
        lazyQuadProcessor.setCalculator(c -> processQuads(
            this.quads.get(c),
            extractStates[CullingHelper.cullIndex(c)],
            propertyStore,
            defaultRenderType
        ));

        // Get baked quads
        BlockRenderLayer renderType = MinecraftForgeClient.getRenderLayer();
        return lazyQuadProcessor.get(cullDirection, renderType);
    }

    private static Map<BlockRenderLayer,List<BakedQuad>> processQuads(List<Quad> quads, List<Object> states, PropertyStore propertyStore, BlockRenderLayer defaultRenderType){
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
        for(Quad quad : quads){
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
            quad.processor.processQuad(mutableQuad, quad.sprite, state, propertyStore);
        }
        return quadsByRenderType;
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer){
        return true;
    }

    private List<BakedQuad> getItemQuads(ItemStack stack){
        // Check conditions
        if(this.conditions != null && !this.conditions.testForItem(stack))
            return Collections.emptyList();

        // Create random supplier
        Supplier<Random> randomSupplier = Suppliers.memoize(() -> new Random(0));

        PropertyStore propertyStore = FallbackPropertyStore.create(this.propertyStore);

        // Process all quads
        List<BakedQuad> bakedQuads = new ArrayList<>(this.quads.all.size());
        EmittableQuad mutableQuad = null;
        for(Quad quad : this.quads.all){
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
        return this.itemOverrides;
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
        static final RenderData FAILED_CONDITIONS = new RenderData(false, null, null);

        private final boolean conditions;
        private final List<Object>[] combinedTextureStates;
        private final PropertyStore propertyStore;

        private RenderData(boolean conditions, List<Object>[] combinedTextureStates, PropertyStore propertyStore){
            this.conditions = conditions;
            this.combinedTextureStates = combinedTextureStates;
            this.propertyStore = propertyStore;
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
