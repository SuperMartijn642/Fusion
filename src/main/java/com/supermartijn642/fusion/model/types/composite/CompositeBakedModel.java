package com.supermartijn642.fusion.model.types.composite;

import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
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
 * Created 15/06/2026 by SuperMartijn642
 */
public class CompositeBakedModel implements BakedModel, FabricBakedModel {

    private final BakedModel defaultModel;
    private final List<ConditionalList> entries;

    public CompositeBakedModel(BakedModel defaultModel, List<ConditionalList> entries){
        this.defaultModel = defaultModel;
        this.entries = entries;
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter level, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context){
        for(ConditionalList list : this.entries){
            BakedModel model = list.get(level, pos, state);
            if(model != null)
                ((FabricBakedModel)model).emitBlockQuads(level, state, pos, randomSupplier, context);
        }
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context){
        for(ConditionalList list : this.entries){
            BakedModel model = list.get(stack);
            if(model != null)
                ((FabricBakedModel)model).emitItemQuads(stack, randomSupplier, context);
        }
    }

    @Override
    public boolean isVanillaAdapter(){
        return false;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, RandomSource random){
        long seed = random.nextLong();
        List<BakedQuad> quads = new ArrayList<>();
        for(ConditionalList list : this.entries){
            BakedModel model = list.get(null, null, state);
            if(model != null){
                random.setSeed(seed);
                quads.addAll(model.getQuads(state, cullDirection, random));
            }
        }
        return quads;
    }

    @Override
    public TextureAtlasSprite getParticleIcon(){
        return this.defaultModel.getParticleIcon();
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
    public ItemTransforms getTransforms(){
        return this.defaultModel.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides(){
        return this.defaultModel.getOverrides();
    }

    @Override
    public boolean isCustomRenderer(){
        return false;
    }

    public record ConditionalList(List<ModelEntry> entries) {
        @Nullable
        BakedModel get(BlockAndTintGetter level, BlockPos pos, BlockState state){
            for(ModelEntry entry : this.entries){
                if(entry.predicate == null || entry.predicate.testForBlockState(level, pos, state))
                    return entry.model;
            }
            return null;
        }

        @Nullable
        BakedModel get(ItemStack stack){
            for(ModelEntry entry : this.entries){
                if(entry.predicate == null || entry.predicate.testForItem(stack))
                    return entry.model;
            }
            return null;
        }
    }

    public record ModelEntry(BakedModel model, ModelPredicate predicate) {
    }
}
