package com.supermartijn642.fusion.mixin;

import com.google.common.collect.ImmutableMap;
import com.supermartijn642.fusion.entity.EntityModelModifierManager;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * Created 23/09/2024 by SuperMartijn642
 */
@Mixin(EntityRenderers.class)
public class EntityRenderersMixin {

    @Final
    @Mutable
    @Shadow
    private static Map<EntityType<?>,EntityRendererProvider<?>> PROVIDERS;
    @Unique
    private static Map<EntityType<?>,EntityRendererProvider<?>> providersCopy;

    @Inject(
        method = "createEntityRenderers",
        at = @At("HEAD")
    )
    private static void interceptRendererCreation(EntityRendererProvider.Context context, CallbackInfoReturnable<?> ci){
        ImmutableMap.Builder<EntityType<?>,EntityRendererProvider<?>> builder = ImmutableMap.builder();
        //noinspection unchecked
        PROVIDERS.forEach((entityType, rendererProvider) -> builder.put(entityType, c -> (EntityRenderer<Entity,?>)EntityModelModifierManager.handleRendererCreation(entityType, rendererProvider, c)));
        providersCopy = PROVIDERS;
        PROVIDERS = builder.build();
    }

    @Inject(
        method = "createEntityRenderers",
        at = @At("TAIL")
    )
    private static void resetProviderMap(EntityRendererProvider.Context context, CallbackInfoReturnable<?> ci){
        PROVIDERS = providersCopy;
        providersCopy = null;
    }
}
