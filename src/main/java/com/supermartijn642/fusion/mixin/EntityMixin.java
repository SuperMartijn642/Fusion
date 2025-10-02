package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.entity.EntityModelModifierManager;
import com.supermartijn642.fusion.entity.model.EntityLayerProperties;
import com.supermartijn642.fusion.extensions.EntityExtension;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Arrays;

/**
 * Created 23/09/2024 by SuperMartijn642
 */
@Mixin(Entity.class)
public class EntityMixin implements EntityExtension {

    @Unique
    private EntityLayerProperties.ModelChoice[] models;
    @Unique
    private int lastReload = -1;

    @Override
    public EntityLayerProperties.ModelChoice getFusionModel(int layerIndex){
        return this.models[layerIndex];
    }

    @Override
    public void setFusionModel(int layerIndex, EntityLayerProperties.ModelChoice model){
        if(this.lastReload != EntityModelModifierManager.reloadCounter){
            if(this.models != null)
                Arrays.fill(this.models, null);
            this.lastReload = EntityModelModifierManager.reloadCounter;
        }
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
    public boolean shouldFusionRecomputeModel(int layerIndex){
        return this.models == null || this.models.length <= layerIndex || this.models[layerIndex] == null || this.lastReload != EntityModelModifierManager.reloadCounter;
    }

    @Override
    public void markFusionRecomputeModels(){
        if(this.models != null)
            Arrays.fill(this.models, null);
    }
}
