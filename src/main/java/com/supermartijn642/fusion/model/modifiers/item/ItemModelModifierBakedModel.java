package com.supermartijn642.fusion.model.modifiers.item;

import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.api.util.Pair;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IEnviromentBlockReader;
import net.minecraftforge.client.model.data.IModelData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.List;
import java.util.Random;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class ItemModelModifierBakedModel implements IBakedModel {

    private final IBakedModel defaultModel;
    private final List<Pair<ItemModelPredicate,IBakedModel>> models;

    public ItemModelModifierBakedModel(IBakedModel defaultModel, List<Pair<ItemModelPredicate,IBakedModel>> models){
        this.defaultModel = defaultModel;
        this.models = models;
    }

    public IBakedModel forStack(ItemStack stack){
        for(Pair<ItemModelPredicate,IBakedModel> entry : this.models){
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
    public @Nonnull IModelData getModelData(@Nonnull IEnviromentBlockReader level, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nonnull IModelData data){
        return this.defaultModel.getModelData(level, pos, state, data);
    }

    @Override
    public ItemCameraTransforms getTransforms(){
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
    public boolean isCustomRenderer(){
        return this.defaultModel.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon(){
        return this.defaultModel.getParticleIcon();
    }

    @Override
    public TextureAtlasSprite getParticleTexture(@Nonnull IModelData data){
        return this.defaultModel.getParticleTexture(data);
    }

    @Override
    public ItemOverrideList getOverrides(){
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
    public org.apache.commons.lang3.tuple.Pair<? extends IBakedModel,Matrix4f> handlePerspective(ItemCameraTransforms.TransformType transformType){
        return this.defaultModel.handlePerspective(transformType);
    }
}
