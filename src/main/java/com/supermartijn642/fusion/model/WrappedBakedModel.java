package com.supermartijn642.fusion.model;

import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class WrappedBakedModel implements BakedModel, FabricBakedModel {

    protected final BakedModel original;

    public WrappedBakedModel(BakedModel original){
        this.original = original;
    }

    @Override
    public boolean isVanillaAdapter(){
        return ((FabricBakedModel)this.original).isVanillaAdapter();
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context){
        ((FabricBakedModel)this.original).emitBlockQuads(blockView, state, pos, randomSupplier, context);
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context){
        ((FabricBakedModel)this.original).emitItemQuads(stack, randomSupplier, context);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, Random random){
        return this.original.getQuads(state, direction, random);
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
}
