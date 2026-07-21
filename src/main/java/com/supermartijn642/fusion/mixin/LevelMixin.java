package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.util.Dimensional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Created 21/07/2026 by SuperMartijn642
 */
@Mixin(Level.class)
public class LevelMixin implements Dimensional {

    @Override
    public ResourceLocation fusionGetDimension(){
        //noinspection DataFlowIssue
        Level self = (Level)(Object)this;
        return self.dimension().location();
    }
}
