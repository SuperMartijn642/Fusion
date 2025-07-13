package com.supermartijn642.fusion.model;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Random;

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

    protected abstract List<BakedQuad> getQuads(ItemStack stack, boolean fabulous, @NotNull Random random, @NotNull IModelData data, @Nullable RenderType renderType);

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, @NotNull Random random, @NotNull IModelData data){
        if(cullDirection != null)
            return Collections.emptyList();
        return this.getQuads(this.stack, this.fabulous, random, data, MinecraftForgeClient.getRenderType());
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, Random random){
        if(cullDirection != null)
            return Collections.emptyList();
        return this.getQuads(this.stack, this.fabulous, random, EmptyModelData.INSTANCE, MinecraftForgeClient.getRenderType());
    }

    public void set(ItemStack stack, boolean fabulous){
        this.stack = stack;
        this.fabulous = fabulous;
    }

    public List<BakedModel> asList(){
        return this.asList;
    }
}
