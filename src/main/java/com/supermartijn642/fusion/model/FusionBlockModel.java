package com.supermartijn642.fusion.model;

import com.supermartijn642.fusion.api.model.BlockModelBakingContext;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.ItemModelBakingContext;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.extensions.BlockModelExtension;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class FusionBlockModel extends BlockModel {

    public static final ThreadLocal<Identifier> CURRENT_MODEL = new ThreadLocal<>();

    private final Identifier name;
    private final ModelInstance<?> model;
    private final UnbakedModel vanillaModel;
    private Collection<Identifier> dependencies;
    private Map<Identifier,UnbakedModel> resolvedDependencies;

    public FusionBlockModel(ModelInstance<?> model){
        super(null, null, null, null, new TextureSlots.Data(Map.of()), null);
        Identifier name = CURRENT_MODEL.get();
        this.name = name == null ? IdentifierUtil.withFusionNamespace("unknown") : name;
        this.model = model;
        this.vanillaModel = model.getAsVanillaModel();
    }

    public BlockStateModel bakeBlockModel(ResolvedModel wrapper, ModelBaker modelBakery, ModelState modelState){
        this.resolveDependencies(modelBakery);
        // Create baking context
        BlockModelBakingContext context = new BlockModelBakingContextImpl(
            modelBakery,
            material -> modelBakery.sprites().get(material, wrapper),
            modelState,
            this.name,
            this.resolvedDependencies,
            wrapper.getTopTextureSlots().resolvedValues,
            wrapper.getTopAmbientOcclusion(),
            wrapper.getTopGuiLight().lightLikeBlock(),
            wrapper.getTopTransforms(),
            wrapper.getTopGeometry()
        );
        // Let the custom model handle the actual baking
        try{
            return this.model.bakeBlockModel(context);
        }catch(Exception e){
            throw new RuntimeException("Encountered an exception while baking block model of type '" + ModelTypeRegistryImpl.getIdentifier(this.model.getModelType()) + "' for  '" + this.name + "'!", e);
        }
    }

    public ItemModel bakeItemModel(ResolvedModel wrapper, ModelBaker modelBakery, List<ItemTintSource> tintSources, EntityModelSet entityModelSet){
        this.resolveDependencies(modelBakery);
        // Create baking context
        ItemModelBakingContext context = new ItemModelBakingContextImpl(
            modelBakery,
            material -> modelBakery.sprites().get(material, wrapper),
            this.name,
            this.resolvedDependencies,
            wrapper.getTopTextureSlots().resolvedValues,
            wrapper.getTopAmbientOcclusion(),
            wrapper.getTopGuiLight().lightLikeBlock(),
            wrapper.getTopTransforms(),
            wrapper.getTopGeometry(),
            tintSources,
            entityModelSet
        );
        // Let the custom model handle the actual baking
        try{
            return this.model.bakeItemModel(context);
        }catch(Exception e){
            throw new RuntimeException("Encountered an exception while baking item model of type '" + ModelTypeRegistryImpl.getIdentifier(this.model.getModelType()) + "' for  '" + this.name + "'!", e);
        }
    }

    private Map<Identifier,UnbakedModel> resolveDependencies(ModelBaker modelBaker){
        if(this.resolvedDependencies != null)
            return this.resolvedDependencies;
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
        for(Identifier location : this.dependencies)
            this.resolvedDependencies.put(location, modelBaker.getModel(location).wrapped());
        // Recursively gather all dependencies for the model
        Deque<Identifier> unresolved = new LinkedList<>(this.dependencies);
        while(!unresolved.isEmpty()){
            Identifier location = unresolved.removeFirst();
            UnbakedModel unbakedModel = this.resolvedDependencies.get(location);
            if(unbakedModel instanceof FusionBlockModel fusionBlockModel)
                this.resolvedDependencies.putAll(fusionBlockModel.resolveDependencies(modelBaker));
            else{
                Identifier parent = unbakedModel.parent();
                if(parent != null){
                    this.resolvedDependencies.put(parent, modelBaker.getModel(parent).wrapped());
                    unresolved.add(parent);
                }
            }
        }
        // Always add missing model
        this.resolvedDependencies.put(MissingBlockModel.LOCATION, modelBaker.getModel(MissingBlockModel.LOCATION).wrapped());
        return this.resolvedDependencies;
    }

    public boolean hasVanillaModel(){
        return this.vanillaModel != null;
    }

    public UnbakedModel getVanillaModel(){
        return this.vanillaModel;
    }

    @Override
    public @Nullable UnbakedGeometry geometry(){
        return this.vanillaModel == null ? null : this.vanillaModel.geometry();
    }

    @Override
    public @Nullable GuiLight guiLight(){
        return this.vanillaModel == null ? null : this.vanillaModel.guiLight();
    }

    @Override
    public @Nullable Boolean ambientOcclusion(){
        return this.vanillaModel == null ? null : this.vanillaModel.ambientOcclusion();
    }

    @Override
    public @Nullable ItemTransforms transforms(){
        return this.vanillaModel == null ? null : this.vanillaModel.transforms();
    }

    @Override
    public TextureSlots.Data textureSlots(){
        return this.vanillaModel == null ? new TextureSlots.Data(Map.of()) : this.vanillaModel.textureSlots();
    }

    @Override
    public @Nullable Identifier parent(){
        return this.vanillaModel == null ? null : this.vanillaModel.parent();
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
