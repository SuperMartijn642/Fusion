package com.supermartijn642.fusion.model.modifiers.block;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Created 19/09/2024 by SuperMartijn642
 */
public class BlockModelModifierBakedModel implements BakedModel, FabricBakedModel {

    private final BakedModel original;
    private final List<BakedModel> models;

    public BlockModelModifierBakedModel(BakedModel original, List<BakedModel> models){
        this.original = original;
        this.models = List.copyOf(models);
    }

    @Override
    public boolean isVanillaAdapter(){
        return false;
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context){
        ((FabricBakedModel)this.original).emitBlockQuads(blockView, state, pos, randomSupplier, context);
        for(BakedModel model : this.models)
            ((FabricBakedModel)model).emitBlockQuads(blockView, state, pos, randomSupplier, context);
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context){
        ((FabricBakedModel)this.original).emitItemQuads(stack, randomSupplier, context);
        for(BakedModel model : this.models)
            ((FabricBakedModel)model).emitItemQuads(stack, randomSupplier, context);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random random){
        List<BakedQuad> quads = new ArrayList<>(this.original.getQuads(state, side, random));
        for(BakedModel model : this.models)
            quads.addAll(model.getQuads(state, side, random));
        return quads;
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
