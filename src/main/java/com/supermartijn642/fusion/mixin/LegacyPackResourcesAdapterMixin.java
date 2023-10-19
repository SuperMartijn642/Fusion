package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.extensions.PackResourcesExtension;
import net.minecraft.client.resources.LegacyPackResourcesAdapter;
import net.minecraft.server.packs.PackResources;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Created 22/10/2023 by SuperMartijn642
 */
@Mixin(LegacyPackResourcesAdapter.class)
public class LegacyPackResourcesAdapterMixin implements PackResourcesExtension {

    @Final
    @Shadow
    private PackResources source;

    @Override
    public void setFusionOverridesFolder(@NotNull String folder){
        if(this.source instanceof PackResourcesExtension)
            ((PackResourcesExtension)this.source).setFusionOverridesFolder(folder);
    }
}
