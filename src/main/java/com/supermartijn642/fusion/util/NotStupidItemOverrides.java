package com.supermartijn642.fusion.util;

import com.google.common.collect.Lists;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Item overrides extension with a not stupid constructor.
 * <p>
 * Created 09/06/2026 by SuperMartijn642
 */
public class NotStupidItemOverrides extends ItemOverrides {

    public NotStupidItemOverrides(List<ItemOverride> overrides, Function<ResourceLocation,BakedModel> modelBaker){
        Map<ResourceLocation,Integer> properties = new LinkedHashMap<>();
        List<BakedOverride> bakedOverrides = Lists.newArrayList();
        for(ItemOverride override : overrides){
            // Bake the model
            BakedModel bakedModel;
            try{
                bakedModel = modelBaker.apply(override.getModel());
            }catch(Exception e){
                throw new RuntimeException("Encountered an exception baking item overrides model '" + override.getModel() + "'!", e);
            }
            // Create property matchers
            PropertyMatcher[] propertyMatchers = override.getPredicates().map(predicate -> {
                int propertyIndex = properties.computeIfAbsent(predicate.getProperty(), p -> properties.size());
                return new PropertyMatcher(propertyIndex, predicate.getValue());
            }).toArray(PropertyMatcher[]::new);
            // Add override
            bakedOverrides.add(new BakedOverride(propertyMatchers, bakedModel));
        }
        this.overrides = bakedOverrides.toArray(new BakedOverride[0]);
        this.properties = properties.keySet().toArray(new ResourceLocation[0]);
    }
}
