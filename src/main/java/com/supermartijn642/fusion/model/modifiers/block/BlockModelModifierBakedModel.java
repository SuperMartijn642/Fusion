package com.supermartijn642.fusion.model.modifiers.block;

import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Created 19/09/2024 by SuperMartijn642
 */
public class BlockModelModifierBakedModel implements BlockStateModel {

    private static final ModelProperty<RenderData> RENDER_DATA = new ModelProperty<>();

    private final BlockStateModel original;
    private final List<ConditionalModel> defaultModelOverrides;
    private final List<List<ConditionalModel>> appendModels;
    private final TextureAtlasSprite particleSprite;

    BlockModelModifierBakedModel(BlockStateModel original, List<ConditionalModel> defaultModelOverrides, List<List<ConditionalModel>> appendModels){
        this.original = original;
        this.defaultModelOverrides = defaultModelOverrides;
        this.appendModels = appendModels;

        // Resolve particle material
        TextureAtlasSprite particleSprite = null;
        for(ConditionalModel override : this.defaultModelOverrides){
            if(override.conditions == null || override.conditions.test(null, null, null)){
                particleSprite = override.model.particleIcon();
                break;
            }
        }
        if(particleSprite == null)
            particleSprite = this.original.particleIcon();
        this.particleSprite = particleSprite;
    }

    public void collectByOffset(ModelsByRandomOffset output, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state){
        // Check whether the breaking overlay is being rendered
        boolean isBreakingOverlay = FusionClient.isRenderingBreakingOverlay();

        // Default model
        overrides:
        {
            for(ConditionalModel override : this.defaultModelOverrides){
                if(override.conditions == null || override.conditions.test(level, pos, state)){
                    if(!isBreakingOverlay || override.showBreakingOverlay)
                        output.add(override.randomOffset, override.model);
                    break overrides;
                }
            }
            output.add(RandomOffsetFunction.MATCH_BLOCK, this.original);
        }

        // Append models
        for(List<ConditionalModel> appendEntry : this.appendModels){
            // First model whose conditions are met is submitted
            for(ConditionalModel conditional : appendEntry){
                if(conditional.conditions == null || conditional.conditions.test(level, pos, state)){
                    if(!isBreakingOverlay || conditional.showBreakingOverlay)
                        output.add(conditional.randomOffset, conditional.model);
                    break;
                }
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

        return new RenderData(state, defaultModel, defaultModelData, appendModels, appendModelsData);
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData){
        return ModelData.builder()
            .with(RENDER_DATA, this.getRenderData(level, pos, state, modelData))
            .build();
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> parts, ModelData modelData, ChunkSectionLayer renderType){
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
        boolean doRenderTypeCheck = renderType != null && renderData.state != null;

        // Default model
        if(renderData.defaultModel != -1){
            ConditionalModel override = this.defaultModelOverrides.get(renderData.defaultModel);
            if((!isBreakingOverlay || override.showBreakingOverlay)
                && (!doRenderTypeCheck || override.model.getRenderTypes(renderData.state, random, renderData.defaultModelData).contains(renderType)))
                override.model.collectParts(random, parts, renderData.defaultModelData, renderType);
        }else if(!doRenderTypeCheck || this.original.getRenderTypes(renderData.state, random, renderData.defaultModelData).contains(renderType))
            this.original.collectParts(random, parts, renderData.defaultModelData, renderType);

        // Append models
        for(int i = 0; i < this.appendModels.size(); i++){
            if(renderData.appendModels[i] == -1)
                continue;
            ConditionalModel conditional = this.appendModels.get(i).get(renderData.appendModels[i]);
            if(!isBreakingOverlay || conditional.showBreakingOverlay){
                random.setSeed(seed);
                ModelData conditionalData = renderData.appendModelsData[i];
                if(!doRenderTypeCheck || conditional.model.getRenderTypes(renderData.state, random, conditionalData).contains(renderType))
                    conditional.model.collectParts(random, parts, conditionalData, renderType);
            }
        }
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> parts){
        this.collectParts(random, parts, ModelData.EMPTY, null);
    }

    @Override
    public Collection<ChunkSectionLayer> getRenderTypes(@NotNull BlockState state, @NotNull RandomSource random, @NotNull ModelData modelData){
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
        Set<ChunkSectionLayer> renderTypes = EnumSet.noneOf(ChunkSectionLayer.class);

        // Default model
        if(renderData.defaultModel != -1){
            ConditionalModel override = this.defaultModelOverrides.get(renderData.defaultModel);
            if(!isBreakingOverlay || override.showBreakingOverlay)
                renderTypes.addAll(override.model.getRenderTypes(state, random, renderData.defaultModelData));
        }else
            renderTypes.addAll(this.original.getRenderTypes(state, random, renderData.defaultModelData));

        // Append models
        for(int i = 0; i < this.appendModels.size(); i++){
            if(renderData.appendModels[i] == -1)
                continue;
            ConditionalModel conditional = this.appendModels.get(i).get(renderData.appendModels[i]);
            if(!isBreakingOverlay || conditional.showBreakingOverlay){
                random.setSeed(seed);
                renderTypes.addAll(conditional.model.getRenderTypes(state, random, renderData.appendModelsData[i]));
            }
        }

        return renderTypes;
    }

    @Override
    public TextureAtlasSprite particleIcon(){
        return this.particleSprite;
    }

    @Override
    public TextureAtlasSprite particleIcon(@NotNull ModelData data){
        // Get render data
        RenderData renderData = data.get(RENDER_DATA);
        if(renderData == null)
            return this.particleIcon();

        if(renderData.defaultModel != -1){
            ConditionalModel override = this.defaultModelOverrides.get(renderData.defaultModel);
            return override.model.particleIcon(renderData.defaultModelData);
        }
        return this.original.particleIcon(renderData.defaultModelData);
    }

    record ConditionalModel(BlockStateModel model, @Nullable BlockStateModelPredicate conditions, boolean showBreakingOverlay, RandomOffsetFunction randomOffset) {
    }

    private record RenderData(BlockState state, int defaultModel, ModelData defaultModelData, int[] appendModels, ModelData[] appendModelsData) {
    }
}
