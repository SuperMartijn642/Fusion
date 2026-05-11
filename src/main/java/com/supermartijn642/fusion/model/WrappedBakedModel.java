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
import net.minecraftforge.client.model.data.IModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class WrappedBakedModel implements BakedModel, CustomRenderTypeBakedModel {

    protected final BakedModel original;

    public WrappedBakedModel(BakedModel original){
        this.original = original;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, @NotNull Random random, @NotNull IModelData data){
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
    public ItemTransforms getTransforms(){
        return this.original.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides(){
        return this.original.getOverrides();
    }

    @Override
    public boolean useAmbientOcclusion(BlockState state){
        return this.original.useAmbientOcclusion(state);
    }

    @Override
    public boolean doesHandlePerspectives(){
        return this.original.doesHandlePerspectives();
    }

    @Override
    public BakedModel handlePerspective(ItemTransforms.TransformType transformType, PoseStack poseStack){
        return this.original.handlePerspective(transformType, poseStack);
    }

    @NotNull
    @Override
    public IModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull IModelData data){
        return this.original.getModelData(level, pos, state, data);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(@NotNull IModelData data){
        return this.original.getParticleIcon(data);
    }

    @Override
    public boolean isLayered(){
        return this.original.isLayered();
    }

    @Override
    public List<Pair<BakedModel,RenderType>> getLayerModels(ItemStack stack, boolean fabulous){
        return this.original.getLayerModels(stack, fabulous);
    }

    @Override
    public boolean canRenderInLayer(BlockState state, RenderType layer){
        return ModelRenderTypeHelper.canRenderInLayer(this.original, state, layer, ModelRenderTypeHelper.couldBlockRenderInLayerOriginally(state, layer));
    }
}
