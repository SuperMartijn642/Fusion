package com.supermartijn642.fusion.model.custom.geometry;

import com.supermartijn642.fusion.api.model.custom.CullableQuads;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.model.FusionBlockModelData;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created 03/05/2026 by SuperMartijn642
 */
public class ModelGeometryImpl implements ModelGeometry {

    public static ModelGeometry of(UnbakedGeometry geometry){
        if(geometry instanceof UnbakedCuboidGeometry)
            return CuboidModelGeometry.of((UnbakedCuboidGeometry)geometry);
        return new ModelGeometryImpl(geometry);
    }

    public static TextureSlots createTextureSlots(MaterialResolver materialResolver) {
        return new TextureSlots(Map.of()) {
            @Override
            public @Nullable Material getMaterial(String reference){
                ModelMaterial.Resolved material = materialResolver.get(reference, false);
                if(material.isMissing())
                    return null;
                return new Material(material.sprite().contents().name(), material.forceTranslucent());
            }
        };
    }

    public static MaterialResolver fromKeyLookup(Function<String,Either<String,ModelMaterial>> lookup,
                                                 Function<ModelMaterial,ModelMaterial.Resolved> materialResolver,
                                                 Consumer<String> reportMissing,
                                                 Consumer<List<String>> reportCircular){
        Map<String,ModelMaterial.Resolved> resolvedMaterials = new HashMap<>();
        return (key, required) -> {
            // Check if the key has already been resolved
            ModelMaterial.Resolved resolved = resolvedMaterials.get(key);
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
                    ModelMaterial.Resolved material = materialResolver.apply(next.right());
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
                ModelMaterial.Resolved previouslyResolved = resolvedMaterials.get(key);
                if(previouslyResolved != null){
                    for(String encounteredKey : encounteredKeys)
                        resolvedMaterials.put(encounteredKey, previouslyResolved);
                    return previouslyResolved;
                }
            }
            ModelMaterial.Resolved missing = materialResolver.apply(ModelMaterial.missing());
            for(String encounteredKey : encounteredKeys)
                resolvedMaterials.put(encounteredKey, missing);
            return missing;
        };
    }

    static ModelBaker.Interner DUMMY_INTERNER = new ModelBaker.Interner() {
        @Override
        public Vector3fc vector(Vector3fc vector){
            return vector;
        }

        @Override
        public BakedQuad.MaterialInfo materialInfo(BakedQuad.MaterialInfo material){
            return material;
        }
    };

    private final UnbakedGeometry geometry;

    ModelGeometryImpl(UnbakedGeometry geometry){
        this.geometry = geometry;
    }

    @Override
    public CullableQuads bake(ModelTransform transformation, MaterialResolver materialResolver){
        // Create dummy texture slots instance
        TextureSlots textureSlots = createTextureSlots(materialResolver);
        // Create dummy model baker
        MaterialBaker materialBaker = new MaterialBaker() {
            @Override
            public Material.Baked get(Material material, ModelDebugName name){
                ModelMaterial.Resolved resolved = materialResolver.get(material.sprite().toString());
                return resolved.isMissing() || resolved.forceTranslucent() == material.forceTranslucent() ?
                    resolved.toBakedMaterial() :
                    new Material.Baked(resolved.sprite(), material.forceTranslucent());
            }

            @Override
            public Material.Baked reportMissingReference(String reference, ModelDebugName name){
                return materialResolver.get(reference).toBakedMaterial();
            }
        };
        ModelBaker modelBaker = new ModelBaker() {
            @Override
            public ResolvedModel getModel(Identifier location){
                Pair<BlockStateModelPart,ResolvedModel> missingModels = FusionBlockModelData.MISSING_MODEL.get();
                if(missingModels == null)
                    throw new IllegalStateException("Missing model requested during geometry baking while non was set!");
                return missingModels.right();
            }

            @Override
            public BlockStateModelPart missingBlockModelPart(){
                Pair<BlockStateModelPart,ResolvedModel> missingModels = FusionBlockModelData.MISSING_MODEL.get();
                if(missingModels == null)
                    throw new IllegalStateException("Missing model part requested during geometry baking while non was set!");
                return missingModels.left();
            }

            @Override
            public MaterialBaker materials(){
                return materialBaker;
            }

            @Override
            public Interner interner(){
                return DUMMY_INTERNER;
            }

            @Override
            public <X> X compute(SharedOperationKey<X> key){
                return key.compute(this);
            }
        };
        // Bake the model
        try{
            return CullableQuads.of(this.geometry.bake(textureSlots, modelBaker, transformation.toModelState(), () -> ""));
        }catch(Exception e){
            throw new RuntimeException("Encountered an exception baking geometry of class '" + this.geometry.getClass().getName() + "'!", e);
        }
    }
}
