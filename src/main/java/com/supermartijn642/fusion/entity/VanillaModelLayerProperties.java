package com.supermartijn642.fusion.entity;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Created 25/09/2024 by SuperMartijn642
 */
public class VanillaModelLayerProperties {

    private static final Map<ModelLayerLocation,VanillaModelLayerProperties> PROPERTIES;
    private static final VanillaModelLayerProperties LIVING_PROPERTIES;
    private static final VanillaModelLayerProperties FALLBACK_PROPERTIES;

    static{
        LIVING_PROPERTIES = new VanillaModelLayerProperties(Transform.NONE, true, true, false, 0, -1.501f, 0);
        FALLBACK_PROPERTIES = new VanillaModelLayerProperties(Transform.NONE, false, false, false, 0, 0, 0);

        // TODO go through all layers for special cases
        ImmutableMap.Builder<ModelLayerLocation,VanillaModelLayerProperties> builder = ImmutableMap.builder();

        PROPERTIES = builder.build();
    }

    @NotNull
    public static VanillaModelLayerProperties get(ModelLayerLocation location, EntityRenderer<?,?> renderer){
        if(PROPERTIES.containsKey(location))
            return PROPERTIES.get(location);
        // Fallback
        if(renderer instanceof LivingEntityRenderer)
            return LIVING_PROPERTIES;
        return FALLBACK_PROPERTIES;
    }

    private final Transform transform;
    private final boolean flipX, flipY, flipZ;
    private final float offsetX, offsetY, offsetZ;

    private VanillaModelLayerProperties(Transform transform, boolean flipX, boolean flipY, boolean flipZ, float offsetX, float offsetY, float offsetZ){
        this.transform = transform;
        this.flipX = flipX;
        this.flipY = flipY;
        this.flipZ = flipZ;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    public void transform(PoseStack poseStack){
        this.transform.transform(poseStack);
    }

    public boolean shouldFlipX(){
        return this.flipX;
    }

    public boolean shouldFlipY(){
        return this.flipY;
    }

    public boolean shouldFlipZ(){
        return this.flipZ;
    }

    public float getOffsetX(){
        return this.offsetX;
    }

    public float getOffsetY(){
        return this.offsetY;
    }

    public float getOffsetZ(){
        return this.offsetZ;
    }

    private interface Transform {
        Transform NONE = poseStack -> {};

        void transform(PoseStack poseStack);
    }
}
