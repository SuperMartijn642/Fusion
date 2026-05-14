package com.supermartijn642.fusion.model.modifiers.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.api.util.Pair;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class ItemModelModifierBakedModel implements BakedModel {

    private final BakedModel defaultModel;
    private final List<Pair<ItemModelPredicate,BakedModel>> models;

    public ItemModelModifierBakedModel(BakedModel defaultModel, List<Pair<ItemModelPredicate,BakedModel>> models){
        this.defaultModel = defaultModel;
        this.models = models;
    }

    public BakedModel forStack(ItemStack stack){
        for(Pair<ItemModelPredicate,BakedModel> entry : this.models){
            if(entry.left().test(stack))
                return entry.right();
        }
        return this.defaultModel;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, Random random, IModelData data){
        return this.defaultModel.getQuads(state, cullDirection, random, data);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, Random random){
        return this.defaultModel.getQuads(state, cullDirection, random);
    }

    @Override
    public boolean isLayered(){
        return this.defaultModel.isLayered();
    }

    @Override
    public List<com.mojang.datafixers.util.Pair<BakedModel,RenderType>> getLayerModels(ItemStack stack, boolean fabulous){
        return this.defaultModel.getLayerModels(stack, fabulous);
    }

    @Override
    public @Nonnull IModelData getModelData(@Nonnull BlockAndTintGetter level, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nonnull IModelData data){
        return this.defaultModel.getModelData(level, pos, state, data);
    }

    @Override
    public ItemTransforms getTransforms(){
        return this.defaultModel.getTransforms();
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
    public boolean isCustomRenderer(){
        return this.defaultModel.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon(){
        return this.defaultModel.getParticleIcon();
    }

    @Override
    public TextureAtlasSprite getParticleIcon(@Nonnull IModelData data){
        return this.defaultModel.getParticleIcon(data);
    }

    @Override
    public ItemOverrides getOverrides(){
        return this.defaultModel.getOverrides();
    }

    @Override
    public boolean isAmbientOcclusion(BlockState state){
        return this.defaultModel.isAmbientOcclusion(state);
    }

    @Override
    public boolean doesHandlePerspectives(){
        return this.defaultModel.doesHandlePerspectives();
    }

    @Override
    public BakedModel handlePerspective(ItemTransforms.TransformType transformType, PoseStack poseStack){
        return this.defaultModel.handlePerspective(transformType, poseStack);
    }
}
