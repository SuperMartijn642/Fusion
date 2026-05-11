package com.supermartijn642.fusion.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.supermartijn642.fusion.entity.EntityModelModifierManager;
import com.supermartijn642.fusion.entity.EntityModelModifierReloadListener;
import com.supermartijn642.fusion.entity.model.FusionModelPart;
import com.supermartijn642.fusion.entity.model.loader.FusionEntityModelLoader;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

/**
 * Created 17/09/2024 by SuperMartijn642
 */
@Mixin(EntityModelSet.class)
public class EntityModelSetMixin {

    @Shadow
    private Map<ModelLayerLocation,LayerDefinition> roots;

    @ModifyReturnValue(
        method = "bakeLayer",
        at = @At("RETURN")
    )
    private ModelPart trackBakedModel(ModelPart part, ModelLayerLocation location){
        FusionModelPart fusionModelPart = EntityModelModifierManager.handleModelBake(location, part);
        return fusionModelPart == null ? part : fusionModelPart;
    }

    @Inject(
        method = "onResourceManagerReload",
        at = @At("TAIL")
    )
    private void loadFusionEntityModels(ResourceManager resourceManager, CallbackInfo ci){
        // Gather all model locations which should be considered for loading
        Set<ResourceLocation> locations = new HashSet<>();
        // Add models referenced in model modifiers
        EntityModelModifierReloadListener.reload(resourceManager);
        EntityModelModifierReloadListener.getModelLocations(locations::add);
        // Add the identifiers of existing models
        for(ModelLayerLocation layer : this.roots.keySet())
            locations.add(FusionEntityModelLoader.locationForLayer(layer));

        // Try to load models from resource packs
        List<ResourceLocation> locationsSorted = locations.stream().sorted(Comparator.comparing(ResourceLocation::toString)).toList();
        FusionEntityModelLoader.loadModels(locationsSorted, resourceManager);

        // Finalize the models/layers
        EntityModelModifierManager.bakeModels(this.roots);
    }
}
