package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.extensions.PackResourcesExtension;
import net.minecraft.client.resources.LegacyResourcePackWrapper;
import net.minecraft.resources.IResourcePack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nonnull;

/**
 * Created 22/10/2023 by SuperMartijn642
 */
@Mixin(LegacyResourcePackWrapper.class)
public class LegacyResourcePackWrapperMixin implements PackResourcesExtension {

    @Final
    @Shadow
    private IResourcePack source;

    @Override
    public void setFusionOverridesFolder(@Nonnull String folder){
        if(this.source instanceof PackResourcesExtension)
            ((PackResourcesExtension)this.source).setFusionOverridesFolder(folder);
    }
}
