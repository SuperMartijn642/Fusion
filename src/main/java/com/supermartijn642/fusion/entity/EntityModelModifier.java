package com.supermartijn642.fusion.entity;

import com.supermartijn642.fusion.entity.model.EntityLayerProperties;
import net.minecraft.client.model.geom.ModelLayerLocation;

import java.util.Map;

/**
 * Created 23/09/2024 by SuperMartijn642
 */
public class EntityModelModifier {

    private final Map<ModelLayerLocation,EntityLayerProperties> layers;

    public EntityModelModifier(Map<ModelLayerLocation,EntityLayerProperties> layers){
        this.layers = layers;
    }

    public Map<ModelLayerLocation,EntityLayerProperties> getLayers(){
        return this.layers;
    }
}
