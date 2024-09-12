package com.supermartijn642.fusion.model;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Created 12/09/2024 by SuperMartijn642
 */
public abstract class ItemBakedModel extends WrappedBakedModel {

    private final List<IBakedModel> asList = Collections.singletonList(this);
    private ItemStack stack;

    public ItemBakedModel(IBakedModel original){
        super(original);
    }

    protected abstract List<BakedQuad> getQuads(ItemStack stack, @Nonnull Random random, @Nonnull IModelData data, @Nonnull RenderType renderType);

    @Override
    public @Nonnull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, @Nonnull Random random, @Nonnull IModelData data){
        return this.getQuads(this.stack, random, data, MinecraftForgeClient.getRenderLayer());
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, Random random){
        return this.getQuads(this.stack, random, EmptyModelData.INSTANCE, MinecraftForgeClient.getRenderLayer());
    }

    public void set(ItemStack stack){
        this.stack = stack;
    }

    public List<IBakedModel> asList(){
        return this.asList;
    }
}
