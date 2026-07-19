package com.supermartijn642.fusion.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
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
 * Created 08/07/2026 by SuperMartijn642
 */
public abstract class CombinedBakedModel implements BakedModel, CustomRenderTypeBakedModel {

    public static BakedModel of(List<BakedModel> models){
        return new CombinedBakedModel() {
            @Override
            protected List<BakedModel> getModels(){
                return models;
            }
        };
    }

    private static final ModelProperty<IModelData[]> SUB_MODEL_DATA = new ModelProperty<>();

    protected abstract List<BakedModel> getModels();

    protected IModelData getModelData(int modelIndex, BakedModel model, BlockAndTintGetter level, BlockPos pos, BlockState state, IModelData modelData){
        return model.getModelData(level, pos, state, modelData);
    }

    @Override
    public IModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, IModelData modelData){
        List<BakedModel> models = this.getModels();
        IModelData[] subModelData = new IModelData[models.size()];
        for(int i = 0; i < models.size(); i++)
            subModelData[i] = this.getModelData(i, models.get(i), level, pos, state, modelData);
        return new ModelDataMap.Builder().withInitial(SUB_MODEL_DATA, subModelData).build();
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random random, IModelData modelData){
        IModelData[] subModelData = modelData.getData(SUB_MODEL_DATA);

        // Check whether we need to check the models' render types against the given one
        RenderType renderType = MinecraftForgeClient.getRenderLayer();
        boolean doRenderTypeCheck = renderType != null && state != null;
        boolean isDefaultRenderType = !doRenderTypeCheck || ModelRenderTypeHelper.couldBlockRenderInLayerOriginally(state, renderType);

        List<BakedQuad> quads = new ArrayList<>();
        List<BakedModel> models = this.getModels();
        for(int i = 0; i < models.size(); i++){
            BakedModel model = models.get(i);
            if(!doRenderTypeCheck || ModelRenderTypeHelper.canRenderInLayer(model, state, renderType, isDefaultRenderType))
                quads.addAll(model.getQuads(state, side, random, subModelData == null ? EmptyModelData.INSTANCE : subModelData[i]));
        }
        return quads;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction direction, Random random){
        List<BakedQuad> quads = new ArrayList<>();
        for(BakedModel model : this.getModels())
            quads.addAll(model.getQuads(blockState, direction, random));
        return quads;
    }

    @Override
    public boolean canRenderInLayer(BlockState state, RenderType renderType){
        // Check whether the render type is a default one for the state
        boolean isDefaultRenderType = ModelRenderTypeHelper.couldBlockRenderInLayerOriginally(state, renderType);
        // Check models
        for(BakedModel model : this.getModels()){
            if(ModelRenderTypeHelper.canRenderInLayer(model, state, renderType, isDefaultRenderType))
                return true;
        }
        return false;
    }

    @Override
    public boolean useAmbientOcclusion(){
        return this.getModels().get(0).useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d(){
        return this.getModels().get(0).isGui3d();
    }

    @Override
    public boolean usesBlockLight(){
        return this.getModels().get(0).usesBlockLight();
    }

    @Override
    public TextureAtlasSprite getParticleIcon(){
        return this.getModels().get(0).getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms(){
        return this.getModels().get(0).getTransforms();
    }

    @Override
    public ItemOverrides getOverrides(){
        return this.getModels().get(0).getOverrides();
    }

    @Override
    public boolean isCustomRenderer(){
        return this.getModels().get(0).isCustomRenderer();
    }

    @Override
    public boolean isAmbientOcclusion(BlockState state){
        return this.getModels().get(0).isAmbientOcclusion(state);
    }

    @Override
    public boolean doesHandlePerspectives(){
        return this.getModels().get(0).doesHandlePerspectives();
    }

    @Override
    public BakedModel handlePerspective(ItemTransforms.TransformType displayContext, PoseStack poseStack){
        return this.getModels().get(0).handlePerspective(displayContext, poseStack);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(IModelData modelData){
        IModelData[] subModelData = modelData.getData(SUB_MODEL_DATA);
        return this.getModels().get(0).getParticleIcon(subModelData == null ? EmptyModelData.INSTANCE : subModelData[0]);
    }

    @Override
    public boolean isLayered(){
        return this.getModels().get(0).isLayered();
    }

    @Override
    public List<Pair<BakedModel,RenderType>> getLayerModels(ItemStack stack, boolean fabulous){
        return this.getModels().get(0).getLayerModels(stack, fabulous);
    }
}
