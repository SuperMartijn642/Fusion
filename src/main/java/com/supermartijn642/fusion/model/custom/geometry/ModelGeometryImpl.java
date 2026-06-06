package com.supermartijn642.fusion.model.custom.geometry;

import com.supermartijn642.fusion.Fusion;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.PropertyGetter;
import com.supermartijn642.fusion.model.FusionBlockModelData;
import com.supermartijn642.fusion.model.ModelRenderTypeHelper;
import com.supermartijn642.fusion.util.CullingHelper;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.block.Blocks;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
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
            TextureAtlasSprite missing = materialResolver.apply(ModelMaterial.missingBlockAtlas());
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
    public void bake(QuadConsumer consumer, ModelTransform transformation, MaterialResolver materialResolver){
        // Bake the model
        Function<RenderMaterial,TextureAtlasSprite> spriteGetter = material -> materialResolver.get(material.texture().toString());
        ModelBakery modelBakery = FusionBlockModelData.modelBakery.get();
        IBakedModel baked;
        try{
            ResourceLocation identifier = this.model instanceof BlockModel && !((BlockModel)this.model).name.isEmpty() && IdentifierUtil.isValidIdentifier(((BlockModel)this.model).name) ?
                new ResourceLocation(((BlockModel)this.model).name) :
                Fusion.identifier("unknown_geometry");
            baked = this.model.bake(modelBakery, spriteGetter, transformation.toModelTransform(), identifier);
        }catch(Exception e){
            throw new RuntimeException("Encountered an exception baking model of class '" + this.model.getClass().getName() + "'!", e);
        }
        if(baked == null)
            return;

        // Create dummy random
        Random random = new Random();

        // Collect all quads from the model
        for(RenderType renderType : RenderType.chunkBufferLayers()){
            if(!ModelRenderTypeHelper.canRenderInLayer(baked, Blocks.AIR.defaultBlockState(), renderType, renderType == RenderType.solid()))
                continue;
            ForgeHooksClient.setRenderLayer(renderType);
            for(Direction cullDirection : CullingHelper.cullDirections()){
                baked.getQuads(Blocks.AIR.defaultBlockState(), cullDirection, random, EmptyModelData.INSTANCE).forEach(q -> {
                    MutableQuad mutableQuad = MutableQuad.create(q);
                    mutableQuad.chunkRenderType(renderType);
                    consumer.consume(mutableQuad, cullDirection, PropertyGetter.empty());
                });
            }
        }
        ForgeHooksClient.setRenderLayer(null);
    }
}
