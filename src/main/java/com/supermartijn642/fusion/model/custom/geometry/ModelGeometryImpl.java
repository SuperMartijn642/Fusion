package com.supermartijn642.fusion.model.custom.geometry;

import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.PropertyGetter;
import com.supermartijn642.fusion.model.ModelRenderTypeHelper;
import com.supermartijn642.fusion.util.CullingHelper;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.IModel;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created 03/05/2026 by SuperMartijn642
 */
public class ModelGeometryImpl implements ModelGeometry {

    public static ModelGeometry of(IModel model){
        ModelBlock vanillaModel = model.asVanillaModel().orElse(null);
        if(vanillaModel != null && vanillaModel.elements != null && !vanillaModel.elements.isEmpty())
            return CuboidModelGeometry.of((ModelBlock)model);
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
            if(IdentifierUtil.isValidIdentifier(key)){
                ModelMaterial material = ModelMaterial.of(new ResourceLocation(key));
                TextureAtlasSprite sprite = materialResolver.apply(material);
                if(material.isMissing() || !ModelMaterial.isMissingSprite(sprite)){
                    resolvedMaterials.put(key, sprite);
                    return sprite;
                }
            }
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

    private final IModel model;

    ModelGeometryImpl(IModel model){
        this.model = model;
    }

    @Override
    public Collection<Either<String,ModelMaterial>> getRequiredMaterials(){
        return Collections.emptyList();
    }

    @Override
    public void bake(QuadConsumer consumer, ModelTransform transformation, MaterialResolver materialResolver){
        // Bake the model
        Function<ResourceLocation,TextureAtlasSprite> spriteGetter = material -> materialResolver.get(material.toString());
        IBakedModel baked;
        try{
            baked = this.model.bake(transformation.toModelState(), DefaultVertexFormats.ITEM, spriteGetter);
        }catch(Exception e){
            throw new RuntimeException("Encountered an exception baking model of class '" + this.model.getClass().getName() + "'!", e);
        }

        // Collect all quads from the model
        for(BlockRenderLayer renderType : BlockRenderLayer.values()){
            if(!ModelRenderTypeHelper.canRenderInLayer(baked, Blocks.AIR.getDefaultState(), renderType, renderType == BlockRenderLayer.SOLID))
                continue;
            ForgeHooksClient.setRenderLayer(renderType);
            for(EnumFacing cullDirection : CullingHelper.cullDirections()){
                baked.getQuads(Blocks.AIR.getDefaultState(), cullDirection, 42).forEach(q -> {
                    MutableQuad mutableQuad = MutableQuad.create(q);
                    mutableQuad.renderLayer(renderType);
                    consumer.consume(mutableQuad, cullDirection, PropertyGetter.empty());
                });
            }
        }
        ForgeHooksClient.setRenderLayer(null);
    }
}
