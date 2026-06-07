package com.supermartijn642.fusion.mixin.neoforge;

import com.google.common.collect.ImmutableMap;
import com.supermartijn642.fusion.util.NeoForgeNamedRenderTypeGroupHelper;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.NamedRenderTypeManager;
import net.neoforged.neoforge.client.RenderTypeGroup;
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
     * NeoForge provides no mapping for render type group -> identifier.
     * Hence, we need to build this mapping ourselves whenever the render type groups registrations are updated.
     */

    @Shadow
    private static ImmutableMap<Identifier,RenderTypeGroup> RENDER_TYPES;

    @Inject(
        method = "init",
        at = @At("TAIL")
    )
    private static void init(CallbackInfo ci) {
        NeoForgeNamedRenderTypeGroupHelper.updateMappings(RENDER_TYPES);
    }
}
