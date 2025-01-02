package com.supermartijn642.fusion.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
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
import net.neoforged.neoforge.common.util.TriState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class WrappedBakedModel implements BakedModel {

    protected final BakedModel original;

    public WrappedBakedModel(BakedModel original){
        this.original = original;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, @NotNull RandomSource random, @NotNull ModelData data, @Nullable RenderType renderType){
        return this.original.getQuads(state, cullDirection, random, data, renderType);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, RandomSource random){
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
    public TextureAtlasSprite getParticleIcon(){
        return this.original.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms(){
        return this.original.getTransforms();
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data){
        return this.original.getRenderTypes(state, rand, data);
    }

    @Override
    public RenderType getRenderType(ItemStack itemStack){
        return this.original.getRenderType(itemStack);
    }

    @Override
    public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType){
        return this.original.useAmbientOcclusion(state, data, renderType);
    }

    @Override
    public void applyTransform(ItemDisplayContext transformType, PoseStack poseStack, boolean applyLeftHandTransform){
        this.original.applyTransform(transformType, poseStack, applyLeftHandTransform);
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data){
        return this.original.getModelData(level, pos, state, data);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data){
        return this.original.getParticleIcon(data);
    }

    @SuppressWarnings("removal")
    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack){
        return this.original.getRenderPasses(stack);
    }
}
