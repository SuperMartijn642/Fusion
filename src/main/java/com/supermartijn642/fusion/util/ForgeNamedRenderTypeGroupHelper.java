package com.supermartijn642.fusion.util;

import net.minecraft.resources.Identifier;
import net.minecraftforge.client.RenderTypeGroup;

import java.util.*;

/**
 * Helps with going from render type group -> identifier as Forge has no way to do this.
 * <p>
 * Created 07/06/2026 by SuperMartijn642
 */
public class ForgeNamedRenderTypeGroupHelper {

    private static Map<RenderTypeGroup,Identifier> RENDER_TYPE_GROUPS = Map.of();

    public static void updateMappings(Map<Identifier,RenderTypeGroup> renderTypes){
        // Sort identifiers, so behavior is consistent
        List<Identifier> identifiers = new ArrayList<>(renderTypes.keySet());
        identifiers.sort(Comparator.comparing(Identifier::toString));

        // Build render type group -> identifier map
        Map<RenderTypeGroup,Identifier> reverseMapping = new HashMap<>();
        for(Identifier identifier : identifiers){
            RenderTypeGroup renderTypeGroup = renderTypes.get(identifier);
            if(!reverseMapping.containsKey(renderTypeGroup))
                reverseMapping.put(renderTypeGroup, identifier);
        }
        RENDER_TYPE_GROUPS = Map.copyOf(reverseMapping);
    }

    public static Identifier getIdentifier(RenderTypeGroup renderTypeGroup){
        return RENDER_TYPE_GROUPS.get(renderTypeGroup);
    }
}
