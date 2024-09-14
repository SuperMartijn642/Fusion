package com.supermartijn642.fusion.model.types.base;

import com.google.common.collect.ImmutableList;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.model.MutableQuad;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.MinecraftForgeClient;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseBakedModel implements IBakedModel {

    private final List<BakedQuad>[] completeBlockMesh;
    private final List<BakedQuad>[][] blockMesh; // indexed by render layer ordinal, cull direction
    private final List<BakedQuad> itemMesh;
    private final List<BlockRenderLayer> blockRenderTypes;
    private final boolean shouldCheckOriginalBlockRenderTypes;
    private final boolean hasAmbientOcclusion;
    private final boolean isGui3d;
    private final TextureAtlasSprite particleIcon;
    private final ItemCameraTransforms transforms;
    private final ItemOverrideList overrides;

    public BaseBakedModel(List<BaseModelQuad> quads, boolean hasAmbientOcclusion, boolean isGui3d, TextureAtlasSprite particleIcon, ItemCameraTransforms transforms, ItemOverrideList overrides){
        this.hasAmbientOcclusion = hasAmbientOcclusion;
        this.isGui3d = isGui3d;
        this.particleIcon = particleIcon;
        this.transforms = transforms;
        this.overrides = overrides;

        // Create block and item meshes from the quads
        // noinspection unchecked
        List<BakedQuad>[][] blockMesh = new List[BlockRenderLayer.values().length + 1][];
        Set<BlockRenderLayer> blockRenderTypes = new HashSet<>();
        List<BakedQuad> itemMesh = new ArrayList<>();
        MutableQuad mutableQuad = new MutableQuad();
        for(BaseModelQuad quad : quads){
            mutableQuad.fillFromBakedQuad(quad.bakedQuad());
            mutableQuad.emissive(quad.emissive());
            if(quad.lightEmission() != null){
                for(int i = 0; i < 4; i++){
                    int sky = Math.max(quad.lightEmission(), mutableQuad.lightmap(i) >> 20 & 0xffff);
                    int block = Math.max(quad.lightEmission(), (mutableQuad.lightmap(i) & 0xffff) >> 4);
                    mutableQuad.lightmap(i, (sky << 20 | block << 4));
                }
            }
            BakedQuad bakedQuad = mutableQuad.toBakedQuad();
            // Add the block quads
            BlockRenderLayer renderType = FusionClient.getRenderTypeMaterial(quad.renderType());
            blockRenderTypes.add(renderType);
            int cullIndex = cullIndex(quad.cullDirection());
            List<BakedQuad>[] mesh = blockMesh[renderType == null ? 0 : renderType.ordinal() + 1];
            if(mesh == null){
                // noinspection unchecked
                mesh = new List[7];
                blockMesh[renderType == null ? 0 : renderType.ordinal() + 1] = mesh;
            }
            if(mesh[cullIndex] == null)
                mesh[cullIndex] = new ArrayList<>();
            mesh[cullIndex].add(mutableQuad.toBakedQuad());
            // Add the item quads
            itemMesh.add(bakedQuad);
        }
        this.blockMesh = blockMesh;
        this.blockRenderTypes = blockRenderTypes.stream().filter(Objects::nonNull).collect(Collectors.toList());
        this.shouldCheckOriginalBlockRenderTypes = blockRenderTypes.contains(null);
        this.itemMesh = ImmutableList.copyOf(itemMesh);

        //noinspection unchecked
        this.completeBlockMesh = new List[7];
        for(int i = 0; i < 7; i++){
            int cullIndex = i;
            this.completeBlockMesh[i] = Arrays.stream(this.blockMesh).filter(Objects::nonNull).map(arr -> arr[cullIndex]).filter(Objects::nonNull).flatMap(List::stream).collect(Collectors.toList());
        }
    }

    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing cullDirection, long seed, @Nullable BlockRenderLayer renderType){
        // If the block state is null, assume this call is intended for item rendering
        if(state == null)
            return cullDirection == null ? this.itemMesh : Collections.emptyList();
        // If render type is not set, just return all block quads
        if(renderType == null)
            return this.completeBlockMesh[cullIndex(cullDirection)];

        List<BakedQuad>[] mesh = this.blockMesh[renderType.ordinal() + 1];
        List<BakedQuad> quads = mesh == null ? null : mesh[cullIndex(cullDirection)];
        if(this.shouldCheckOriginalBlockRenderTypes && state.getBlock().getBlockLayer() == renderType){
            mesh = this.blockMesh[0];
            List<BakedQuad> additionalQuads = mesh == null ? null : mesh[cullIndex(cullDirection)];
            if(additionalQuads != null){
                if(quads == null)
                    quads = additionalQuads;
                quads = Stream.concat(quads.stream(), additionalQuads.stream()).collect(Collectors.toList());
            }
        }
        return quads == null ? Collections.emptyList() : quads;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing cullDirection, long seed){
        return this.getQuads(state, cullDirection, seed, MinecraftForgeClient.getRenderLayer());
    }

    public List<BlockRenderLayer> getBlockRenderTypes(){
        return this.blockRenderTypes;
    }

    @Override
    public boolean isAmbientOcclusion(){
        return this.hasAmbientOcclusion;
    }

    @Override
    public boolean isGui3d(){
        return this.isGui3d;
    }

    @Override
    public boolean isBuiltInRenderer(){
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture(){
        return this.particleIcon;
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms(){
        return this.transforms;
    }

    @Override
    public ItemOverrideList getOverrides(){
        return this.overrides;
    }

    private static int cullIndex(EnumFacing cullDirection){
        return cullDirection == null ? 0 : cullDirection.ordinal() + 1;
    }
}
