package com.supermartijn642.fusion.model.modifiers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Created 19/09/2024 by SuperMartijn642
 */
public class BlockModelModifierBakedModel implements BakedModel {

    private static final ModelProperty<IModelData[]> DATA_PROPERTY = new ModelProperty<>();

    private final BakedModel original;
    private final List<BakedModel> models;
    private final boolean hasNonSimpleModels;
    private final List<BakedModel> nonSimpleModels;
    private final List<BakedQuad> quads;
    @SuppressWarnings("unchecked")
    private final List<BakedQuad>[] culledQuads = new List[6];
    private final boolean hasLayeredModels;

    public BlockModelModifierBakedModel(BakedModel original, List<BakedModel> models){
        this.original = original;
        this.models = new ArrayList<>(models.size() + 1);
        this.models.add(original);
        this.models.addAll(models);
        List<BakedModel> nonSimpleModels = new ArrayList<>();
        List<BakedQuad> quads = new ArrayList<>();
        //noinspection unchecked
        List<BakedQuad>[] culledQuads = IntStream.range(0, 6).mapToObj(i -> new ArrayList<>()).toArray(List[]::new);
        boolean hasLayeredModels = false;
        Random random = new Random();
        for(BakedModel model : this.models){
            if(!model.getClass().equals(SimpleBakedModel.class)){
                nonSimpleModels.add(model);
                if(model.isLayered())
                    hasLayeredModels = true;
            }else{
                //noinspection deprecation
                quads.addAll(model.getQuads(null, null, random));
                for(Direction side : Direction.values())
                    //noinspection deprecation
                    culledQuads[side.ordinal()].addAll(model.getQuads(null, side, random));
            }
        }
        this.hasNonSimpleModels = !nonSimpleModels.isEmpty();
        this.nonSimpleModels = nonSimpleModels.isEmpty() ? null : List.copyOf(nonSimpleModels);
        this.quads = List.copyOf(quads);
        for(Direction side : Direction.values())
            this.culledQuads[side.ordinal()] = List.copyOf(culledQuads[side.ordinal()]);
        this.hasLayeredModels = hasLayeredModels;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull Random random, @NotNull IModelData data){
        if(!this.hasNonSimpleModels)
            return side == null ? this.quads : this.culledQuads[side.ordinal()];
        IModelData[] arr = data.getData(DATA_PROPERTY);
        List<BakedQuad> quads = new ArrayList<>(side == null ? this.quads : this.culledQuads[side.ordinal()]);
        for(int i = 0; i < this.nonSimpleModels.size(); i++)
            quads.addAll(this.nonSimpleModels.get(i).getQuads(state, side, random, arr == null || arr[i] == null ? EmptyModelData.INSTANCE : arr[i]));
        return quads;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random random){
        if(!this.hasNonSimpleModels)
            return side == null ? this.quads : this.culledQuads[side.ordinal()];
        List<BakedQuad> quads = new ArrayList<>(side == null ? this.quads : this.culledQuads[side.ordinal()]);
        for(BakedModel model : this.nonSimpleModels)
            quads.addAll(model.getQuads(state, side, random));
        return quads;
    }

    @Override
    public boolean isLayered(){
        return this.hasLayeredModels;
    }

    @Override
    public List<Pair<BakedModel,RenderType>> getLayerModels(ItemStack stack, boolean fabulous){
        List<Pair<BakedModel,RenderType>> layers = new ArrayList<>(this.models.size());
        for(BakedModel model : this.models)
            layers.addAll(model.getLayerModels(stack, fabulous));
        return layers;
    }

    @Override
    public IModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, IModelData data){
        if(!this.hasNonSimpleModels)
            return data;
        IModelData[] arr = new IModelData[this.nonSimpleModels.size()];
        for(int i = 0; i < this.nonSimpleModels.size(); i++)
            arr[i] = this.nonSimpleModels.get(i).getModelData(level, pos, state, data);
        return new ModelDataMap.Builder().withInitial(DATA_PROPERTY, arr).build();
    }

    @Override
    public ItemTransforms getTransforms(){
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
    public ItemOverrides getOverrides(){
        return this.original.getOverrides();
    }

    @Override
    public boolean useAmbientOcclusion(BlockState state){
        return this.original.useAmbientOcclusion(state);
    }

    @Override
    public boolean doesHandlePerspectives(){
        return this.original.doesHandlePerspectives();
    }

    @Override
    public BakedModel handlePerspective(ItemTransforms.TransformType transformType, PoseStack poseStack){
        return this.original.handlePerspective(transformType, poseStack);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(@NotNull IModelData data){
        return this.original.getParticleIcon(data);
    }
}
