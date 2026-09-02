package com.supermartijn642.fusion.mixin.rubidium;

import com.supermartijn642.fusion.util.BiomeGetter;
import com.supermartijn642.fusion.util.Dimensional;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Created 21/07/2026 by SuperMartijn642
 */
@Mixin(WorldSlice.class)
public class WorldSliceMixinRubidium implements Dimensional, BiomeGetter {

    @Final
    @Shadow
    private ClientLevel world;

    @Override
    public ResourceLocation fusionGetDimension(){
        return this.world.dimension().location();
    }

    @Override
    public @Nullable ResourceLocation fusionGetBiome(BlockPos pos){
        return ((BiomeGetter)this.world).fusionGetBiome(pos);
    }
}
