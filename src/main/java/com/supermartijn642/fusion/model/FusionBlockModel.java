package com.supermartijn642.fusion.model;

import com.mojang.datafixers.util.Pair;
import com.supermartijn642.fusion.api.model.*;
import com.supermartijn642.fusion.extensions.BlockModelExtension;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class FusionBlockModel extends BlockModel {

    public static final IUnbakedModel DUMMY_MODEL = new IUnbakedModel() {
        @Override
        public Collection<ResourceLocation> getDependencies(){
            return Collections.emptyList();
        }

        @Override
        public Collection<RenderMaterial> getMaterials(Function<ResourceLocation,IUnbakedModel> function, Set<Pair<String,String>> set){
            return Collections.emptyList();
        }

        @Nullable
        @Override
        public IBakedModel bake(ModelBakery modelBakery, Function<RenderMaterial,TextureAtlasSprite> function, IModelTransform modelState, ResourceLocation resourceLocation){
            return null;
        }
    };

    private final ModelInstance<?> model;
    private final BlockModel vanillaModel;
    private Collection<ResourceLocation> dependencies;

    public FusionBlockModel(ModelInstance<?> model){
        super(null, Collections.emptyList(), Collections.emptyMap(), false, null, ItemCameraTransforms.NO_TRANSFORMS, Collections.emptyList());
        this.model = model;
        this.vanillaModel = model.getAsVanillaModel();
    }

    @Override
    public IBakedModel bake(ModelBakery bakery, BlockModel someOtherModel, Function<RenderMaterial,TextureAtlasSprite> spriteGetter, IModelTransform modelTransform, ResourceLocation modelLocation, boolean gui3d){
        // Let the custom model handle the actual baking
        ModelBakingContext context = new ModelBakingContextImpl(bakery, spriteGetter, modelTransform, modelLocation);
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
    public Collection<RenderMaterial> getMaterials(Function<ResourceLocation,IUnbakedModel> modelGetter, Set<com.mojang.datafixers.util.Pair<String,String>> errors){
        GatherTexturesContext context = identifier -> getModelInstance(modelGetter.apply(identifier));
        Collection<RenderMaterial> materials = null;
        try{
            Collection<SpriteIdentifier> pairs = this.model.getTextureDependencies(context);
            if(pairs != null)
                materials = pairs.stream().map(SpriteIdentifier::toMaterial).collect(Collectors.toSet());
        }catch(Exception e){
            throw new RuntimeException("Encountered an exception whilst requesting texture dependencies from model type '" + ModelTypeRegistryImpl.getIdentifier(this.model.getModelType()) + "' for  '" + this.name + "'!", e);
        }
        if(materials == null)
            throw new RuntimeException("Model type '" + ModelTypeRegistryImpl.getIdentifier(this.model.getModelType()) + "' returned null when requesting texture dependencies for '" + this.name + "'!");

        Collection<ResourceLocation> dependencies = this.getDependencies();
        if(!dependencies.isEmpty()){
            materials = new ArrayList<>(materials);
            dependencies.forEach(location -> {
                IUnbakedModel model = modelGetter.apply(location);
                if(model == null)
                    BlockModel.LOGGER.warn("Could not find dependency model '{}' while loading model '{}'", location, this);
            });
        }
        return materials;
    }

    public boolean hasVanillaModel(){
        return this.vanillaModel != null;
    }

    public BlockModel getVanillaModel(){
        return this.vanillaModel;
    }

    public static ModelInstance<?> getModelInstance(IUnbakedModel model){
        if(model instanceof FusionBlockModel)
            return ((FusionBlockModel)model).model;
        if(model instanceof BlockModel){
            ModelInstance<?> modelInstance = ((BlockModelExtension)model).getFusionModel();
            if(modelInstance == null){
                modelInstance = new ModelInstanceImpl<>(DefaultModelTypes.VANILLA, (BlockModel)model);
                ((BlockModelExtension)model).setFusionModel(modelInstance);
            }
            return modelInstance;
        }
        return new ModelInstanceImpl<>(DefaultModelTypes.UNKNOWN, model);
    }
}
