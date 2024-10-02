package com.supermartijn642.fusion.entity;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Created 02/10/2024 by SuperMartijn642
 */
public class EntityRenderTypeHelper {

    private static final Map<String,Function<ResourceLocation,RenderType>> RENDER_TYPES_BY_NAME;
    private static final Map<String,BiFunction<ResourceLocation,Boolean,RenderType>> VAR_OUTLINE_RENDER_TYPES_BY_NAME;

    static {
        ImmutableMap.Builder<String,Function<ResourceLocation,RenderType>> renderTypes = ImmutableMap.builder();
        renderTypes.put("armor_cutout_no_cull", RenderType::armorCutoutNoCull);
        renderTypes.put("entity_solid", RenderType::entitySolid);
        renderTypes.put("entity_cutout", RenderType::entityCutout);
        renderTypes.put("item_entity_translucent_cull", RenderType::itemEntityTranslucentCull);
        renderTypes.put("entity_translucent_cull", RenderType::entityTranslucentCull);
        renderTypes.put("entity_smooth_cutout", RenderType::entitySmoothCutout);
        renderTypes.put("entity_decal", RenderType::entityDecal);
        renderTypes.put("entity_no_outline", RenderType::entityNoOutline);
        renderTypes.put("entity_alpha", RenderType::dragonExplosionAlpha);
        renderTypes.put("eyes", RenderType::eyes);
        renderTypes.put("water_mask", t -> RenderType.waterMask());
        renderTypes.put("armor_entity_glint", t -> RenderType.armorEntityGlint());
        RENDER_TYPES_BY_NAME = renderTypes.build();
        ImmutableMap.Builder<String,BiFunction<ResourceLocation,Boolean,RenderType>> outlineRenderTypes = ImmutableMap.builder();
        outlineRenderTypes.put("entity_cutout_no_cull", RenderType::entityCutoutNoCull);
        outlineRenderTypes.put("entity_cutout_no_cull_z_offset", RenderType::entityCutoutNoCullZOffset);
        outlineRenderTypes.put("entity_translucent", RenderType::entityTranslucent);
        VAR_OUTLINE_RENDER_TYPES_BY_NAME = outlineRenderTypes.build();
    }

    public static RenderType getRenderTypeWithTexture(RenderType renderType, ResourceLocation texture){
        // Check for a match with render types suppliers that have an outline argument
        if(renderType instanceof RenderType.CompositeRenderType){
            BiFunction<ResourceLocation,Boolean,RenderType> supplier = VAR_OUTLINE_RENDER_TYPES_BY_NAME.get(renderType.name);
            if(supplier != null){
                RenderType.OutlineProperty outlineProperty = ((RenderType.CompositeRenderType)renderType).state.outlineProperty;
                return supplier.apply(texture, outlineProperty == RenderType.OutlineProperty.AFFECTS_OUTLINE);
            }
        }
        // Check for a match with regular render types suppliers
        Function<ResourceLocation,RenderType> supplier = RENDER_TYPES_BY_NAME.get(renderType.name);
        if(supplier != null)
            return supplier.apply(texture);
        // If nothing, return null
        return null;
    }
}
