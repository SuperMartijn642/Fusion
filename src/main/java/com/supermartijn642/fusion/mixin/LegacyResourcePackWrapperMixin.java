package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.extensions.ResourcePackExtension;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.LegacyV2Adapter;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;

/**
 * Created 22/10/2023 by SuperMartijn642
 */
@Mixin(LegacyV2Adapter.class)
public class LegacyResourcePackWrapperMixin implements ResourcePackExtension {

    @Final
    @Shadow
    private IResourcePack pack;

    @Override
    public void setFusionOverridesFolder(@Nonnull String folder){
        if(this.pack instanceof ResourcePackExtension)
            ((ResourcePackExtension)this.pack).setFusionOverridesFolder(folder);
    }

    @Override
    public Collection<ResourceLocation> fusionGetResources(String folder, int maxDepth, Predicate<String> filter){
        return Collections.emptyList();
    }
}
