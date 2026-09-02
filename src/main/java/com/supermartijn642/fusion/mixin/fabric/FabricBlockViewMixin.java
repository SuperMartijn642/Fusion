package com.supermartijn642.fusion.mixin.fabric;

import com.supermartijn642.fusion.util.BiomeGetter;
import net.fabricmc.fabric.api.blockview.v2.FabricBlockView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Created 03/09/2026 by SuperMartijn642
 */
@Mixin(FabricBlockView.class)
public interface FabricBlockViewMixin extends BiomeGetter {

    @Override
    default @Nullable Identifier fusionGetBiome(BlockPos pos){
        Holder<Biome> biome = ((FabricBlockView)this).getBiomeFabric(pos);
        return biome == null || !biome.isBound() ? null : biome.unwrapKey().get().identifier();
    }
}
