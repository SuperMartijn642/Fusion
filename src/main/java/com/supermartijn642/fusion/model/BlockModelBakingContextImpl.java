package com.supermartijn642.fusion.model;

import com.supermartijn642.fusion.api.model.BlockModelBakingContext;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.ModelMaterial;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.function.Function;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class BlockModelBakingContextImpl implements BlockModelBakingContext {

    private final ModelBaker modelBaker;
    private final Function<Material,Material.Baked> materialBaker;
    private final ModelState modelState;
    private final Identifier modelIdentifier;
    private final Map<Identifier,UnbakedModel> dependencies;
    private final Map<String,Material> topLevelTextureReferences;
    private final boolean topLevelAmbientOcclusion;
    private final boolean topLevelUseBlockLighting;
    private final ItemTransforms topLevelItemTransforms;
    private final UnbakedGeometry topLevelGeometry;

    public BlockModelBakingContextImpl(ModelBaker modelBaker, Function<Material,Material.Baked> materialBaker, ModelState modelState, Identifier modelIdentifier, Map<Identifier,UnbakedModel> dependencies, Map<String,Material> topLevelTextureReferences, boolean topLevelAmbientOcclusion, boolean topLevelUseBlockLighting, ItemTransforms topLevelItemTransforms, UnbakedGeometry topLevelGeometry){
        this.modelBaker = modelBaker;
        this.materialBaker = materialBaker;
        this.modelState = modelState;
        this.modelIdentifier = modelIdentifier;
        this.dependencies = dependencies;
        this.topLevelTextureReferences = topLevelTextureReferences;
        this.topLevelAmbientOcclusion = topLevelAmbientOcclusion;
        this.topLevelUseBlockLighting = topLevelUseBlockLighting;
        this.topLevelItemTransforms = topLevelItemTransforms;
        this.topLevelGeometry = topLevelGeometry;
    }

    @Override
    public ModelBaker getModelBaker(){
        return this.modelBaker;
    }

    @Override
    public Material.Baked getMaterial(ModelMaterial identifier){
        return this.materialBaker.apply(identifier.toMaterial());
    }

    @Override
    public ModelState getTransformation(){
        return this.modelState;
    }

    @Override
    public Identifier getModelIdentifier(){
        return this.modelIdentifier;
    }

    @Override
    public ModelInstance<?> getModel(Identifier identifier){
        if(!this.dependencies.containsKey(identifier))
            throw new IllegalStateException("Requesting model that was not given as a dependency!");
        return FusionBlockModel.getModelInstance(this.dependencies.get(identifier));
    }

    @Override
    public Map<String,Material> getTopLevelTextureReferences(){
        return this.topLevelTextureReferences;
    }

    @Override
    public boolean getTopLevelAmbientOcclusion(){
        return this.topLevelAmbientOcclusion;
    }

    @Override
    public boolean getTopLevelUseBlockLighting(){
        return this.topLevelUseBlockLighting;
    }

    @Override
    public ItemTransforms getTopLevelItemTransforms(){
        return this.topLevelItemTransforms;
    }

    @Override
    public UnbakedGeometry getTopLevelGeometry(){
        return this.topLevelGeometry;
    }
}
