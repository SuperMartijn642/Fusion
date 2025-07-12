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
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Created 12/09/2024 by SuperMartijn642
 */
public abstract class ItemBakedModel extends WrappedBakedModel {

    private final List<BakedModel> asList = List.of(this);
    private ItemStack stack;
    private boolean fabulous;

    public ItemBakedModel(BakedModel original){
        super(original);
    }

    protected abstract List<BakedQuad> getQuads(ItemStack stack, boolean fabulous, @NotNull RandomSource random, @NotNull ModelData data, @Nullable RenderType renderType);

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, @NotNull RandomSource random, @NotNull ModelData data, @Nullable RenderType renderType){
        if(cullDirection != null)
            return Collections.emptyList();
        return this.getQuads(this.stack, this.fabulous, random, data, renderType);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, RandomSource random){
        if(cullDirection != null)
            return Collections.emptyList();
        return this.getQuads(this.stack, this.fabulous, random, ModelData.EMPTY, null);
    }

    public void set(ItemStack stack, boolean fabulous){
        this.stack = stack;
        this.fabulous = fabulous;
    }

    public List<BakedModel> asList(){
        return this.asList;
    }

    /**
     * Copies the behaviour of {@link net.neoforged.neoforge.client.RenderTypeHelper#getFallbackItemRenderType(ItemStack, BakedModel, boolean)}, but ignores the model.
     */
    public static RenderType getNonModelRenderType(ItemStack stack, boolean fabulous){
        if(stack.getItem() instanceof BlockItem blockItem){
            //noinspection deprecation
            var renderTypes = ItemBlockRenderTypes.getRenderLayers(blockItem.getBlock().defaultBlockState());
            if(renderTypes.contains(RenderType.translucent()))
                return RenderTypeHelper.getEntityRenderType(RenderType.translucent(), fabulous);
            return Sheets.cutoutBlockSheet();
        }
        return fabulous ? Sheets.translucentCullBlockSheet() : Sheets.translucentItemSheet();
    }
}
