package com.supermartijn642.fusion.util;

import com.google.common.collect.ImmutableMap;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.RenderTypeGroup;

import java.util.*;

/**
 * Helps with going from render type group -> identifier as NeoForge has no way to do this.
 * <p>
 * Created 07/06/2026 by SuperMartijn642
 */
public class NeoForgeNamedRenderTypeGroupHelper {

    private static Map<RenderTypeGroup,ResourceLocation> RENDER_TYPE_GROUPS = Map.of();

    public static void updateMappings(ImmutableMap<ResourceLocation,RenderTypeGroup> renderTypes){
        // Sort identifiers, so behavior is consistent
        List<ResourceLocation> identifiers = new ArrayList<>(renderTypes.keySet());
        identifiers.sort(Comparator.comparing(ResourceLocation::toString));

        // Build render type group -> identifier map
        Map<RenderTypeGroup,ResourceLocation> reverseMapping = new HashMap<>();
        for(ResourceLocation identifier : identifiers){
            RenderTypeGroup renderTypeGroup = renderTypes.get(identifier);
            if(!reverseMapping.containsKey(renderTypeGroup))
                reverseMapping.put(renderTypeGroup, identifier);
        }
        RENDER_TYPE_GROUPS = Map.copyOf(reverseMapping);
    }

    public static ResourceLocation getIdentifier(RenderTypeGroup renderTypeGroup){
        return RENDER_TYPE_GROUPS.get(renderTypeGroup);
    }
}
