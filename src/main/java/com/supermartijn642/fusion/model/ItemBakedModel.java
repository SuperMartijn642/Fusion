package com.supermartijn642.fusion.model;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Created 12/09/2024 by SuperMartijn642
 */
public abstract class ItemBakedModel extends WrappedBakedModel {

    private final List<BakedModel> asList = List.of(this);
    private ItemStack stack;

    public ItemBakedModel(BakedModel original){
        super(original);
    }

    protected abstract List<BakedQuad> getQuads(ItemStack stack, @NotNull RandomSource random, @NotNull ModelData data, @Nullable RenderType renderType);

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, @NotNull RandomSource random, @NotNull ModelData data, @Nullable RenderType renderType){
        return this.getQuads(this.stack, random, data, renderType);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, RandomSource random){
        return this.getQuads(this.stack, random, ModelData.EMPTY, null);
    }

    public void set(ItemStack stack){
        this.stack = stack;
    }

    public List<BakedModel> asList(){
        return this.asList;
    }

    /**
     * Copies the behaviour of {@link net.neoforged.neoforge.client.RenderTypeHelper#getFallbackItemRenderType(ItemStack, BakedModel)}, but ignores the model.
     */
    public static RenderType getNonModelRenderType(ItemStack stack){
        if(stack.getItem() instanceof BlockItem blockItem){
            //noinspection deprecation
            var renderTypes = ItemBlockRenderTypes.getRenderLayers(blockItem.getBlock().defaultBlockState());
            if(renderTypes.contains(RenderType.translucent()))
                return Sheets.translucentItemSheet();
            return Sheets.cutoutBlockSheet();
        }
        return Sheets.translucentItemSheet();
    }
}
