package com.supermartijn642.fusion.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created 08/07/2026 by SuperMartijn642
 */
public abstract class CombinedBakedModel implements BakedModel {

    public static BakedModel of(List<BakedModel> models){
        return new CombinedBakedModel() {
            @Override
            protected List<BakedModel> getModels(){
                return models;
            }
        };
    }

    private static final ModelProperty<ModelData[]> SUB_MODEL_DATA = new ModelProperty<>();

    protected abstract List<BakedModel> getModels();

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData){
        List<BakedModel> models = this.getModels();
        ModelData[] subModelData = new ModelData[models.size()];
        for(int i = 0; i < models.size(); i++)
            subModelData[i] = models.get(i).getModelData(level, pos, state, modelData);
        return ModelData.builder().with(SUB_MODEL_DATA, subModelData).build();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource randomSource, ModelData modelData, @Nullable RenderType renderType){
        ModelData[] subModelData = modelData.get(SUB_MODEL_DATA);
        boolean doRenderTypeCheck = renderType != null && state != null;
        List<BakedQuad> quads = new ArrayList<>();
        List<BakedModel> models = this.getModels();
        for(int i = 0; i < models.size(); i++){
            BakedModel model = models.get(i);
            ModelData subData = subModelData == null ? ModelData.EMPTY : subModelData[i];
            if(!doRenderTypeCheck || model.getRenderTypes(state, randomSource, subData).contains(renderType))
                quads.addAll(model.getQuads(state, side, randomSource, subData, renderType));
        }
        return quads;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction direction, RandomSource randomSource){
        List<BakedQuad> quads = new ArrayList<>();
        for(BakedModel model : this.getModels())
            quads.addAll(model.getQuads(blockState, direction, randomSource));
        return quads;
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
    public boolean useAmbientOcclusion(BlockState state){
        return this.getModels().get(0).useAmbientOcclusion(state);
    }

    @Override
    public boolean useAmbientOcclusion(BlockState state, RenderType renderType){
        return this.getModels().get(0).useAmbientOcclusion(state, renderType);
    }

    @Override
    public BakedModel applyTransform(ItemTransforms.TransformType transformType, PoseStack poseStack, boolean applyLeftHandTransform){
        return this.getModels().get(0).applyTransform(transformType, poseStack, applyLeftHandTransform);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData modelData){
        ModelData[] subModelData = modelData.get(SUB_MODEL_DATA);
        return this.getModels().get(0).getParticleIcon(subModelData == null ? ModelData.EMPTY : subModelData[0]);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData modelData){
        ModelData[] subModelData = modelData.get(SUB_MODEL_DATA);
        ChunkRenderTypeSet renderTypes = ChunkRenderTypeSet.none();
        List<BakedModel> models = this.getModels();
        for(int i = 0; i < models.size(); i++)
            renderTypes = ChunkRenderTypeSet.union(renderTypes, models.get(i).getRenderTypes(state, random, subModelData == null ? ModelData.EMPTY : subModelData[i]));
        return renderTypes;
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack itemStack, boolean fabulous){
        return this.getModels().get(0).getRenderTypes(itemStack, fabulous);
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack itemStack, boolean fabulous){
        return this.getModels().get(0).getRenderPasses(itemStack, fabulous);
    }
}
