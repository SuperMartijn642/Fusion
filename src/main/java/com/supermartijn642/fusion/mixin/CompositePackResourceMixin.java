package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.extensions.PackResourcesExtension;
import net.minecraft.server.packs.CompositePackResources;
import net.minecraft.server.packs.PackResources;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

/**
 * Created 13/07/2025 by SuperMartijn642
 */
@Mixin(CompositePackResources.class)
public class CompositePackResourceMixin implements PackResourcesExtension {

    @Final
    @Shadow
    private PackResources primaryPackResources;
    @Final
    @Shadow
    private List<PackResources> packResourcesStack;

    @Override
    public void setFusionOverridesFolder(String folder){
        if(this.primaryPackResources instanceof PackResourcesExtension)
            ((PackResourcesExtension)this.primaryPackResources).setFusionOverridesFolder(folder);
        for(PackResources packResources : this.packResourcesStack){
            if(packResources instanceof PackResourcesExtension)
                ((PackResourcesExtension)packResources).setFusionOverridesFolder(folder);
        }
    }
}
