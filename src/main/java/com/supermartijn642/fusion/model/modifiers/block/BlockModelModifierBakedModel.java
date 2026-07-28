package com.supermartijn642.fusion.model.modifiers.block;

import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.integration.framedblocks.FusionFramedBlocksIntegration;
import com.supermartijn642.fusion.model.WrappedBakedModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
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
import java.util.List;

/**
 * Created 19/09/2024 by SuperMartijn642
 */
public class BlockModelModifierBakedModel extends WrappedBakedModel {

    private static final ModelProperty<RenderData> RENDER_DATA = new ModelProperty<>();

    private final BakedModel original;
    private final List<ConditionalModel> defaultModelOverrides;
    private final List<List<ConditionalModel>> appendModels;
    private final TextureAtlasSprite particleSprite;
    private final boolean ambientOcclusion;

    BlockModelModifierBakedModel(BakedModel original, List<ConditionalModel> defaultModelOverrides, List<List<ConditionalModel>> appendModels){
        super(original);
        this.original = original;
        this.defaultModelOverrides = defaultModelOverrides;
        this.appendModels = appendModels;

        // Resolve default context properties
        TextureAtlasSprite particleSprite = this.original.getParticleIcon();
        boolean ambientOcclusion = true;
        for(ConditionalModel override : this.defaultModelOverrides){
            if(override.conditions == null || override.conditions.test(null, null, null)){
                particleSprite = override.model.getParticleIcon();
                ambientOcclusion = override.model.useAmbientOcclusion();
                break;
            }
        }
        this.particleSprite = particleSprite;
        this.ambientOcclusion = ambientOcclusion;
    }

    public void collectByOffset(ModelsByRandomOffset output, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state, @Nullable ModelData modelData){
        RenderData renderData = modelData == null ? null : modelData.get(RENDER_DATA);
        if(renderData == null)
            renderData = this.getRenderData(level, pos, state, ModelData.EMPTY);

        // Check whether the breaking overlay is being rendered
        boolean isBreakingOverlay = FusionClient.isRenderingBreakingOverlay();

        // Default model
        if(renderData.defaultModel != -1){
            ConditionalModel override = this.defaultModelOverrides.get(renderData.defaultModel);
            if(!isBreakingOverlay || override.showBreakingOverlay)
                output.add(override.randomOffset, override.model, renderData.defaultModelData);
        }else
            output.add(RandomOffsetFunction.MATCH_BLOCK, this.original, renderData.defaultModelData);

        // Append models
        for(int i = 0; i < this.appendModels.size(); i++){
            if(renderData.appendModels[i] == -1)
                continue;
            ConditionalModel conditional = this.appendModels.get(i).get(renderData.appendModels[i]);
            if(!isBreakingOverlay || conditional.showBreakingOverlay){
                ModelData conditionalData = renderData.appendModelsData[i];
                output.add(conditional.randomOffset, conditional.model, conditionalData);
            }
        }
    }

