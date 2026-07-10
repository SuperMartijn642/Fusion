package com.supermartijn642.fusion.model;

import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

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

    protected abstract List<BakedModel> getModels();

    @Override
    public boolean isVanillaAdapter(){
        return false;
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter level, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context){
        for(BakedModel model : this.getModels())
            model.emitBlockQuads(level, state, pos, randomSupplier, context);
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context){
        for(BakedModel model : this.getModels())
            model.emitItemQuads(stack, randomSupplier, context);
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
    public ItemOverrides getOverrides(){
        return this.getModels().getFirst().getOverrides();
    }

    @Override
    public boolean isCustomRenderer(){
        return this.getModels().getFirst().isCustomRenderer();
    }
}
