package com.supermartijn642.fusion.mixin.sodium;

import com.supermartijn642.fusion.util.BiomeGetter;
import com.supermartijn642.fusion.util.Dimensional;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Created 21/07/2026 by SuperMartijn642
 */
@Mixin(LevelSlice.class)
public class LevelSliceMixin implements Dimensional, BiomeGetter {

    @Final
    @Shadow
    private ClientLevel level;

    @Override
    public Identifier fusionGetDimension(){
        return this.level.dimension().identifier();
    }

    @Override
    public @Nullable Identifier fusionGetBiome(BlockPos pos){
        return ((BiomeGetter)this.level).fusionGetBiome(pos);
    }
}
