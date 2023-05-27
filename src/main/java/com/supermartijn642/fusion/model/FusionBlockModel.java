package com.supermartijn642.fusion.model;

import com.supermartijn642.fusion.api.model.*;
import com.supermartijn642.fusion.extensions.BlockModelExtension;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.IModelState;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class FusionBlockModel extends ModelBlock implements IModel {

    private final ModelInstance<?> model;
    private final ModelBlock vanillaModel;
    private Collection<ResourceLocation> dependencies;

    public FusionBlockModel(ModelInstance<?> model){
        super(null, Collections.emptyList(), Collections.emptyMap(), false, false, ItemCameraTransforms.DEFAULT, Collections.emptyList());
        this.model = model;
        this.vanillaModel = model.getAsVanillaModel();
    }

    @Override
    public IBakedModel bake(IModelState modelTransform, VertexFormat format, Function<ResourceLocation,TextureAtlasSprite> spriteGetter){
        // Let the custom model handle the actual baking
        ModelBakingContext context = new ModelBakingContextImpl(spriteGetter, modelTransform, new ResourceLocation(this.name));
        return this.model.bake(context);
    }

    @Override
    public Collection<ResourceLocation> getDependencies(){
        if(this.dependencies != null)
            return this.dependencies;
        try{
            this.dependencies = this.model.getModelDependencies();
        }catch(Exception e){
            throw new RuntimeException("Encountered an exception whilst requesting dependencies from model type '" + ModelTypeRegistryImpl.getIdentifier(this.model.getModelType()) + "' for  '" + this.name + "'!", e);
        }
        if(this.dependencies == null)
            throw new RuntimeException("Model type '" + ModelTypeRegistryImpl.getIdentifier(this.model.getModelType()) + "' returned null when requesting dependencies '" + this.name + "'!");
        return this.dependencies;
    }

    @Override
    public Collection<ResourceLocation> getTextures(){
        GatherTexturesContext context = FusionBlockModel::getModelInstance;
        Collection<ResourceLocation> materials = null;
        try{
            Collection<SpriteIdentifier> pairs = this.model.getTextureDependencies(context);
            if(pairs != null)
                materials = pairs.stream().map(SpriteIdentifier::getTexture).collect(Collectors.toSet());
        }catch(Exception e){
            throw new RuntimeException("Encountered an exception whilst requesting texture dependencies from model type '" + ModelTypeRegistryImpl.getIdentifier(this.model.getModelType()) + "' for '" + this.name + "'!", e);
        }
        if(materials == null)
            throw new RuntimeException("Model type '" + ModelTypeRegistryImpl.getIdentifier(this.model.getModelType()) + "' returned null when requesting texture dependencies for '" + this.name + "'!");
        return materials;
    }

    @Override
    public Optional<ModelBlock> asVanillaModel(){
        return Optional.ofNullable(this.vanillaModel);
    }

    public static ModelInstance<?> getModelInstance(ResourceLocation location){
        IModel model = ModelLoaderRegistry.getModelOrMissing(location);
        if(model instanceof FusionBlockModel)
            return ((FusionBlockModel)model).model;
        Optional<ModelBlock> optional = model.asVanillaModel();
        if(optional.isPresent()){
            ModelBlock vanillaModel = optional.get();
            ModelInstance<?> modelInstance = ((BlockModelExtension)vanillaModel).getFusionModel();
            if(modelInstance == null){
                modelInstance = new ModelInstanceImpl<>(DefaultModelTypes.VANILLA, vanillaModel);
                ((BlockModelExtension)vanillaModel).setFusionModel(modelInstance);
            }
            return modelInstance;
        }
        return new ModelInstanceImpl<>(DefaultModelTypes.UNKNOWN, model);
    }
}
