package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.texture.FusionTextureMetadataSection;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.Set;

/**
 * Created 29/09/2025 by SuperMartijn642
 */
@Mixin(AtlasManager.AtlasConfig.class)
public class AtlasConfigMixin {

    @ModifyVariable(
        method = "<init>(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;ZLjava/util/Set;)V",
        at = @At("HEAD"),
        ordinal = 0
    )
    private static Set<MetadataSectionType<?>> init(Set<MetadataSectionType<?>> set){
        if(set.isEmpty())
            return Set.of(FusionTextureMetadataSection.TYPE);
        ArrayList<MetadataSectionType<?>> copy = new ArrayList<>(set.size() + 1);
        copy.addAll(set);
        copy.add(FusionTextureMetadataSection.TYPE);
        return Set.copyOf(copy);
    }
}
