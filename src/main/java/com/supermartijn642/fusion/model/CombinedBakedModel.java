package com.supermartijn642.fusion.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedOverrides;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.common.util.TriState;
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
        return this.getModels().getFirst().useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d(){
        return this.getModels().getFirst().isGui3d();
    }

    @Override
    public boolean usesBlockLight(){
        return this.getModels().getFirst().usesBlockLight();
    }

    @Override
    public TextureAtlasSprite getParticleIcon(){
        return this.getModels().getFirst().getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms(){
        return this.getModels().getFirst().getTransforms();
    }

    @Override
    public BakedOverrides overrides(){
        return this.getModels().getFirst().overrides();
    }

    @Override
    public boolean isCustomRenderer(){
        return this.getModels().getFirst().isCustomRenderer();
    }

    @Override
    public TriState useAmbientOcclusion(BlockState state, ModelData modelData, RenderType renderType){
        ModelData[] subModelData = modelData.get(SUB_MODEL_DATA);
        return this.getModels().getFirst().useAmbientOcclusion(state, subModelData == null ? ModelData.EMPTY : subModelData[0], renderType);
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext transformType, PoseStack poseStack, boolean applyLeftHandTransform){
        return this.getModels().getFirst().applyTransform(transformType, poseStack, applyLeftHandTransform);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData modelData){
        ModelData[] subModelData = modelData.get(SUB_MODEL_DATA);
        return this.getModels().getFirst().getParticleIcon(subModelData == null ? ModelData.EMPTY : subModelData[0]);
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
    public List<RenderType> getRenderTypes(ItemStack itemStack){
        return this.getModels().getFirst().getRenderTypes(itemStack);
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack){
        return this.getModels().getFirst().getRenderPasses(stack);
    }
}
