package com.supermartijn642.fusion.model;

import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.custom.BlockStateModelBakingContext;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.custom.UntypedModelInstance;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
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
        return this.modelBaker.sprites().get(material.toMaterial(), this.identifier::toString);
    }

    @Override
    public @Nullable ModelInstance<?> getModel(ResourceLocation identifier){
        UntypedModelInstance modelInstance = FusionBlockModelData.getModelInstance(this.modelBaker.getModel(identifier).wrapped());
        return modelInstance instanceof ModelInstance<?> ? (ModelInstance<?>)modelInstance : null;
    }

    @Override
    public ModelBaker getModelBaker(){
        return this.modelBaker;
    }
}
