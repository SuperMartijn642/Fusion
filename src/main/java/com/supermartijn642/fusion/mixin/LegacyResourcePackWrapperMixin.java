package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.extensions.PackResourcesExtension;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.LegacyV2Adapter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nonnull;

/**
 * Created 22/10/2023 by SuperMartijn642
 */
@Mixin(LegacyV2Adapter.class)
public class LegacyResourcePackWrapperMixin implements PackResourcesExtension {

    @Final
    @Shadow
    private IResourcePack pack;

    @Override
    public void setFusionOverridesFolder(@Nonnull String folder){
        if(this.pack instanceof PackResourcesExtension)
            ((PackResourcesExtension)this.pack).setFusionOverridesFolder(folder);
    }
}
