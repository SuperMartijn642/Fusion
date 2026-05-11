package com.supermartijn642.fusion.model.custom.geometry;

import com.supermartijn642.fusion.api.model.custom.CullableQuads;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.model.FusionBlockModelData;
import com.supermartijn642.fusion.model.ModelRenderTypeHelper;
import net.minecraft.block.Blocks;
import net.minecraft.client.renderer.model.BlockModel;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.data.EmptyModelData;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created 03/05/2026 by SuperMartijn642
 */
public class ModelGeometryImpl implements ModelGeometry {

    public static ModelGeometry of(IUnbakedModel model){
        if(model instanceof BlockModel)
            return CuboidModelGeometry.of((BlockModel)model);
        return new ModelGeometryImpl(model);
    }

    public static MaterialResolver fromKeyLookup(Function<String,Either<String,ModelMaterial>> lookup,
                                                 Function<ModelMaterial,TextureAtlasSprite> materialResolver,
                                                 Consumer<String> reportMissing,
                                                 Consumer<List<String>> reportCircular){
        Map<String,TextureAtlasSprite> resolvedMaterials = new HashMap<>();
        return (key, required) -> {
            // Check if the key has already been resolved
            TextureAtlasSprite resolved = resolvedMaterials.get(key);
            if(resolved != null)
                return resolved;
            // Resolve the key
            List<String> encounteredKeys = new ArrayList<>();
            while(true){
                encounteredKeys.add(key);
                Either<String,ModelMaterial> next = lookup.apply(key);
                if(next == null){
                    if(required)
                        reportMissing.accept(key);
                    break;
                }
                if(next.isRight()){
                    TextureAtlasSprite material = materialResolver.apply(next.right());
                    for(String encounteredKey : encounteredKeys)
                        resolvedMaterials.put(encounteredKey, material);
                    return material;
                }
                key = next.left();
                if(encounteredKeys.contains(key)){
                    encounteredKeys.add(key);
                    reportCircular.accept(Collections.unmodifiableList(encounteredKeys));
                    break;
                }
                TextureAtlasSprite previouslyResolved = resolvedMaterials.get(key);
                if(previouslyResolved != null){
                    for(String encounteredKey : encounteredKeys)
                        resolvedMaterials.put(encounteredKey, previouslyResolved);
                    return previouslyResolved;
                }
            }
            TextureAtlasSprite missing = materialResolver.apply(ModelMaterial.missing());
            for(String encounteredKey : encounteredKeys)
                resolvedMaterials.put(encounteredKey, missing);
            return missing;
        };
    }

    private final IUnbakedModel model;

    ModelGeometryImpl(IUnbakedModel model){
        this.model = model;
    }

    @Override
    public Collection<Either<String,ModelMaterial>> getRequiredMaterials(){
        return Collections.emptyList();
    }

    @Override
    public CullableQuads bake(ModelTransform transformation, MaterialResolver materialResolver){
        // Bake the model
        Function<ResourceLocation,TextureAtlasSprite> spriteGetter = material -> materialResolver.get(material.toString());
        ModelBakery modelBakery = FusionBlockModelData.modelBakery.get();
        IBakedModel baked = this.model.bake(modelBakery, spriteGetter, transformation.toModelTransform(), DefaultVertexFormats.BLOCK_NORMALS);
        if(baked == null)
            return CullableQuads.empty();

        // Create dummy random
        Random random = new Random();

        // Collect all quads from the model
        CullableQuads.Builder quads = CullableQuads.builder();
        for(BlockRenderLayer renderType : BlockRenderLayer.values()){
            if(!ModelRenderTypeHelper.canRenderInLayer(baked, Blocks.AIR.defaultBlockState(), renderType, renderType == BlockRenderLayer.SOLID))
                continue;
            ForgeHooksClient.setRenderLayer(renderType);
            baked.getQuads(Blocks.AIR.defaultBlockState(), null, random, EmptyModelData.INSTANCE).forEach(q -> {
                MutableQuad mutableQuad = MutableQuad.create(q);
                mutableQuad.renderLayer(renderType);
                quads.add(null, mutableQuad);
            });
            for(Direction cullDirection : Direction.values()){
                baked.getQuads(Blocks.AIR.defaultBlockState(), cullDirection, random, EmptyModelData.INSTANCE).forEach(q -> {
                    MutableQuad mutableQuad = MutableQuad.create(q);
                    mutableQuad.renderLayer(renderType);
                    quads.add(cullDirection, mutableQuad);
                });
            }
        }
        ForgeHooksClient.setRenderLayer(null);
        return quads.build();
    }
}
