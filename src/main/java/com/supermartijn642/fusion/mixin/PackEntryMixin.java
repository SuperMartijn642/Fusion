package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.extensions.PackExtension;
import com.supermartijn642.fusion.resources.FusionPackMetadata;
import com.supermartijn642.fusion.resources.ResourcePackListTipRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ResourcePacksScreen;
import net.minecraft.client.gui.widget.list.AbstractResourcePackList;
import net.minecraft.client.resources.ClientResourcePackInfo;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

/**
 * Created 21/10/2024 by SuperMartijn642
 */
@Mixin(AbstractResourcePackList.ResourcePackEntry.class)
public class PackEntryMixin {

    @Final
    @Shadow
    private ClientResourcePackInfo resourcePack;
    @Final
    @Shadow
    private ResourcePacksScreen screen;
    @Unique
    private FusionPackMetadata metadata;

    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    private void cacheFusionMetadata(CallbackInfo ci){
        this.metadata = ((PackExtension)this.resourcePack).getFusionMetadata();
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/AbstractGui;blit(IIFFIIII)V",
            shift = At.Shift.BEFORE,
            ordinal = 0
        )
    )
    private void renderBackground(int entryIndex, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTicks, CallbackInfo ci){
        if(this.metadata == null)
            return;
        ResourcePackListTipRenderer.renderBackground(this.metadata, this.resourcePack.getCompatibility().isCompatible(), left, top, width, height);
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/AbstractGui;blit(IIFFIIII)V",
            shift = At.Shift.AFTER,
            ordinal = 0
        )
    )
    private void renderIcon(int entryIndex, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTicks, CallbackInfo ci){
        if(this.metadata == null)
            return;
        ResourcePackListTipRenderer.renderIcon(this.metadata, this.resourcePack.getCompatibility().isCompatible(), left, top, width, height);
    }

    @ModifyVariable(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/widget/list/AbstractResourcePackList$ResourcePackEntry;canMoveRight()Z",
            shift = At.Shift.BEFORE
        ),
        ordinal = 1
    )
    private String adjustDescription(String label){
        if(this.metadata == null)
            return label;
        ITextComponent warningMessage = ResourcePackListTipRenderer.getWarningMessage(this.metadata, this.resourcePack.getCompatibility().isCompatible());
        return warningMessage == null ? label : warningMessage.getColoredString();
    }

    @Inject(
        method = "mouseClicked",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/widget/list/AbstractResourcePackList$ResourcePackEntry;getCompatibility()Lnet/minecraft/resources/PackCompatibility;",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private void showFusionWarningScreen(CallbackInfoReturnable<Boolean> ci){
        Consumer<Boolean> callback = select -> {
            Minecraft.getInstance().setScreen(this.screen);
            if(select)
                //noinspection DataFlowIssue
                this.screen.select((AbstractResourcePackList.ResourcePackEntry)(Object)this);
        };
        if(ResourcePackListTipRenderer.showWarningScreen(this.metadata, this.resourcePack.getCompatibility().isCompatible(), this.resourcePack, callback))
            ci.setReturnValue(false);
    }
}
