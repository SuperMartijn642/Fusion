package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.util.Dimensional;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Created 21/07/2026 by SuperMartijn642
 */
@Mixin(World.class)
public class LevelMixin implements Dimensional {

    @Override
    public ResourceLocation fusionGetDimension(){
        //noinspection DataFlowIssue
        World self = (World)(Object)this;
        return self.getDimension().getType().getRegistryName();
    }
}
