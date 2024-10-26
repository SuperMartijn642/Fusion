package com.supermartijn642.fusion.model;

import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.ModelBakingContext;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.extensions.BlockModelExtension;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.Function;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class FusionBlockModel extends BlockModel {

    public static final UnbakedModel DUMMY_MODEL = new UnbakedModel() {
        @Override
        public void resolveDependencies(Resolver resolver){
        }

        @Override
        public BakedModel bake(ModelBaker modelBaker, Function<Material,TextureAtlasSprite> function, ModelState modelState){
            return null;
        }
    };

    private final ModelInstance<?> model;
    private final BlockModel vanillaModel;
    private Collection<ResourceLocation> dependencies;
    private Map<ResourceLocation,UnbakedModel> resolvedDependencies;

    public FusionBlockModel(ModelInstance<?> model){
        super(null, Collections.emptyList(), Collections.emptyMap(), false, null, ItemTransforms.NO_TRANSFORMS, Collections.emptyList());
        this.model = model;
        this.vanillaModel = model.getAsVanillaModel();
    }

    @Override
    public BakedModel bake(ModelBaker baker, Function<Material,TextureAtlasSprite> spriteGetter, ModelState modelTransform){
        return this.bake(baker, spriteGetter, modelTransform, true);
    }

    @Override
    public BakedModel bake(Function<Material,TextureAtlasSprite> spriteGetter, ModelState modelTransform, boolean gui3d){
        return this.bake(null, spriteGetter, modelTransform, gui3d);
    }

    public BakedModel bake(ModelBaker baker, Function<Material,TextureAtlasSprite> spriteGetter, ModelState modelTransform, boolean gui3d){
        // Let the custom model handle the actual baking
        ModelBakingContext context = new ModelBakingContextImpl(baker, spriteGetter, modelTransform, ResourceLocation.parse(this.name), this.resolvedDependencies);
        return this.model.bake(context);
    }

    @Override
    public void resolveDependencies(Resolver resolver){
        // Get the direct dependencies from the model
        if(this.dependencies == null){
            try{
                this.dependencies = this.model.getModelDependencies();
            }catch(Exception e){
                throw new RuntimeException("Encountered an exception whilst requesting dependencies from model type '" + ModelTypeRegistryImpl.getIdentifier(this.model.getModelType()) + "' for  '" + this.name + "'!", e);
            }
            if(this.dependencies == null)
                throw new RuntimeException("Model type '" + ModelTypeRegistryImpl.getIdentifier(this.model.getModelType()) + "' returned null when requesting dependencies '" + this.name + "'!");
        }
        this.resolvedDependencies = new HashMap<>(this.dependencies.size());
        for(ResourceLocation location : this.dependencies)
            this.resolvedDependencies.put(location, resolver.resolve(location));
        // Recursively gather all dependencies for the model
        Deque<ResourceLocation> unresolved = new LinkedList<>(this.dependencies);
        Resolver trackingResolver = location -> {
            UnbakedModel model = resolver.resolve(location);
            if(!this.resolvedDependencies.containsKey(location)){
                unresolved.add(location);
                this.resolvedDependencies.put(location, resolver.resolve(location));
            }
            return model;
        };
        while(!unresolved.isEmpty()){
            ResourceLocation location = unresolved.removeFirst();
            this.resolvedDependencies.get(location).resolveDependencies(trackingResolver);
        }
        // Always add missing model
        this.resolvedDependencies.put(MissingBlockModel.LOCATION, resolver.resolve(MissingBlockModel.LOCATION));

        // Apply parent for vanilla model
        BlockModel vanillaModel = this.model.getAsVanillaModel();
        if(vanillaModel != null)
            vanillaModel.resolveDependencies(resolver);
    }

    public boolean hasVanillaModel(){
        return this.vanillaModel != null;
    }

    public BlockModel getVanillaModel(){
        return this.vanillaModel;
    }

    public static ModelInstance<?> getModelInstance(UnbakedModel model){
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
