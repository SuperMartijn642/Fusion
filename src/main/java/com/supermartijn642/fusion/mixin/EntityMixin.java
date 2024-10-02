package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.entity.EntityModelModifierManager;
import com.supermartijn642.fusion.extensions.EntityExtension;
import com.supermartijn642.fusion.util.Triple;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
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
    private Triple<ModelPart,ResourceLocation,Float>[] models;
    @Unique
    private int lastReload = -1;

    @Override
    public Triple<ModelPart,ResourceLocation,Float> getFusionModel(int layerIndex){
        return this.models[layerIndex];
    }

    @Override
    public void setFusionModel(int layerIndex, Triple<ModelPart,ResourceLocation,Float> model){
        if(this.lastReload != EntityModelModifierManager.reloadCounter){
            if(this.models != null)
                Arrays.fill(this.models, null);
            this.lastReload = EntityModelModifierManager.reloadCounter;
        }
        if(this.models == null){
            //noinspection unchecked
            this.models = new Triple[layerIndex + 1];
        }else if(this.models.length <= layerIndex){
            //noinspection unchecked
            Triple<ModelPart,ResourceLocation,Float>[] newModels = new Triple[layerIndex + 1];
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
