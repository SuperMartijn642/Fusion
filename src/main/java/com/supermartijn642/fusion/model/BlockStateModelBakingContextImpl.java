package com.supermartijn642.fusion.model;

import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.custom.*;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class BlockStateModelBakingContextImpl implements BlockStateModelBakingContext {

    private final Consumer<String> warnings;
    private final Identifier identifier;
    private final ModelTransform transform;
    private final ModelBaker modelBaker;
    private final ContextMap neoforgeAdditionalProperties;
    private ModelBakery.MissingModels missingModels;

    public BlockStateModelBakingContextImpl(Consumer<String> warnings, Identifier identifier, ModelTransform transform, ModelBaker modelBaker, ContextMap neoforgeAdditionalProperties){
        this.warnings = warnings;
        this.identifier = identifier;
        this.transform = transform;
        this.modelBaker = modelBaker;
        this.neoforgeAdditionalProperties = neoforgeAdditionalProperties;
    }

    @Override
    public void pushWarning(String warning){
        this.warnings.accept(warning);
    }

    @Override
    public Identifier getModelIdentifier(){
        return this.identifier;
    }

    @Override
    public ModelTransform getTransformation(){
        return this.transform;
    }

    @Override
    public TextureAtlasSprite getMaterial(ModelMaterial material){
        return this.modelBaker.sprites().get(material.toMaterial(), this.identifier::toString);
    }

    @Override
    public @Nullable ModelInstance<?> getModel(Identifier identifier){
        UntypedModelInstance modelInstance = FusionBlockModelData.getModelInstance(this.modelBaker.getModel(identifier).wrapped());
        return modelInstance instanceof ModelInstance<?> ? (ModelInstance<?>)modelInstance : null;
    }

    protected ModelBakery.MissingModels getMissingModels(){
        if(this.missingModels == null)
            this.missingModels = ModelBakery.MissingModels.bake(this.modelBaker.getModel(ModelResolver.MISSING_MODEL), this.modelBaker.sprites(), this.modelBaker.parts());
        return this.missingModels;
    }

    @Override
    public BlockStateModel getMissingBlockStateModel(){
        return this.getMissingModels().block();
    }

    @Override
    public BlockModelPart getMissingBlockStateModelPart(){
        return this.modelBaker.missingBlockModelPart();
    }

    @Override
    public ModelBaker getModelBaker(){
        return this.modelBaker;
    }

    @Override
    public ContextMap getNeoForgeAdditionalProperties(){
        return this.neoforgeAdditionalProperties;
    }
}
