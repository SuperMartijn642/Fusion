package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.api.model.custom.quad.EmittableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.api.texture.custom.BlockStateQuadProcessor;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.util.PropertyStore;
import com.supermartijn642.fusion.util.CullingHelper;
import com.supermartijn642.fusion.util.FallbackPropertyStore;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseBlockStateModel implements BlockStateModel {

    private final Quads quads;
    private final ModelPredicate conditions;
    private final TextureAtlasSprite particleSprite;
    private final PropertyStore propertyStore;

    public BaseBlockStateModel(Quads quads, ModelPredicate conditions, TextureAtlasSprite particleSprite, PropertyStore propertyStore){
        this.quads = quads;
        this.conditions = conditions;
        this.particleSprite = particleSprite;
        this.propertyStore = propertyStore;
    }

    @Override
    public void collectParts(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state, RandomSource random, List<BlockModelPart> parts){
        // Check conditions
        if(this.conditions != null && !this.conditions.testForBlockState(level, pos, state))
            return;

        // Get the default render type to use
        //noinspection deprecation
        ChunkSectionLayer defaultRenderType = state == null ?
            ChunkSectionLayer.SOLID :
            ItemBlockRenderTypes.getChunkRenderType(state);

        PropertyStore propertyStore = FallbackPropertyStore.create(this.propertyStore);

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
                extractStates[cullIndex].add(quad.processor().extractState(level, pos, state, () -> random, propertyStore));
            }
        }

        // Get quad processor cache
        LazyQuadProcessor lazyQuadProcessor = new LazyQuadProcessor();
        lazyQuadProcessor.setCalculator(cullDirection -> processQuads(
            this.quads.get(cullDirection),
            extractStates[CullingHelper.cullIndex(cullDirection)],
            propertyStore,
            defaultRenderType
        ));

        // Create a model part for each chunk render type
        for(ChunkSectionLayer renderType : ChunkSectionLayer.values()){
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
                    return BaseBlockStateModel.this.particleSprite;
                }

                @Override
                public ChunkSectionLayer getRenderType(BlockState state){
                    return renderType;
                }
            });
        }
    }

    private static Map<ChunkSectionLayer,List<BakedQuad>> processQuads(List<Quad> quads, List<Object> states, PropertyStore propertyStore, ChunkSectionLayer defaultRenderType){
        // Group quads by render type
        Map<ChunkSectionLayer,List<BakedQuad>> quadsByRenderType = new EnumMap<>(ChunkSectionLayer.class);
        Consumer<QuadAccess> submitter = quad -> {
            ChunkSectionLayer quadRenderType = quad.chunkLayer();
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
        this.collectParts(null, null, null, random, parts);
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random){
        List<Object> identity = new ArrayList<>();
        identity.add(this);
        // Check conditions
        if(this.conditions != null){
            if(!this.conditions.testForBlockState(level, pos, state)){
                identity.add(false);
                return identity;
            }
            identity.add(true);
        }
        // Add keys for all the textures that have additional processing
        PropertyStore propertyStore = FallbackPropertyStore.create(this.propertyStore);
        for(Direction cullDirection : CullingHelper.cullDirections()){
            for(Quad quad : this.quads.get(cullDirection)){
                // Ignore quads that don't need processing
                if(quad.processor() == null)
                    continue;
                // Extract state
                Object s = quad.processor().extractState(level, pos, state, () -> random, propertyStore);
                // Create key
                Object key = quad.processor().createGeometryKey(s, propertyStore);
                if(key == null)
                    return null;
                identity.add(key);
            }
        }
        return identity;
    }

    @Override
    public TextureAtlasSprite particleIcon(){
        return this.particleSprite;
    }

    public record Quads(List<Quad>[] quads) {
        List<Quad> get(Direction cullDirection){
            return this.quads[CullingHelper.cullIndex(cullDirection)];
        }
    }

    public record Quad(QuadAccess quad, SpriteInstance sprite, BlockStateQuadProcessor<Object> processor) {
    }

    private static class LazyQuadProcessor {
        @SuppressWarnings("unchecked")
        private final Map<ChunkSectionLayer,List<BakedQuad>>[] byDirection = new Map[7];
        private Function<Direction,Map<ChunkSectionLayer,List<BakedQuad>>> calculator;

        List<BakedQuad> get(Direction cullDirection, ChunkSectionLayer renderType){
            Map<ChunkSectionLayer,List<BakedQuad>> byRenderType = this.byDirection[CullingHelper.cullIndex(cullDirection)];
            if(byRenderType == null)
                byRenderType = this.byDirection[CullingHelper.cullIndex(cullDirection)] = this.calculator.apply(cullDirection);
            if(renderType == null){
                List<BakedQuad> allQuads = new ArrayList<>();
                byRenderType.values().forEach(allQuads::addAll);
                return allQuads;
            }
            return byRenderType.getOrDefault(renderType, Collections.emptyList());
        }

        void setCalculator(Function<Direction,Map<ChunkSectionLayer,List<BakedQuad>>> calculator){
            this.calculator = calculator;
        }
    }
}
