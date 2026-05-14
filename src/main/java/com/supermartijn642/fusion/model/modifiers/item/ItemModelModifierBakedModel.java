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
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.TriState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, RandomSource random, ModelData data, @Nullable RenderType renderType){
        return this.defaultModel.getQuads(state, cullDirection, random, data, renderType);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, RandomSource random){
        return this.defaultModel.getQuads(state, cullDirection, random);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data){
        return this.defaultModel.getRenderTypes(state, random, data);
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous){
        return this.defaultModel.getRenderTypes(stack, fabulous);
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous){
        return this.defaultModel.getRenderPasses(stack, fabulous);
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
    public ItemOverrides getOverrides(){
        return this.defaultModel.getOverrides();
    }

    @Override
    public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType){
        return this.defaultModel.useAmbientOcclusion(state, data, renderType);
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext transformType, PoseStack poseStack, boolean applyLeftHandTransform){
        return this.defaultModel.applyTransform(transformType, poseStack, applyLeftHandTransform);
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data){
        return this.defaultModel.getModelData(level, pos, state, data);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data){
        return this.defaultModel.getParticleIcon(data);
    }
}