    private RenderData getRenderData(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state, ModelData modelData){
        boolean hasAllArguments = level != null && pos != null && state != null;

        // Default model
        int defaultModel = -1;
        ModelData defaultModelData = null;
        for(int i = 0; i < this.defaultModelOverrides.size(); i++){
            ConditionalModel override = this.defaultModelOverrides.get(i);
            if(override.conditions == null || override.conditions.test(level, pos, state)){
                defaultModel = i;
                defaultModelData = hasAllArguments ? override.model.getModelData(level, pos, state, modelData) : modelData;
            }
        }
        if(defaultModel == -1)
            defaultModelData = hasAllArguments ? this.original.getModelData(level, pos, state, modelData) : modelData;

        // Append models
        int[] appendModels = new int[this.appendModels.size()];
        ModelData[] appendModelsData = new ModelData[this.appendModels.size()];
        for(int i = 0; i < this.appendModels.size(); i++){
            List<ConditionalModel> appendEntry = this.appendModels.get(i);
            appendModels[i] = -1;
            // First model whose conditions are met is submitted
            for(int j = 0; j < appendEntry.size(); j++){
                ConditionalModel conditional = appendEntry.get(j);
                if(conditional.conditions == null || conditional.conditions.test(level, pos, state)){
                    appendModels[i] = j;
                    appendModelsData[i] = hasAllArguments ? conditional.model.getModelData(level, pos, state, modelData) : modelData;
                    break;
                }
            }
        }

        return new RenderData(defaultModel, defaultModelData, appendModels, appendModelsData);
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData){
        RenderData renderData = this.getRenderData(level, pos, state, modelData);
        return ModelData.builder()
            .with(RENDER_DATA, renderData)
            .with(FusionFramedBlocksIntegration.getCacheKeyProperty(), FusionFramedBlocksIntegration.lazyCacheable(() -> createCacheKey(renderData)))
            .build();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, RandomSource random, ModelData modelData, @Nullable RenderType renderType){
        // Get render data
        RenderData renderData = modelData.get(RENDER_DATA);
        if(renderData == null)
            renderData = this.getRenderData(null, null, null, modelData);

        // Get seed to reset random instance
        long seed = random.nextLong();
        random.setSeed(seed);

        // Check whether the breaking overlay is being rendered
        boolean isBreakingOverlay = FusionClient.isRenderingBreakingOverlay();

        // Check whether we need to check the models' render types against the given one
        boolean doRenderTypeCheck = renderType != null && state != null;

        // Collect all quads
        List<BakedQuad> quads = new ArrayList<>();

        // Default model
        if(renderData.defaultModel != -1){
            ConditionalModel override = this.defaultModelOverrides.get(renderData.defaultModel);
            if((!isBreakingOverlay || override.showBreakingOverlay)
                && (!doRenderTypeCheck || override.model.getRenderTypes(state, random, renderData.defaultModelData).contains(renderType)))
                quads.addAll(override.model.getQuads(state, cullDirection, random, renderData.defaultModelData, renderType));
        }else if(!doRenderTypeCheck || this.original.getRenderTypes(state, random, renderData.defaultModelData).contains(renderType))
            quads.addAll(this.original.getQuads(state, cullDirection, random, renderData.defaultModelData, renderType));

        // Append models
        for(int i = 0; i < this.appendModels.size(); i++){
            if(renderData.appendModels[i] == -1)
                continue;
            ConditionalModel conditional = this.appendModels.get(i).get(renderData.appendModels[i]);
            if(!isBreakingOverlay || conditional.showBreakingOverlay){
                random.setSeed(seed);
                ModelData conditionalData = renderData.appendModelsData[i];
                if(!doRenderTypeCheck || conditional.model.getRenderTypes(state, random, conditionalData).contains(renderType))
                    quads.addAll(conditional.model.getQuads(state, cullDirection, random, conditionalData, renderType));
            }
        }
        return quads;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, RandomSource random){
        return this.getQuads(state, cullDirection, random, ModelData.EMPTY, null);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource random, @NotNull ModelData modelData){
        // Get render data
        RenderData renderData = modelData.get(RENDER_DATA);
        if(renderData == null)
            renderData = this.getRenderData(null, null, null, modelData);

        // Get seed to reset random instance
        long seed = random.nextLong();
        random.setSeed(seed);

        // Check whether the breaking overlay is being rendered
        boolean isBreakingOverlay = FusionClient.isRenderingBreakingOverlay();

        // Collect all render types
        ChunkRenderTypeSet renderTypes = ChunkRenderTypeSet.none();

        // Default model
        if(renderData.defaultModel != -1){
            ConditionalModel override = this.defaultModelOverrides.get(renderData.defaultModel);
            if(!isBreakingOverlay || override.showBreakingOverlay)
                renderTypes = ChunkRenderTypeSet.union(renderTypes, override.model.getRenderTypes(state, random, renderData.defaultModelData));
        }else
            renderTypes = ChunkRenderTypeSet.union(renderTypes, this.original.getRenderTypes(state, random, renderData.defaultModelData));

        // Append models
        for(int i = 0; i < this.appendModels.size(); i++){
            if(renderData.appendModels[i] == -1)
                continue;
            ConditionalModel conditional = this.appendModels.get(i).get(renderData.appendModels[i]);
            if(!isBreakingOverlay || conditional.showBreakingOverlay){
                random.setSeed(seed);
                renderTypes = ChunkRenderTypeSet.union(renderTypes, conditional.model.getRenderTypes(state, random, renderData.appendModelsData[i]));
            }
        }

        return renderTypes;
    }

    @Override
    public TextureAtlasSprite getParticleIcon(){
        return this.particleSprite;
    }

    @Override
    public TextureAtlasSprite getParticleIcon(@NotNull ModelData modelData){
        // Get render data
        RenderData renderData = modelData.get(RENDER_DATA);
        if(renderData == null)
            return this.getParticleIcon();

        if(renderData.defaultModel != -1){
            ConditionalModel override = this.defaultModelOverrides.get(renderData.defaultModel);
            return override.model.getParticleIcon(renderData.defaultModelData);
        }
        return this.original.getParticleIcon(renderData.defaultModelData);
    }

    @Override
    public boolean useAmbientOcclusion(){
        return this.ambientOcclusion;
    }

    private static Object createCacheKey(RenderData renderData){
        List<Object> keys = new ArrayList<>(renderData.appendModelsData.length * 2 + 2);
        keys.add(renderData.defaultModel);
        keys.add(FusionFramedBlocksIntegration.getCacheProperty(renderData.defaultModelData));
        for(int i = 0; i < renderData.appendModels.length; i++){
            keys.add(renderData.appendModels[i]);
            keys.add(FusionFramedBlocksIntegration.getCacheProperty(renderData.appendModelsData[i]));
        }
        return keys;
    }

    record ConditionalModel(BakedModel model, @Nullable BlockStateModelPredicate conditions, boolean showBreakingOverlay, RandomOffsetFunction randomOffset) {
    }

    private record RenderData(int defaultModel, ModelData defaultModelData, int[] appendModels, ModelData[] appendModelsData) {
    }
}
