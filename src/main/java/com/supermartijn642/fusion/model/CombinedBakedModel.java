package com.supermartijn642.fusion.model;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ILightReader;
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
public abstract class CombinedBakedModel implements IBakedModel, CustomRenderTypeBakedModel {

    public static IBakedModel of(List<IBakedModel> models){
        return new CombinedBakedModel() {
            @Override
            protected List<IBakedModel> getModels(){
                return models;
            }
        };
    }

    private static final ModelProperty<IModelData[]> SUB_MODEL_DATA = new ModelProperty<>();

    protected abstract List<IBakedModel> getModels();

    @Override
    public IModelData getModelData(ILightReader level, BlockPos pos, BlockState state, IModelData modelData){
        List<IBakedModel> models = this.getModels();
        IModelData[] subModelData = new IModelData[models.size()];
        for(int i = 0; i < models.size(); i++)
            subModelData[i] = models.get(i).getModelData(level, pos, state, modelData);
        return new ModelDataMap.Builder().withInitial(SUB_MODEL_DATA, subModelData).build();
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random random, IModelData modelData){
        IModelData[] subModelData = modelData.getData(SUB_MODEL_DATA);
        List<BakedQuad> quads = new ArrayList<>();
        List<IBakedModel> models = this.getModels();
        for(int i = 0; i < models.size(); i++)
            quads.addAll(models.get(i).getQuads(state, side, random, subModelData == null ? EmptyModelData.INSTANCE : subModelData[i]));
        return quads;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction direction, Random random){
        List<BakedQuad> quads = new ArrayList<>();
        for(IBakedModel model : this.getModels())
            quads.addAll(model.getQuads(blockState, direction, random));
        return quads;
    }

    @Override
    public boolean canRenderInLayer(BlockState state, RenderType renderType){
        // Check whether the render type is a default one for the state
        boolean isDefaultRenderType = ModelRenderTypeHelper.couldBlockRenderInLayerOriginally(state, renderType);
        // Check models
        for(IBakedModel model : this.getModels()){
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
    public ItemCameraTransforms getTransforms(){
        return this.getModels().get(0).getTransforms();
    }

    @Override
    public ItemOverrideList getOverrides(){
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
    public IBakedModel handlePerspective(ItemCameraTransforms.TransformType displayContext, MatrixStack poseStack){
        return this.getModels().get(0).handlePerspective(displayContext, poseStack);
    }

    @Override
    public TextureAtlasSprite getParticleTexture(IModelData modelData){
        IModelData[] subModelData = modelData.getData(SUB_MODEL_DATA);
        return this.getModels().get(0).getParticleTexture(subModelData == null ? EmptyModelData.INSTANCE : subModelData[0]);
    }
}
