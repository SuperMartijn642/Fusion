package com.supermartijn642.fusion.model.types.composite;

import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.model.CustomRenderTypeBakedModel;
import com.supermartijn642.fusion.model.ModelRenderTypeHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ILightReader;
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
public class CompositeBakedModel implements IBakedModel, CustomRenderTypeBakedModel {

    private static final ModelProperty<RenderData> RENDER_DATA = new ModelProperty<>();

    private final IBakedModel defaultModel;
    private final List<ConditionalList> entries;
    private final int initialCapacityGuess;

    public CompositeBakedModel(IBakedModel defaultModel, List<ConditionalList> entries){
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

    private RenderData getRenderData(@Nullable ItemStack stack, @Nullable ILightReader level, @Nullable BlockPos pos, @Nullable BlockState state, @Nullable IModelData modelData){
        boolean canCollectModelData = level != null && pos != null && state != null;
        if(modelData == null)
            modelData = EmptyModelData.INSTANCE;
        List<IBakedModel> models = new ArrayList<>(this.initialCapacityGuess);
        List<IModelData> subModelData = canCollectModelData ? new ArrayList<>(this.initialCapacityGuess) : null;
        for(ConditionalList list : this.entries){
            IBakedModel model = stack == null ?
                list.get(level, pos, state) :
                list.get(stack);
            if(model == null)
                continue;
            models.add(model);
            if(canCollectModelData)
                subModelData.add(model.getModelData(level, pos, state, modelData));
        }
        return new RenderData(stack != null, models, subModelData, modelData);
    }

    @Override
    public @NotNull IModelData getModelData(@NotNull ILightReader level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull IModelData modelData){
        ItemStack stack = FusionClient.ITEM_STACK_RENDER_CONTEXT.get();
        return new ModelDataMap.Builder()
            .withInitial(RENDER_DATA, this.getRenderData(stack, level, pos, state, modelData))
            .build();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, Random random, IModelData modelData){
        // Check whether we are rendering an item
        ItemStack stack = FusionClient.ITEM_STACK_RENDER_CONTEXT.get();

        RenderData renderData = modelData.getData(RENDER_DATA);
        if(renderData == null || ((stack == null) == renderData.isForItem))
            renderData = this.getRenderData(stack, null, null, state, modelData);

        // Get seed to reset random instance
        long seed = random.nextLong();

        // Check whether we need to check the models' render types against the given one
        RenderType renderType = MinecraftForgeClient.getRenderLayer();
        boolean shouldCheckRenderType = state != null && renderType != null;
        boolean isDefaultRenderType = !shouldCheckRenderType || ModelRenderTypeHelper.couldBlockRenderInLayerOriginally(state, renderType);

        List<BakedQuad> quads = new ArrayList<>();
        for(int i = 0; i < renderData.models.size(); i++){
            IBakedModel model = renderData.models.get(i);
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
        // Check whether we are rendering an item
        ItemStack stack = FusionClient.ITEM_STACK_RENDER_CONTEXT.get();

        // Get seed to reset random instance
        long seed = random.nextLong();

        // Check whether we need to check the models' render types against the given one
        RenderType renderType = MinecraftForgeClient.getRenderLayer();
        boolean shouldCheckRenderType = state != null && renderType != null;
        boolean isDefaultRenderType = !shouldCheckRenderType || ModelRenderTypeHelper.couldBlockRenderInLayerOriginally(state, renderType);

        List<BakedQuad> quads = new ArrayList<>();
        for(ConditionalList list : this.entries){
            IBakedModel model = stack == null ?
                list.get(null, null, state) :
                list.get(stack);
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
    public TextureAtlasSprite getParticleTexture(@NotNull IModelData modelData){
        RenderData renderData = modelData.getData(RENDER_DATA);
        if(renderData == null)
            return this.defaultModel.getParticleTexture(modelData);
        if(renderData.models.isEmpty())
            return this.defaultModel.getParticleTexture(renderData.generalModelData);
        IBakedModel model = renderData.models.get(0);
        IModelData data = renderData.modelData == null ? renderData.generalModelData : renderData.modelData.get(0);
        return model.getParticleTexture(data);
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
    public ItemCameraTransforms getTransforms(){
        return this.defaultModel.getTransforms();
    }

    @Override
    public ItemOverrideList getOverrides(){
        return this.defaultModel.getOverrides();
    }

    @Override
    public boolean isCustomRenderer(){
        return false;
    }

    public static class ConditionalList {
        private final List<ModelEntry> entries;

        public ConditionalList(List<ModelEntry> entries){
            this.entries = entries;
        }

        @Nullable
        IBakedModel get(ILightReader level, BlockPos pos, BlockState state){
            for(ModelEntry entry : this.entries){
                if(entry.predicate == null || entry.predicate.testForBlockState(level, pos, state))
                    return entry.model;
            }
            return null;
        }

        @Nullable
        IBakedModel get(ItemStack stack){
            for(ModelEntry entry : this.entries){
                if(entry.predicate == null || entry.predicate.testForItem(stack))
                    return entry.model;
            }
            return null;
        }
    }

    public static class ModelEntry {
        private final IBakedModel model;
        private final ModelPredicate predicate;

        public ModelEntry(IBakedModel model, ModelPredicate predicate){
            this.model = model;
            this.predicate = predicate;
        }
    }

    private static class RenderData {
        private final boolean isForItem;
        private final List<IBakedModel> models;
        private final @Nullable List<IModelData> modelData;
        private final IModelData generalModelData;

        private RenderData(boolean isForItem, List<IBakedModel> models, @Nullable List<IModelData> modelData, IModelData generalModelData){
            this.isForItem = isForItem;
            this.models = models;
            this.modelData = modelData;
            this.generalModelData = generalModelData;
        }
    }
}
