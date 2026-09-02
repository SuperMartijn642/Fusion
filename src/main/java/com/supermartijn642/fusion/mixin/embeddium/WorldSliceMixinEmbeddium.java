package com.supermartijn642.fusion.mixin.embeddium;

import com.supermartijn642.fusion.util.BiomeGetter;
import com.supermartijn642.fusion.util.Dimensional;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Created 21/07/2026 by SuperMartijn642
 */
@Mixin(WorldSlice.class)
public class WorldSliceMixinEmbeddium implements Dimensional, BiomeGetter {

    @Final
    @Shadow
    private World world;

    @Override
    public ResourceLocation fusionGetDimension(){
        return this.world.dimension().location();
    }

    @Override
    public @Nullable ResourceLocation fusionGetBiome(BlockPos pos){
        return ((BiomeGetter)this.world).fusionGetBiome(pos);
    }
}
