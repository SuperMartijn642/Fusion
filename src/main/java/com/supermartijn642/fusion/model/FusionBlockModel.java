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
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class FusionBlockModel extends BlockModel {

    public static final UnbakedModel DUMMY_MODEL = new UnbakedModel() {
        @Override
        public Collection<ResourceLocation> getDependencies(){
            return Collections.emptyList();
        }

        @Override
        public void resolveParents(Function<ResourceLocation,UnbakedModel> function){
        }

        @Nullable
        @Override
        public BakedModel bake(ModelBaker modelBaker, Function<Material,TextureAtlasSprite> function, ModelState modelState, ResourceLocation resourceLocation){
            return null;
        }
    };

    private final ModelInstance<?> model;
    private final BlockModel vanillaModel;
    private Collection<ResourceLocation> dependencies;

    public FusionBlockModel(ModelInstance<?> model){
        super(null, Collections.emptyList(), Collections.emptyMap(), false, null, ItemTransforms.NO_TRANSFORMS, Collections.emptyList());
        this.model = model;
        this.vanillaModel = model.getAsVanillaModel();
    }

    @Override
    public BakedModel bake(ModelBaker baker, BlockModel someOtherModel, Function<Material,TextureAtlasSprite> spriteGetter, ModelState modelTransform, ResourceLocation modelLocation, boolean gui3d){
        // Let the custom model handle the actual baking
        ModelBakingContext context = new ModelBakingContextImpl(baker, spriteGetter, modelTransform, modelLocation);
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
    public void resolveParents(Function<ResourceLocation,UnbakedModel> function){
        BlockModel vanillaModel = this.model.getAsVanillaModel();
        if(vanillaModel != null)
            vanillaModel.resolveParents(function);
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
