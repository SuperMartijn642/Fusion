package com.supermartijn642.fusion.model;

import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.custom.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class BlockStateModelBakingContextImpl implements BlockStateModelBakingContext {

    private final Consumer<String> warnings;
    private final ResourceLocation identifier;
    private final ModelTransform transform;
    private final ModelBaker modelBaker;
    private BakedModel missingModel;

    public BlockStateModelBakingContextImpl(Consumer<String> warnings, ResourceLocation identifier, ModelTransform transform, ModelBaker modelBaker){
        this.warnings = warnings;
        this.identifier = identifier;
        this.transform = transform;
        this.modelBaker = modelBaker;
    }

    @Override
    public void pushWarning(String warning){
        this.warnings.accept(warning);
    }

    @Override
    public ResourceLocation getModelIdentifier(){
        return this.identifier;
    }

    @Override
    public ModelTransform getTransformation(){
        return this.transform;
    }

    @Override
    public TextureAtlasSprite getMaterial(ModelMaterial material){
        return this.modelBaker.sprites().get(material.toMaterial());
    }

    @Override
    public @Nullable ModelInstance<?> getModel(ResourceLocation identifier){
        if(!(this.modelBaker instanceof ModelBakery.ModelBakerImpl impl))
            return null;
        UntypedModelInstance modelInstance = FusionBlockModelData.getModelInstance(impl.getModel(identifier));
        return modelInstance instanceof ModelInstance<?> ? (ModelInstance<?>)modelInstance : null;
    }

    @Override
    public BakedModel getMissingBakedModel(){
        if(this.missingModel == null)
            this.missingModel = this.modelBaker.bake(ModelResolver.MISSING_MODEL, ModelTransform.identity().toModelState());
        return this.missingModel;
    }

    @Override
    public ModelBaker getModelBaker(){
        return this.modelBaker;
    }
}
