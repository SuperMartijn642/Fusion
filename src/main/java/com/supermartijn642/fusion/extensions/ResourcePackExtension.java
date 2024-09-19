package com.supermartijn642.fusion.extensions;

import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * Created 19/10/2023 by SuperMartijn642
 */
public interface ResourcePackExtension {

    void setFusionOverridesFolder(@Nonnull String folder);

    Collection<ResourceLocation> fusionGetResources(String folder, int maxDepth, Predicate<String> filter);
}
