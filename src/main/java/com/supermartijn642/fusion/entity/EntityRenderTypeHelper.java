package com.supermartijn642.fusion.entity;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Created 02/10/2024 by SuperMartijn642
 */
public class EntityRenderTypeHelper {

    private static final Map<String,Function<Identifier,RenderType>> RENDER_TYPES_BY_NAME;
    private static final Map<String,BiFunction<Identifier,Boolean,RenderType>> VAR_OUTLINE_RENDER_TYPES_BY_NAME;

    static{
        ImmutableMap.Builder<String,Function<Identifier,RenderType>> renderTypes = ImmutableMap.builder();
        renderTypes.put("armor_cutout_no_cull", RenderTypes::armorCutoutNoCull);
        renderTypes.put("armor_translucent", RenderTypes::armorTranslucent);
        renderTypes.put("entity_solid", RenderTypes::entitySolid);
        renderTypes.put("entity_solid_z_offset_forward", RenderTypes::entitySolidZOffsetForward);
        renderTypes.put("entity_cutout", RenderTypes::entityCutout);
        renderTypes.put("item_entity_translucent_cull", RenderTypes::itemTranslucent);
        renderTypes.put("entity_shadow", RenderTypes::entityShadow);
        renderTypes.put("eyes", RenderTypes::eyes);
        RENDER_TYPES_BY_NAME = renderTypes.build();
        ImmutableMap.Builder<String,BiFunction<Identifier,Boolean,RenderType>> outlineRenderTypes = ImmutableMap.builder();
        outlineRenderTypes.put("entity_cutout_no_cull", RenderTypes::entityCutout);
        outlineRenderTypes.put("entity_cutout_no_cull_z_offset", RenderTypes::entityCutoutZOffset);
        outlineRenderTypes.put("entity_translucent", RenderTypes::entityTranslucent);
        outlineRenderTypes.put("entity_translucent_emissive", RenderTypes::entityTranslucentEmissive);
        VAR_OUTLINE_RENDER_TYPES_BY_NAME = outlineRenderTypes.build();
    }

    public static RenderType getRenderTypeWithTexture(RenderType renderType, Identifier texture){
        // Check for a match with render types suppliers that have an outline argument
        BiFunction<Identifier,Boolean,RenderType> varOutlineSupplier = VAR_OUTLINE_RENDER_TYPES_BY_NAME.get(renderType.name);
        if(varOutlineSupplier != null){
            RenderSetup.OutlineProperty outlineProperty = renderType.state.outlineProperty;
            return varOutlineSupplier.apply(texture, outlineProperty == RenderSetup.OutlineProperty.AFFECTS_OUTLINE);
        }
        // Check for a match with regular render types suppliers
        Function<Identifier,RenderType> supplier = RENDER_TYPES_BY_NAME.get(renderType.name);
        if(supplier != null)
            return supplier.apply(texture);
        // If nothing, return null
        return null;
    }
}
