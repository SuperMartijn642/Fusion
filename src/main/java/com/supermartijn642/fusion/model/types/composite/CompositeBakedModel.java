package com.supermartijn642.fusion.model.types.composite;

import com.mojang.datafixers.util.Pair;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.model.CustomRenderTypeBakedModel;
import com.supermartijn642.fusion.model.ModelRenderTypeHelper;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created 15/06/2026 by SuperMartijn642
 */
public class CompositeBakedModel implements BakedModel, CustomRenderTypeBakedModel {

    private static final ModelProperty<RenderData> RENDER_DATA = new ModelProperty<>();

    private final BakedModel defaultModel;
    private final List<ConditionalList> entries;
    private final int initialCapacityGuess;

    public CompositeBakedModel(BakedModel defaultModel, List<ConditionalList> entries){
        this.defaultModel = defaultModel;
        this.entries = entries;

        // Resolve default values
        int guaranteedModelCount = 0;
        for(ConditionalList list : entries){
            for(ModelEntry entry : list.entries){
                if(entry.predicate == null || entry.predicate.alwaysTrue())
                    guaranteedModelCount++;
            }
        }
        this.initialCapacityGuess = Math.min(guaranteedModelCount * 2, entries.size());
    }

    private RenderData getRenderData(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state, @Nullable IModelData modelData){
        boolean canCollectModelData = level != null && pos != null && state != null;
        if(modelData == null)
            modelData = EmptyModelData.INSTANCE;
        List<BakedModel> models = new ArrayList<>(this.initialCapacityGuess);
        List<IModelData> subModelData = canCollectModelData ? new ArrayList<>(this.initialCapacityGuess) : null;
        for(ConditionalList list : this.entries){
            BakedModel model = list.get(level, pos, state);
            if(model == null)
                continue;
            models.add(model);
            if(canCollectModelData)
                subModelData.add(model.getModelData(level, pos, state, modelData));
        }
        return new RenderData(models, subModelData, modelData);
    }

    @Override
    public @NotNull IModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull IModelData modelData){
        return new ModelDataMap.Builder()
            .withInitial(RENDER_DATA, this.getRenderData(level, pos, state, modelData))
            .build();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, Random random, IModelData modelData){
        RenderData renderData = modelData.getData(RENDER_DATA);
        if(renderData == null)
            renderData = this.getRenderData(null, null, state, modelData);

        // Get seed to reset random instance
        long seed = random.nextLong();

        // Check whether we need to check the models' render types against the given one
        RenderType renderType = MinecraftForgeClient.getRenderType();
        boolean shouldCheckRenderType = state != null && renderType != null;
        boolean isDefaultRenderType = !shouldCheckRenderType || ModelRenderTypeHelper.couldBlockRenderInLayerOriginally(state, renderType);

        List<BakedQuad> quads = new ArrayList<>();
        for(int i = 0; i < renderData.models.size(); i++){
            BakedModel model = renderData.models.get(i);
            IModelData data = renderData.modelData == null ? renderData.generalModelData : renderData.modelData.get(i);
            if(!shouldCheckRenderType || ModelRenderTypeHelper.canRenderInLayer(model, state, renderType, isDefaultRenderType)){
                random.setSeed(seed);
                quads.addAll(model.getQuads(state, cullDirection, random, data));
            }
        }
        return quads;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, Random random){
        // Get seed to reset random instance
        long seed = random.nextLong();

        // Check whether we need to check the models' render types against the given one
        RenderType renderType = MinecraftForgeClient.getRenderType();
        boolean shouldCheckRenderType = state != null && renderType != null;
        boolean isDefaultRenderType = !shouldCheckRenderType || ModelRenderTypeHelper.couldBlockRenderInLayerOriginally(state, renderType);

        List<BakedQuad> quads = new ArrayList<>();
        for(ConditionalList list : this.entries){
            BakedModel model = list.get(null, null, state);
            if(model == null)
                continue;
            if(!shouldCheckRenderType || ModelRenderTypeHelper.canRenderInLayer(model, state, renderType, isDefaultRenderType)){
                random.setSeed(seed);
                quads.addAll(model.getQuads(state, cullDirection, random));
            }
        }
        return quads;
    }

    @Override
    public boolean canRenderInLayer(BlockState state, RenderType renderType){
        boolean isDefaultRenderType = ModelRenderTypeHelper.couldBlockRenderInLayerOriginally(state, renderType);
        for(ConditionalList list : this.entries){
            for(ModelEntry entry : list.entries){
                if(ModelRenderTypeHelper.canRenderInLayer(entry.model, state, renderType, isDefaultRenderType))
                    return true;
            }
        }
        return false;
    }

    @Override
    public List<Pair<BakedModel,RenderType>> getLayerModels(ItemStack stack, boolean fabulous){
        List<Pair<BakedModel,RenderType>> models = new ArrayList<>(this.initialCapacityGuess);
        for(ConditionalList list : this.entries){
            BakedModel model = list.get(stack);
            if(model != null)
                models.addAll(model.getLayerModels(stack, fabulous));
        }
        return models;
    }

    @Override
    public boolean isLayered(){
        return true;
    }

    @Override
    public TextureAtlasSprite getParticleIcon(@NotNull IModelData modelData){
        RenderData renderData = modelData.getData(RENDER_DATA);
        if(renderData == null)
            return this.defaultModel.getParticleIcon(modelData);
        if(renderData.models.isEmpty())
            return this.defaultModel.getParticleIcon(renderData.generalModelData);
        BakedModel model = renderData.models.get(0);
        IModelData data = renderData.modelData == null ? renderData.generalModelData : renderData.modelData.get(0);
        return model.getParticleIcon(data);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(){
        return this.defaultModel.getParticleIcon();
    }

    @Override
    public boolean useAmbientOcclusion(){
        return this.defaultModel.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d(){
        return this.defaultModel.isGui3d();
    }

    @Override
    public boolean usesBlockLight(){
        return this.defaultModel.usesBlockLight();
    }

    @Override
    public ItemTransforms getTransforms(){
        return this.defaultModel.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides(){
        return this.defaultModel.getOverrides();
    }

    @Override
    public boolean isCustomRenderer(){
        return false;
    }

    public record ConditionalList(List<ModelEntry> entries) {
        @Nullable
        BakedModel get(BlockAndTintGetter level, BlockPos pos, BlockState state){
            for(ModelEntry entry : this.entries){
                if(entry.predicate == null || entry.predicate.testForBlockState(level, pos, state))
                    return entry.model;
            }
            return null;
        }

        @Nullable
        BakedModel get(ItemStack stack){
            for(ModelEntry entry : this.entries){
                if(entry.predicate == null || entry.predicate.testForItem(stack))
                    return entry.model;
            }
            return null;
        }
    }

    public record ModelEntry(BakedModel model, ModelPredicate predicate) {
    }

    private record RenderData(List<BakedModel> models, @Nullable List<IModelData> modelData, IModelData generalModelData) {
    }
}
