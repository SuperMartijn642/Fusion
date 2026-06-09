package com.supermartijn642.fusion.util;

import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemOverride;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Item overrides extension with a not stupid constructor.
 * <p>
 * Created 09/06/2026 by SuperMartijn642
 */
public class NotStupidItemOverrides extends ItemOverrideList {

    public NotStupidItemOverrides(List<ItemOverride> overrides, Function<ResourceLocation,IBakedModel> modelBaker){
        this.overrideModels = new ArrayList<>(overrides.size());
        for(int i = overrides.size() - 1; i >= 0; i--){
            ItemOverride override = overrides.get(i);
            this.overrides.add(override);
            try{
                this.overrideModels.add(modelBaker.apply(override.getModel()));
            }catch(Exception e){
                throw new RuntimeException("Encountered an exception baking item overrides model '" + override.getModel() + "'!", e);
            }
        }
    }
}
