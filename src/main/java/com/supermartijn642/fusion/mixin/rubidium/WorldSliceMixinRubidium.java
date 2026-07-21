package com.supermartijn642.fusion.mixin.rubidium;

import com.supermartijn642.fusion.util.Dimensional;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Created 21/07/2026 by SuperMartijn642
 */
@Mixin(WorldSlice.class)
public class WorldSliceMixinRubidium implements Dimensional {

    @Final
    @Shadow
    private Level world;

    @Override
    public ResourceLocation fusionGetDimension(){
        return this.world.dimension().location();
    }
}
