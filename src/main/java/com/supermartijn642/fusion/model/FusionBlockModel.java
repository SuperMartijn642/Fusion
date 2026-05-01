package com.supermartijn642.fusion.model;

import com.supermartijn642.fusion.api.model.BlockModelBakingContext;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.ItemModelBakingContext;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.extensions.CuboidModelExtension;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.cuboid.MissingCuboidModel;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class FusionBlockModel {

    public static final ThreadLocal<Identifier> CURRENT_MODEL = new ThreadLocal<>();

    @Nullable
    public static FusionBlockModel get(UnbakedModel model){
        return ((CuboidModelExtension)model).getFusionBlockModelData();
    }

    private final CuboidModel cuboidModel;
    private final Identifier name;
    private final ModelInstance<?> model;
    private final UnbakedModel vanillaModel;
    private Collection<Identifier> dependencies;
    private Map<Identifier,UnbakedModel> resolvedDependencies;

    public FusionBlockModel(ModelInstance<?> model){
        this.cuboidModel = new CuboidModel(null, null, null, null, new TextureSlots.Data(Map.of()), null);
        //noinspection DataFlowIssue
        ((CuboidModelExtension)(Object)this.cuboidModel).setFusionBlockModelData(this);
        Identifier name = CURRENT_MODEL.get();
        this.name = name == null ? IdentifierUtil.withFusionNamespace("unknown") : name;
        this.model = model;
        this.vanillaModel = model.getAsVanillaModel();
    }

    public CuboidModel asCuboidModel(){
        return this.cuboidModel;
    }

    public BlockStateModel bakeBlockModel(ResolvedModel wrapper, ModelBaker modelBakery, ModelState modelState){
        this.resolveDependencies(modelBakery);
        // Create baking context
        BlockModelBakingContext context = new BlockModelBakingContextImpl(
            modelBakery,
            material -> modelBakery.materials().get(material, wrapper),
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

    public ItemModel bakeItemModel(ResolvedModel wrapper, Matrix4fc transformation, List<ItemTintSource> tintSources, ModelBaker modelBakery, EntityModelSet entityModelSet){
        this.resolveDependencies(modelBakery);
        // Create baking context
        ItemModelBakingContext context = new ItemModelBakingContextImpl(
            modelBakery,
            material -> modelBakery.materials().get(material, wrapper),
            transformation,
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
            FusionBlockModel fusionBlockModel = get(unbakedModel);
            if(fusionBlockModel != null)
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
        this.resolvedDependencies.put(MissingCuboidModel.LOCATION, modelBaker.getModel(MissingCuboidModel.LOCATION).wrapped());
        return this.resolvedDependencies;
    }

    public boolean hasVanillaModel(){
        return this.vanillaModel != null;
    }

    public UnbakedModel getVanillaModel(){
        return this.vanillaModel;
    }

    public UnbakedGeometry geometry(){
        return this.vanillaModel == null ? null : this.vanillaModel.geometry();
    }

    public UnbakedModel.GuiLight guiLight(){
        return this.vanillaModel == null ? null : this.vanillaModel.guiLight();
    }

    public Boolean ambientOcclusion(){
        return this.vanillaModel == null ? null : this.vanillaModel.ambientOcclusion();
    }

    public ItemTransforms transforms(){
        return this.vanillaModel == null ? null : this.vanillaModel.transforms();
    }

    public TextureSlots.Data textureSlots(){
        return this.vanillaModel == null ? new TextureSlots.Data(Map.of()) : this.vanillaModel.textureSlots();
    }

    public Identifier parent(){
        return this.vanillaModel == null ? null : this.vanillaModel.parent();
    }

    public static ModelInstance<?> getModelInstance(UnbakedModel model){
        FusionBlockModel fusionBlockModel = get(model);
        if(fusionBlockModel != null)
            return fusionBlockModel.model;
        if(model instanceof CuboidModel){
            ModelInstance<?> modelInstance = ((CuboidModelExtension)model).getFusionModel();
            if(modelInstance == null){
                modelInstance = new ModelInstanceImpl<>(DefaultModelTypes.VANILLA, (CuboidModel)model);
                ((CuboidModelExtension)model).setFusionModel(modelInstance);
            }
            return modelInstance;
        }
        return new ModelInstanceImpl<>(DefaultModelTypes.UNKNOWN, model);
    }
}
