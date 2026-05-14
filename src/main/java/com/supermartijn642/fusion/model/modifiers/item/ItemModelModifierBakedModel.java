package com.supermartijn642.fusion.model.modifiers.item;

import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.api.util.Pair;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.List;

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
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing cullDirection, long seed){
        return this.defaultModel.getQuads(state, cullDirection, seed);
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms(){
        return this.defaultModel.getItemCameraTransforms();
    }

    @Override
    public boolean isAmbientOcclusion(IBlockState state){
        return this.defaultModel.isAmbientOcclusion(state);
    }

    @Override
    public org.apache.commons.lang3.tuple.Pair<? extends IBakedModel,Matrix4f> handlePerspective(ItemCameraTransforms.TransformType transformType){
        return this.defaultModel.handlePerspective(transformType);
    }

    @Override
    public boolean isAmbientOcclusion(){
        return this.defaultModel.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d(){
        return this.defaultModel.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer(){
        return this.defaultModel.isBuiltInRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleTexture(){
        return this.defaultModel.getParticleTexture();
    }

    @Override
    public ItemOverrideList getOverrides(){
        return this.defaultModel.getOverrides();
    }
}
