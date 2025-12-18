package com.supermartijn642.fusion.model.modifiers.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.model.types.base.CustomRenderTypeBakedModel;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IEnviromentBlockReader;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.*;
import java.util.stream.IntStream;

/**
 * Created 19/09/2024 by SuperMartijn642
 */
public class BlockModelModifierBakedModel implements IBakedModel, CustomRenderTypeBakedModel {

    private static final ModelProperty<IModelData[]> DATA_PROPERTY = new ModelProperty<>();

    private final IBakedModel original;
    private final List<IBakedModel> models;
    private final boolean showBreakingOverlay;
    private final boolean hasNonSimpleModels;
    private final List<IBakedModel> nonSimpleModels;
    private final List<BakedQuad> quads;
    @SuppressWarnings("unchecked")
    private final List<BakedQuad>[] culledQuads = new List[6];
    private final Set<BlockRenderLayer> customBlockRenderTypes;

    public BlockModelModifierBakedModel(IBakedModel original, List<IBakedModel> models, boolean showBreakingOverlay){
        this.original = original;
        this.models = new ArrayList<>(models.size() + 1);
        this.models.add(original);
        this.models.addAll(models);
        this.showBreakingOverlay = showBreakingOverlay;
        List<IBakedModel> nonSimpleModels = new ArrayList<>();
        List<BakedQuad> quads = new ArrayList<>();
        //noinspection unchecked
        List<BakedQuad>[] culledQuads = IntStream.range(0, 6).mapToObj(i -> new ArrayList<>()).toArray(List[]::new);
        Set<BlockRenderLayer> customBlockRenderTypes = new HashSet<>();
        Random random = new Random();
        for(IBakedModel model : this.models){
            if(!model.getClass().equals(SimpleBakedModel.class)){
                nonSimpleModels.add(model);
                if(model instanceof CustomRenderTypeBakedModel)
                    customBlockRenderTypes.addAll(((CustomRenderTypeBakedModel)model).getBlockRenderTypes());
            }else{
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
        this.customBlockRenderTypes = ImmutableSet.copyOf(customBlockRenderTypes);
    }

    @Override
    public @Nonnull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull Random random, @Nonnull IModelData data){
        if(!this.showBreakingOverlay && FusionClient.IS_RENDERING_BREAKING_OVERLAY.get() != null)
            return this.original.getQuads(state, side, random, data);
        BlockRenderLayer renderType = MinecraftForgeClient.getRenderLayer();
        boolean addSimpleQuads = renderType == null || state == null || state.getBlock().canRenderInLayer(state, renderType);
        if(!this.hasNonSimpleModels)
            return addSimpleQuads ? side == null ? this.quads : this.culledQuads[side.ordinal()] : Collections.emptyList();
        IModelData[] arr = data.getData(DATA_PROPERTY);
        List<BakedQuad> quads = addSimpleQuads ? new ArrayList<>(side == null ? this.quads : this.culledQuads[side.ordinal()]) : new ArrayList<>();
        for(int i = 0; i < this.nonSimpleModels.size(); i++)
            quads.addAll(this.nonSimpleModels.get(i).getQuads(state, side, random, arr == null || arr[i] == null ? EmptyModelData.INSTANCE : arr[i]));
        return quads;
    }

    @Override
    public @Nonnull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random random){
        if(!this.showBreakingOverlay && FusionClient.IS_RENDERING_BREAKING_OVERLAY.get() != null)
            return this.original.getQuads(state, side, random);
        BlockRenderLayer renderType = MinecraftForgeClient.getRenderLayer();
        boolean addSimpleQuads = renderType == null || state == null || state.getBlock().canRenderInLayer(state, renderType);
        if(!this.hasNonSimpleModels)
            return addSimpleQuads ? side == null ? this.quads : this.culledQuads[side.ordinal()] : Collections.emptyList();
        List<BakedQuad> quads = addSimpleQuads ? new ArrayList<>(side == null ? this.quads : this.culledQuads[side.ordinal()]) : new ArrayList<>();
        for(IBakedModel model : this.nonSimpleModels)
            quads.addAll(model.getQuads(state, side, random));
        return quads;
    }

    @Override
    public Collection<BlockRenderLayer> getBlockRenderTypes(){
        if(!this.showBreakingOverlay && FusionClient.IS_RENDERING_BREAKING_OVERLAY.get() != null)
            return this.original instanceof CustomRenderTypeBakedModel ? ((CustomRenderTypeBakedModel)this.original).getBlockRenderTypes() : Collections.emptyList();
        return this.customBlockRenderTypes;
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
