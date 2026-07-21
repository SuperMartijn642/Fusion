package com.supermartijn642.fusion.mixin.embeddium;

import com.supermartijn642.fusion.util.Dimensional;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Created 21/07/2026 by SuperMartijn642
 */
@Mixin(WorldSlice.class)
public class WorldSliceMixinEmbeddium implements Dimensional {

    @Final
    @Shadow
    private ClientLevel world;

    @Override
    public ResourceLocation fusionGetDimension(){
        return this.world.dimension().location();
    }
}
