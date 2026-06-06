package com.supermartijn642.fusion.model.custom.geometry;

import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.PropertyGetter;
import com.supermartijn642.fusion.model.FusionBlockModelData;
import com.supermartijn642.fusion.util.CullingHelper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.SimpleUnbakedGeometry;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created 03/05/2026 by SuperMartijn642
 */
public class ModelGeometryImpl implements ModelGeometry {

    public static ModelGeometry of(UnbakedGeometry geometry){
        if(geometry instanceof SimpleUnbakedGeometry)
            return CuboidModelGeometry.of((SimpleUnbakedGeometry)geometry);
        return new ModelGeometryImpl(geometry);
    }

    public static TextureSlots createTextureSlots(MaterialResolver materialResolver){
        return new TextureSlots(Map.of()) {
            @Override
            public @Nullable Material getMaterial(String reference){
                TextureAtlasSprite sprite = materialResolver.get(reference, false);
                if(ModelMaterial.isMissingSprite(sprite))
                    return null;
                return new Material(sprite.atlasLocation(), sprite.contents().name());
            }
        };
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

    private final UnbakedGeometry geometry;

    ModelGeometryImpl(UnbakedGeometry geometry){
        this.geometry = geometry;
    }

    @Override
    public void bake(QuadConsumer consumer, ModelTransform transformation, MaterialResolver materialResolver){
        // Create dummy texture slots instance
        TextureSlots textureSlots = createTextureSlots(materialResolver);
        // Create dummy sprite getter
        SpriteGetter spriteGetter = new SpriteGetter() {
            @Override
            public TextureAtlasSprite get(Material material, ModelDebugName name){
                return materialResolver.get(material.texture().toString());
            }

            @Override
            public TextureAtlasSprite reportMissingReference(String reference, ModelDebugName name){
                return materialResolver.get(reference);
            }
        };
        ModelBaker modelBaker = new ModelBaker() {
            @Override
            public ResolvedModel getModel(ResourceLocation location){
                ResolvedModel missingModels = FusionBlockModelData.MISSING_MODEL.get();
                if(missingModels == null)
                    throw new IllegalStateException("Missing model requested during geometry baking while non was set!");
                return missingModels;
            }

            @Override
            public SpriteGetter sprites(){
                return spriteGetter;
            }

            @Override
            public <X> X compute(SharedOperationKey<X> key){
                return key.compute(this);
            }
        };
        // Bake the model
        QuadCollection quadCollection;
        try{
            quadCollection = this.geometry.bake(textureSlots, modelBaker, transformation.toModelState(), () -> "");
        }catch(Exception e){
            throw new RuntimeException("Encountered an exception baking geometry of class '" + this.geometry.getClass().getName() + "'!", e);
        }
        // Emit the quads
        for(Direction cullDirection : CullingHelper.cullDirections()){
            for(BakedQuad quad : quadCollection.getQuads(cullDirection)){
                consumer.consume(MutableQuad.create(quad), cullDirection, PropertyGetter.empty());
            }
        }
    }
}
