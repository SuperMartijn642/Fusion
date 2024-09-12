package com.supermartijn642.fusion.model.types.base;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.model.ItemBakedModel;
import com.supermartijn642.fusion.model.MutableQuad;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.Atlases;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseBakedModel implements IBakedModel {

    private final List<BakedQuad>[] completeBlockMesh;
    private final List<BakedQuad> completeItemMesh;
    private final Map<RenderType,List<BakedQuad>[]> blockMesh;
    private final Map<RenderType,List<BakedQuad>> itemMesh;
    private final List<RenderType> blockRenderTypes;
    private final List<RenderType> itemRenderTypes, itemRenderTypesFabulous;
    private final boolean shouldCheckOriginalItemRenderTypes, shouldCheckOriginalBlockRenderTypes;
    private final ItemBakedModel itemModel;
    private final List<Pair<IBakedModel,RenderType>> itemPasses, itemPassesFabulous;
    private final boolean hasAmbientOcclusion;
    private final boolean isGui3d;
    private final boolean usesBlockLight;
    private final TextureAtlasSprite particleIcon;
    private final ItemCameraTransforms transforms;
    private final ItemOverrideList overrides;

    public BaseBakedModel(List<BaseModelQuad> quads, boolean hasAmbientOcclusion, boolean isGui3d, boolean usesBlockLight, TextureAtlasSprite particleIcon, ItemCameraTransforms transforms, ItemOverrideList overrides){
        this.hasAmbientOcclusion = hasAmbientOcclusion;
        this.isGui3d = isGui3d;
        this.usesBlockLight = usesBlockLight;
        this.particleIcon = particleIcon;
        this.transforms = transforms;
        this.overrides = overrides;

        // Create block and item meshes from the quads
        Map<RenderType,List<BakedQuad>[]> blockMesh = new HashMap<>();
        Set<RenderType> blockRenderTypes = new HashSet<>();
        Map<RenderType,List<BakedQuad>> itemMesh = new HashMap<>();
        Set<RenderType> itemRenderTypes = new HashSet<>(), itemRenderTypesFabulous = new HashSet<>();
        MutableQuad mutableQuad = new MutableQuad();
        for(BaseModelQuad quad : quads){
            mutableQuad.fillFromBakedQuad(quad.bakedQuad());
            mutableQuad.emissive(quad.emissive());
            if(quad.lightEmission() != null){
                for(int i = 0; i < 4; i++){
                    int sky = Math.max(quad.lightEmission(), LightTexture.sky(mutableQuad.lightmap(i)));
                    int block = Math.max(quad.lightEmission(), LightTexture.block(mutableQuad.lightmap(i)));
                    mutableQuad.lightmap(i, LightTexture.pack(sky, block));
                }
            }
            BakedQuad finishedQuad = mutableQuad.toBakedQuad();
            // Add the block quads
            RenderType renderType = FusionClient.getRenderTypeMaterial(quad.renderType());
            blockRenderTypes.add(renderType);
            int cullIndex = cullIndex(quad.cullDirection());
            //noinspection unchecked
            List<BakedQuad>[] mesh = blockMesh.computeIfAbsent(renderType, r -> new List[7]);
            if(mesh[cullIndex] == null)
                mesh[cullIndex] = new ArrayList<>();
            mesh[cullIndex].add(mutableQuad.toBakedQuad());
            // Add the item quads
            RenderType itemRenderType = renderType == FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER ? FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER
                : renderType == RenderType.translucent() ? Atlases.translucentItemSheet() : Atlases.cutoutBlockSheet();
            itemRenderTypes.add(itemRenderType);
            List<BakedQuad> itemQuads = itemMesh.get(renderType);
            if(itemQuads == null){
                itemQuads = new ArrayList<>();
                itemMesh.put(renderType, itemQuads);
                RenderType fabulousRenderType = renderType == FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER ? FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER
                    : renderType == RenderType.translucent() ? Atlases.translucentCullBlockSheet() : Atlases.cutoutBlockSheet();
                itemRenderTypesFabulous.add(fabulousRenderType);
                itemMesh.put(fabulousRenderType, itemQuads);
            }
            itemQuads.add(finishedQuad);
        }
        this.blockMesh = ImmutableMap.copyOf(blockMesh);
        this.blockRenderTypes = blockRenderTypes.stream().filter(r -> r != FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER).collect(Collectors.toList());
        this.shouldCheckOriginalBlockRenderTypes = blockRenderTypes.contains(FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER);
        this.itemMesh = ImmutableMap.copyOf(itemMesh);
        this.itemRenderTypes = itemRenderTypes.stream().filter(r -> r != FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER).collect(Collectors.toList());
        this.itemRenderTypesFabulous = itemRenderTypesFabulous.stream().filter(r -> r != FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER).collect(Collectors.toList());
        this.shouldCheckOriginalItemRenderTypes = itemRenderTypes.contains(FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER);

        //noinspection unchecked
        this.completeBlockMesh = new List[7];
        for(int i = 0; i < 7; i++){
            int cullIndex = i;
            this.completeBlockMesh[i] = this.blockMesh.values().stream().map(arr -> arr[cullIndex]).filter(Objects::nonNull).flatMap(List::stream).collect(Collectors.toList());
        }
        this.completeItemMesh = this.itemRenderTypes.stream().map(this.itemMesh::get).flatMap(List::stream).collect(Collectors.toList());

        // Create a model to return the item quads
        this.itemModel = new ItemBakedModel(this) {
            @Override
            protected List<BakedQuad> getQuads(ItemStack stack, boolean fabulous, @Nonnull Random random, @Nonnull IModelData data, @Nullable RenderType renderType){
                if(renderType == null)
                    return BaseBakedModel.this.completeItemMesh;

                List<BakedQuad> quads = BaseBakedModel.this.itemMesh.get(renderType);
                if(BaseBakedModel.this.shouldCheckOriginalItemRenderTypes && RenderTypeLookup.getRenderType(stack, fabulous) == renderType){
                    List<BakedQuad> additionalQuads = BaseBakedModel.this.itemMesh.get(FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER);
                    if(additionalQuads != null){
                        if(quads == null)
                            quads = additionalQuads;
                        quads = Stream.concat(quads.stream(), additionalQuads.stream()).collect(Collectors.toList());
                    }
                }
                return quads == null ? Collections.emptyList() : quads;
            }
        };
        this.itemPasses = this.itemRenderTypes.stream().map(r -> Pair.of((IBakedModel)this.itemModel, r)).collect(Collectors.toList());
        this.itemPassesFabulous = this.itemRenderTypesFabulous.stream().map(r -> Pair.of((IBakedModel)this.itemModel, r)).collect(Collectors.toList());
    }

    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, Random random, IModelData data, @Nullable RenderType renderType){
        if(renderType == null)
            return this.completeBlockMesh[cullIndex(cullDirection)];

        List<BakedQuad>[] mesh = this.blockMesh.get(renderType);
        List<BakedQuad> quads = mesh == null ? null : mesh[cullIndex(cullDirection)];
        //noinspection deprecation
        if(this.shouldCheckOriginalBlockRenderTypes && state != null && RenderTypeLookup.getChunkRenderType(state) == renderType){
            mesh = this.blockMesh.get(FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER);
            List<BakedQuad> additionalQuads = mesh == null ? null : mesh[cullIndex(cullDirection)];
            if(additionalQuads != null){
                if(quads == null)
                    quads = additionalQuads;
                quads = Stream.concat(quads.stream(), additionalQuads.stream()).collect(Collectors.toList());
            }
        }
        return quads == null ? Collections.emptyList() : quads;
    }

    @Nonnull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, @Nonnull Random random, @Nonnull IModelData data){
        return this.getQuads(state, cullDirection, random, data, MinecraftForgeClient.getRenderLayer());
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, Random random){
        return this.getQuads(state, cullDirection, random, EmptyModelData.INSTANCE, MinecraftForgeClient.getRenderLayer());
    }

    public List<RenderType> getBlockRenderTypes(){
        return this.blockRenderTypes;
    }

    @Override
    public List<Pair<IBakedModel,RenderType>> getLayerModels(ItemStack stack, boolean fabulous){
        if(this.shouldCheckOriginalItemRenderTypes){
            // There's no way to know the render types beforehand through Forge's API, so just merge them here with the fixed render types
            RenderType renderType = RenderTypeLookup.getRenderType(stack, fabulous);
            if(!(fabulous ? this.itemRenderTypes : this.itemRenderTypesFabulous).contains(renderType)){
                ArrayList<Pair<IBakedModel,RenderType>> combined = new ArrayList<>((fabulous ? this.itemPasses : this.itemPassesFabulous).size() + 1);
                combined.addAll(fabulous ? this.itemPasses : this.itemPassesFabulous);
                combined.add(Pair.of(this.itemModel, renderType));
                return combined;
            }
        }
        return fabulous ? this.itemPasses : this.itemPassesFabulous;
    }

    @Override
    public boolean isLayered(){
        return true;
    }

    @Override
    public boolean useAmbientOcclusion(){
        return this.hasAmbientOcclusion;
    }

    @Override
    public boolean isGui3d(){
        return this.isGui3d;
    }

    @Override
    public boolean usesBlockLight(){
        return this.usesBlockLight;
    }

    @Override
    public boolean isCustomRenderer(){
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon(){
        return this.particleIcon;
    }

    @Override
    public ItemCameraTransforms getTransforms(){
        return this.transforms;
    }

    @Override
    public ItemOverrideList getOverrides(){
        return this.overrides;
    }

    private static int cullIndex(Direction cullDirection){
        return cullDirection == null ? 0 : cullDirection.ordinal() + 1;
    }
}
