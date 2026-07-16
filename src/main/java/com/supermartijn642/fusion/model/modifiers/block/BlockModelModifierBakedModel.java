package com.supermartijn642.fusion.model.modifiers.block;

import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.model.BlockRenderContext;
import com.supermartijn642.fusion.model.ModelRenderTypeHelper;
import com.supermartijn642.fusion.model.WrappedBakedModel;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.MinecraftForgeClient;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created 19/09/2024 by SuperMartijn642
 */
public class BlockModelModifierBakedModel extends WrappedBakedModel {

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
        TextureAtlasSprite particleSprite = this.original.getParticleTexture();
        boolean ambientOcclusion = this.original.isAmbientOcclusion();
        for(ConditionalModel override : this.defaultModelOverrides){
            if(override.conditions == null || override.conditions.test(null, null, null)){
                particleSprite = override.model.getParticleTexture();
                ambientOcclusion = override.model.isAmbientOcclusion();
                break;
            }
        }
        this.particleSprite = particleSprite;
        this.ambientOcclusion = ambientOcclusion;
    }

    public void collectByOffset(ModelsByRandomOffset output, @Nullable IBlockAccess level, @Nullable BlockPos pos, @Nullable IBlockState state){
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

    private RenderData getRenderData(@Nullable IBlockAccess level, @Nullable BlockPos pos, @Nullable IBlockState state){
        // Default model
        int defaultModel = -1;
        for(int i = 0; i < this.defaultModelOverrides.size(); i++){
            ConditionalModel override = this.defaultModelOverrides.get(i);
            if(override.conditions == null || override.conditions.test(level, pos, state))
                defaultModel = i;
        }

        // Append models
        int[] appendModels = new int[this.appendModels.size()];
        for(int i = 0; i < this.appendModels.size(); i++){
            List<ConditionalModel> appendEntry = this.appendModels.get(i);
            appendModels[i] = -1;
            // First model whose conditions are met is submitted
            for(int j = 0; j < appendEntry.size(); j++){
                ConditionalModel conditional = appendEntry.get(j);
                if(conditional.conditions == null || conditional.conditions.test(level, pos, state)){
                    appendModels[i] = j;
                    break;
                }
            }
        }

        return new RenderData(defaultModel, appendModels);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing cullDirection, long seed){
        // Get render data
        BlockRenderContext blockRenderContext = FusionClient.BLOCK_RENDER_CONTEXT.get();
        RenderData renderData = blockRenderContext == null ?
            this.getRenderData(null, null, null) :
            this.getRenderData(blockRenderContext.level(), blockRenderContext.pos(), blockRenderContext.state());

        // Check whether the breaking overlay is being rendered
        boolean isBreakingOverlay = FusionClient.isRenderingBreakingOverlay();

        // Check whether we need to check the models' render types against the given one
        BlockRenderLayer renderType = MinecraftForgeClient.getRenderLayer();
        boolean doRenderTypeCheck = renderType != null && state != null;
        boolean isDefaultRenderType = !doRenderTypeCheck || ModelRenderTypeHelper.couldBlockRenderInLayerOriginally(state, renderType);

        // Collect all quads
        List<BakedQuad> quads = new ArrayList<>();

        // Default model
        if(renderData.defaultModel != -1){
            ConditionalModel override = this.defaultModelOverrides.get(renderData.defaultModel);
            if((!isBreakingOverlay || override.showBreakingOverlay)
                && (!doRenderTypeCheck || ModelRenderTypeHelper.canRenderInLayer(override.model, state, renderType, isDefaultRenderType)))
                quads.addAll(override.model.getQuads(state, cullDirection, seed));
        }else if(!doRenderTypeCheck || ModelRenderTypeHelper.canRenderInLayer(this.original, state, renderType, isDefaultRenderType))
            quads.addAll(this.original.getQuads(state, cullDirection, seed));

        // Append models
        for(int i = 0; i < this.appendModels.size(); i++){
            if(renderData.appendModels[i] == -1)
                continue;
            ConditionalModel conditional = this.appendModels.get(i).get(renderData.appendModels[i]);
            if((!isBreakingOverlay || conditional.showBreakingOverlay) &&
                (!doRenderTypeCheck || ModelRenderTypeHelper.canRenderInLayer(conditional.model, state, renderType, isDefaultRenderType)))
                quads.addAll(conditional.model.getQuads(state, cullDirection, seed));
        }
        return quads;
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer renderType){
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
    public TextureAtlasSprite getParticleTexture(){
        return this.particleSprite;
    }

    @Override
    public boolean isAmbientOcclusion(){
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
        private final int[] appendModels;

        private RenderData(int defaultModel, int[] appendModels){
            this.defaultModel = defaultModel;
            this.appendModels = appendModels;
        }
    }
}
