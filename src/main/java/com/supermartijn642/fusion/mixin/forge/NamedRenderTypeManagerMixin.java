package com.supermartijn642.fusion.mixin.forge;

import com.google.common.collect.ImmutableMap;
import com.supermartijn642.fusion.util.ForgeNamedRenderTypeGroupHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.NamedRenderTypeManager;
import net.minecraftforge.client.RenderTypeGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 07/06/2026 by SuperMartijn642
 */
@Mixin(NamedRenderTypeManager.class)
public class NamedRenderTypeManagerMixin {

    /*
     * Forge provides no mapping for render type group -> identifier.
     * Hence, we need to build this mapping ourselves whenever the render type groups registrations are updated.
     */

    @Shadow(remap = false)
    private static Map<ResourceLocation,RenderTypeGroup> RENDER_TYPES;

    @Inject(
        method = "init",
        at = @At("TAIL"),
        remap = false
    )
    private static void init(CallbackInfo ci){
        ForgeNamedRenderTypeGroupHelper.updateMappings(RENDER_TYPES);
    }
}
