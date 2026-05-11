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
import net.minecraftforge.client.model.data.IModelData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class WrappedBakedModel implements IBakedModel, CustomRenderTypeBakedModel {

    protected final IBakedModel original;

    public WrappedBakedModel(IBakedModel original){
        this.original = original;
    }

    @Override
    public @Nonnull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, @Nonnull Random random, @Nonnull IModelData data){
        return this.original.getQuads(state, cullDirection, random, data);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, Random random){
        return this.original.getQuads(state, cullDirection, random);
    }

    @Override
    public boolean useAmbientOcclusion(){
        return this.original.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d(){
        return this.original.isGui3d();
    }

    @Override
    public boolean usesBlockLight(){
        return this.original.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer(){
        return this.original.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon(){
        return this.original.getParticleIcon();
    }

    @Override
    public ItemCameraTransforms getTransforms(){
        return this.original.getTransforms();
    }

    @Override
    public ItemOverrideList getOverrides(){
        return this.original.getOverrides();
    }

    @Override
    public boolean isAmbientOcclusion(BlockState state){
        return this.original.isAmbientOcclusion(state);
    }

    @Override
    public boolean doesHandlePerspectives(){
        return this.original.doesHandlePerspectives();
    }

    @Override
    public IBakedModel handlePerspective(ItemCameraTransforms.TransformType transformType, MatrixStack poseStack){
        return this.original.handlePerspective(transformType, poseStack);
    }

    @Nonnull
    @Override
    public IModelData getModelData(@Nonnull ILightReader level, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nonnull IModelData data){
        return this.original.getModelData(level, pos, state, data);
    }

    @Override
    public TextureAtlasSprite getParticleTexture(@Nonnull IModelData data){
        return this.original.getParticleTexture(data);
    }

    @Override
    public boolean canRenderInLayer(BlockState state, RenderType layer){
        return ModelRenderTypeHelper.canRenderInLayer(this.original, state, layer, ModelRenderTypeHelper.couldBlockRenderInLayerOriginally(state, layer));
    }
}
