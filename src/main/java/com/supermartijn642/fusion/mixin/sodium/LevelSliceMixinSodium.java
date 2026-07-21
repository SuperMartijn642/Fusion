package com.supermartijn642.fusion.mixin.sodium;

import com.supermartijn642.fusion.util.Dimensional;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Created 21/07/2026 by SuperMartijn642
 */
@Mixin(LevelSlice.class)
public class LevelSliceMixinSodium implements Dimensional {

    @Final
    @Shadow
    private ClientLevel level;

    @Override
    public ResourceLocation fusionGetDimension(){
        return this.level.dimension().location();
    }
}
