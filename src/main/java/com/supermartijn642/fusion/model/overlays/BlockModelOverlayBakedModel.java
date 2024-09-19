package com.supermartijn642.fusion.model.overlays;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IEnviromentBlockReader;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Created 19/09/2024 by SuperMartijn642
 */
public class BlockModelOverlayBakedModel implements IBakedModel {

    private static final ModelProperty<IModelData[]> DATA_PROPERTY = new ModelProperty<>();

    private final IBakedModel original;
    private final List<IBakedModel> models;
    private final boolean hasNonSimpleModels;
    private final List<IBakedModel> nonSimpleModels;
    private final List<BakedQuad> quads;
    @SuppressWarnings("unchecked")
    private final List<BakedQuad>[] culledQuads = new List[6];

    public BlockModelOverlayBakedModel(IBakedModel original, List<IBakedModel> models){
        this.original = original;
        this.models = new ArrayList<>(models.size() + 1);
        this.models.add(original);
        this.models.addAll(models);
        List<IBakedModel> nonSimpleModels = new ArrayList<>();
        List<BakedQuad> quads = new ArrayList<>();
        //noinspection unchecked
        List<BakedQuad>[] culledQuads = IntStream.range(0, 6).mapToObj(i -> new ArrayList<>()).toArray(List[]::new);
        Random random = new Random();
        for(IBakedModel model : this.models){
            if(!model.getClass().equals(SimpleBakedModel.class))
                nonSimpleModels.add(model);
            else{
                //noinspection deprecation
                quads.addAll(model.getQuads(null, null, random));
                for(Direction side : Direction.values())
                    //noinspection deprecation
                    culledQuads[side.ordinal()].addAll(model.getQuads(null, side, random));
            }
        }
        this.hasNonSimpleModels = !nonSimpleModels.isEmpty();
        this.nonSimpleModels = nonSimpleModels.isEmpty() ? null : ImmutableList.copyOf(nonSimpleModels);
        this.quads = ImmutableList.copyOf(quads);
        for(Direction side : Direction.values())
            this.culledQuads[side.ordinal()] = ImmutableList.copyOf(culledQuads[side.ordinal()]);
    }

    @Override
    public @Nonnull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull Random random, @Nonnull IModelData data){
        if(!this.hasNonSimpleModels)
            return side == null ? this.quads : this.culledQuads[side.ordinal()];
        IModelData[] arr = data.getData(DATA_PROPERTY);
        List<BakedQuad> quads = new ArrayList<>(side == null ? this.quads : this.culledQuads[side.ordinal()]);
        for(int i = 0; i < this.nonSimpleModels.size(); i++)
            quads.addAll(this.nonSimpleModels.get(i).getQuads(state, side, random, arr == null || arr[i] == null ? EmptyModelData.INSTANCE : arr[i]));
        return quads;
    }

    @Override
    public @Nonnull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random random){
        if(!this.hasNonSimpleModels)
            return side == null ? this.quads : this.culledQuads[side.ordinal()];
        List<BakedQuad> quads = new ArrayList<>(side == null ? this.quads : this.culledQuads[side.ordinal()]);
        for(IBakedModel model : this.nonSimpleModels)
            quads.addAll(model.getQuads(state, side, random));
        return quads;
    }

    @Override
    public IModelData getModelData(IEnviromentBlockReader level, BlockPos pos, BlockState state, IModelData data){
        if(!this.hasNonSimpleModels)
            return data;
        IModelData[] arr = new IModelData[this.nonSimpleModels.size()];
        for(int i = 0; i < this.nonSimpleModels.size(); i++)
            arr[i] = this.nonSimpleModels.get(i).getModelData(level, pos, state, data);
        return new ModelDataMap.Builder().withInitial(DATA_PROPERTY, arr).build();
    }

    @Override
    public ItemCameraTransforms getTransforms(){
        return this.original.getTransforms();
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
    public boolean isCustomRenderer(){
        return this.original.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon(){
        return this.original.getParticleIcon();
    }

    @Override
    public ItemOverrideList getOverrides(){
        return this.original.getOverrides();
    }

    @Override
    public boolean isAmbientOcclusion(BlockState state){
        return this.original.isAmbientOcclusion(state);
    }

    @Override
    public boolean doesHandlePerspectives(){
        return this.original.doesHandlePerspectives();
    }

    @Override
    public Pair<? extends IBakedModel,Matrix4f> handlePerspective(ItemCameraTransforms.TransformType transformType){
        return this.original.handlePerspective(transformType);
    }

    @Override
    public TextureAtlasSprite getParticleTexture(@Nonnull IModelData data){
        return this.original.getParticleTexture(data);
    }
}
