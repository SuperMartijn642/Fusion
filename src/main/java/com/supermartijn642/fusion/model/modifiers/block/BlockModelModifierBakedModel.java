package com.supermartijn642.fusion.model.modifiers.block;

import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.model.ModelRenderTypeHelper;
import com.supermartijn642.fusion.model.WrappedBakedModel;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created 19/09/2024 by SuperMartijn642
 */
public class BlockModelModifierBakedModel extends WrappedBakedModel {

    private static final ModelProperty<RenderData> RENDER_DATA = new ModelProperty<>();

    private final IBakedModel original;
    private final List<ConditionalModel> defaultModelOverrides;
    private final List<List<ConditionalModel>> appendModels;
    private final TextureAtlasSprite particleSprite;
    private final boolean ambientOcclusion;

    BlockModelModifierBakedModel(IBakedModel original, List<ConditionalModel> defaultModelOverrides, List<List<ConditionalModel>> appendModels){
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

    public void collectByOffset(ModelsByRandomOffset output, @Nullable IBlockDisplayReader level, @Nullable BlockPos pos, @Nullable BlockState state){
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

    private RenderData getRenderData(@Nullable IBlockDisplayReader level, @Nullable BlockPos pos, @Nullable BlockState state, IModelData modelData){
        boolean hasAllArguments = level != null && pos != null && state != null;

        // Default model
        int defaultModel = -1;
        IModelData defaultModelData = null;
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
        IModelData[] appendModelsData = new IModelData[this.appendModels.size()];
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
    public @NotNull IModelData getModelData(@NotNull IBlockDisplayReader level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull IModelData modelData){
        return new ModelDataMap.Builder()
            .withInitial(RENDER_DATA, this.getRenderData(level, pos, state, modelData))
            .build();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, Random random, IModelData modelData){
        // Get render data
        RenderData renderData = modelData.getData(RENDER_DATA);
        if(renderData == null)
            renderData = this.getRenderData(null, null, null, modelData);

        // Get seed to reset random instance
        long seed = random.nextLong();
        random.setSeed(seed);

        // Check whether the breaking overlay is being rendered
        boolean isBreakingOverlay = FusionClient.isRenderingBreakingOverlay();

        // Check whether we need to check the models' render types against the given one
        RenderType renderType = MinecraftForgeClient.getRenderLayer();
        boolean doRenderTypeCheck = renderType != null && state != null;
        boolean isDefaultRenderType = !doRenderTypeCheck || ModelRenderTypeHelper.couldBlockRenderInLayerOriginally(state, renderType);

        // Collect all quads
        List<BakedQuad> quads = new ArrayList<>();

        // Default model
        if(renderData.defaultModel != -1){
            ConditionalModel override = this.defaultModelOverrides.get(renderData.defaultModel);
            if((!isBreakingOverlay || override.showBreakingOverlay)
                && (!doRenderTypeCheck || ModelRenderTypeHelper.canRenderInLayer(override.model, state, renderType, isDefaultRenderType)))
                quads.addAll(override.model.getQuads(state, cullDirection, random, renderData.defaultModelData));
        }else if(!doRenderTypeCheck || ModelRenderTypeHelper.canRenderInLayer(this.original, state, renderType, isDefaultRenderType))
            quads.addAll(this.original.getQuads(state, cullDirection, random, renderData.defaultModelData));

        // Append models
        for(int i = 0; i < this.appendModels.size(); i++){
            if(renderData.appendModels[i] == -1)
                continue;
            ConditionalModel conditional = this.appendModels.get(i).get(renderData.appendModels[i]);
            if(!isBreakingOverlay || conditional.showBreakingOverlay){
                random.setSeed(seed);
                IModelData conditionalData = renderData.appendModelsData[i];
                if(!doRenderTypeCheck || ModelRenderTypeHelper.canRenderInLayer(conditional.model, state, renderType, isDefaultRenderType))
                    quads.addAll(conditional.model.getQuads(state, cullDirection, random, conditionalData));
            }
        }
        return quads;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, Random random){
        return this.getQuads(state, cullDirection, random, EmptyModelData.INSTANCE);
    }

    @Override
    public boolean canRenderInLayer(BlockState state, RenderType renderType){
        // Check whether the render type is a default one for the state
        boolean isDefaultRenderType = ModelRenderTypeHelper.couldBlockRenderInLayerOriginally(state, renderType);

        // Default model
        for(ConditionalModel override : this.defaultModelOverrides){
            if(ModelRenderTypeHelper.canRenderInLayer(override.model, state, renderType, isDefaultRenderType))
                return true;
        }
        if(ModelRenderTypeHelper.canRenderInLayer(this.original, state, renderType, isDefaultRenderType))
            return true;

        // Append models
        for(List<ConditionalModel> appendEntry : this.appendModels){
            for(ConditionalModel conditional : appendEntry){
                if(ModelRenderTypeHelper.canRenderInLayer(conditional.model, state, renderType, isDefaultRenderType))
                    return true;
            }
        }
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon(){
        return this.particleSprite;
    }

    @Override
    public TextureAtlasSprite getParticleTexture(@NotNull IModelData modelData){
        // Get render data
        RenderData renderData = modelData.getData(RENDER_DATA);
        if(renderData == null)
            return this.getParticleIcon();

        if(renderData.defaultModel != -1){
            ConditionalModel override = this.defaultModelOverrides.get(renderData.defaultModel);
            return override.model.getParticleTexture(renderData.defaultModelData);
        }
        return this.original.getParticleTexture(renderData.defaultModelData);
    }

    @Override
    public boolean useAmbientOcclusion(){
        return this.ambientOcclusion;
    }

    static final class ConditionalModel {
        private final IBakedModel model;
        private final @Nullable BlockStateModelPredicate conditions;
        private final boolean showBreakingOverlay;
        private final RandomOffsetFunction randomOffset;

        ConditionalModel(IBakedModel model, @Nullable BlockStateModelPredicate conditions, boolean showBreakingOverlay, RandomOffsetFunction randomOffset){
            this.model = model;
            this.conditions = conditions;
            this.showBreakingOverlay = showBreakingOverlay;
            this.randomOffset = randomOffset;
        }
    }

    private static final class RenderData {
        private final int defaultModel;
        private final IModelData defaultModelData;
        private final int[] appendModels;
        private final IModelData[] appendModelsData;

        private RenderData(int defaultModel, IModelData defaultModelData, int[] appendModels, IModelData[] appendModelsData){
            this.defaultModel = defaultModel;
            this.defaultModelData = defaultModelData;
            this.appendModels = appendModels;
            this.appendModelsData = appendModelsData;
        }
    }
}
