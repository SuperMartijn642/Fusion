package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.extensions.PackResourcesExtension;
import net.minecraft.client.resources.PackResourcesAdapterV4;
import net.minecraft.server.packs.PackResources;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Created 22/10/2023 by SuperMartijn642
 */
@Mixin(PackResourcesAdapterV4.class)
public class PackResourcesAdapterV4Mixin implements PackResourcesExtension {

    @Final
    @Shadow
    private PackResources pack;

    @Override
    public void setFusionOverridesFolder(String folder){
        if(this.pack instanceof PackResourcesExtension)
            ((PackResourcesExtension)this.pack).setFusionOverridesFolder(folder);
    }
}
