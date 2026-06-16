package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.entity.model.EntityLayerProperties;
import com.supermartijn642.fusion.extensions.EntityRenderStateExtension;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Created 29/09/2025 by SuperMartijn642
 */
@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements EntityRenderStateExtension {

    @Unique
    private EntityLayerProperties.ModelChoice[] models;

    @Override
    public EntityLayerProperties.ModelChoice getFusionModel(int layerIndex){
        return this.models == null ? null : this.models[layerIndex];
    }

    @Override
    public void setFusionModel(int layerIndex, EntityLayerProperties.ModelChoice model){
        if(this.models == null)
            this.models = new EntityLayerProperties.ModelChoice[layerIndex + 1];
        else if(this.models.length <= layerIndex){
            EntityLayerProperties.ModelChoice[] newModels = new EntityLayerProperties.ModelChoice[layerIndex + 1];
            System.arraycopy(this.models, 0, newModels, 0, layerIndex);
            this.models = newModels;
        }
        this.models[layerIndex] = model;
    }

    @Override
    public boolean hasFusionContext(){
        return this.models != null;
    }
}
