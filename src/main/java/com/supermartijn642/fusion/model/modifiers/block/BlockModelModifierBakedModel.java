package com.supermartijn642.fusion.model.modifiers.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.supermartijn642.fusion.model.types.base.CustomRenderTypeBakedModel;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
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

    private final IBakedModel original;
    private final List<IBakedModel> models;
    private final boolean hasNonSimpleModels;
    private final List<IBakedModel> nonSimpleModels;
    private final List<BakedQuad> quads;
    @SuppressWarnings("unchecked")
    private final List<BakedQuad>[] culledQuads = new List[6];
    private final Set<BlockRenderLayer> customBlockRenderTypes;

    public BlockModelModifierBakedModel(IBakedModel original, List<IBakedModel> models){
        this.original = original;
        this.models = new ArrayList<>(models.size() + 1);
        this.models.add(original);
        this.models.addAll(models);
        List<IBakedModel> nonSimpleModels = new ArrayList<>();
        List<BakedQuad> quads = new ArrayList<>();
        //noinspection unchecked
        List<BakedQuad>[] culledQuads = IntStream.range(0, 6).mapToObj(i -> new ArrayList<>()).toArray(List[]::new);
        Set<BlockRenderLayer> customBlockRenderTypes = new HashSet<>();
        for(IBakedModel model : this.models){
            if(!model.getClass().equals(SimpleBakedModel.class)){
                nonSimpleModels.add(model);
                if(model instanceof CustomRenderTypeBakedModel)
                    customBlockRenderTypes.addAll(((CustomRenderTypeBakedModel)model).getBlockRenderTypes());
            }else{
                //noinspection deprecation
                quads.addAll(model.getQuads(null, null, 42));
                for(EnumFacing side : EnumFacing.values())
                    //noinspection deprecation
                    culledQuads[side.ordinal()].addAll(model.getQuads(null, side, 42));
            }
        }
        this.hasNonSimpleModels = !nonSimpleModels.isEmpty();
        this.nonSimpleModels = nonSimpleModels.isEmpty() ? null : ImmutableList.copyOf(nonSimpleModels);
        this.quads = ImmutableList.copyOf(quads);
        for(EnumFacing side : EnumFacing.values())
            this.culledQuads[side.ordinal()] = ImmutableList.copyOf(culledQuads[side.ordinal()]);
        this.customBlockRenderTypes = ImmutableSet.copyOf(customBlockRenderTypes);
    }

    @Override
    public @Nonnull List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long random){
        if(!this.hasNonSimpleModels)
            return side == null ? this.quads : this.culledQuads[side.ordinal()];
        List<BakedQuad> quads = new ArrayList<>(side == null ? this.quads : this.culledQuads[side.ordinal()]);
        for(IBakedModel nonSimpleModel : this.nonSimpleModels)
            quads.addAll(nonSimpleModel.getQuads(state, side, random));
        return quads;
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms(){
        return this.original.getItemCameraTransforms();
    }

    @Override
    public Collection<BlockRenderLayer> getBlockRenderTypes(){
        return this.customBlockRenderTypes;
    }

    @Override
    public boolean isAmbientOcclusion(IBlockState state){
        return this.original.isAmbientOcclusion(state);
    }

    @Override
    public Pair<? extends IBakedModel,Matrix4f> handlePerspective(ItemCameraTransforms.TransformType transformType){
        return this.original.handlePerspective(transformType);
    }

    @Override
    public boolean isAmbientOcclusion(){
        return this.original.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d(){
        return this.original.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer(){
        return this.original.isBuiltInRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleTexture(){
        return this.original.getParticleTexture();
    }

    @Override
    public ItemOverrideList getOverrides(){
        return this.original.getOverrides();
    }
}
