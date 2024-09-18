package com.supermartijn642.fusion.mixin;

import com.google.common.collect.ImmutableMap;
import com.supermartijn642.fusion.entity.model.FusionEntityModelLoader;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
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

    @Inject(
        method = "onResourceManagerReload",
        at = @At("TAIL")
    )
    private void loadFusionEntityModels(ResourceManager resourceManager, CallbackInfo ci){
        // Gather the identifier of existing models
        List<ModelLayerLocation> identifiers = new ArrayList<>(this.roots.keySet());
        identifiers.sort(Comparator.comparing(ModelLayerLocation::toString));
        // Try to load models from resource packs
        Map<ModelLayerLocation,LayerDefinition> builder = new HashMap<>(this.roots.size());
        builder.putAll(this.roots);
        builder.putAll(FusionEntityModelLoader.loadModels(identifiers, resourceManager));
        this.roots = ImmutableMap.copyOf(builder);
    }
}
