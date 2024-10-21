package com.supermartijn642.fusion.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.supermartijn642.fusion.extensions.PackExtension;
import com.supermartijn642.fusion.resources.FusionPackMetadata;
import com.supermartijn642.fusion.resources.ResourcePackListTipRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IBidiRenderer;
import net.minecraft.client.gui.screen.PackLoadingManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.list.ResourcePackList;
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
@Mixin(ResourcePackList.ResourcePackEntry.class)
public class PackEntryMixin {

    @Final
    @Shadow
    private PackLoadingManager.IPack pack;
    @Final
    @Shadow
    protected Screen screen;
    @Unique
    private FusionPackMetadata metadata;

    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    private void cacheFusionMetadata(CallbackInfo ci){
        if(this.pack instanceof PackLoadingManager.AbstractPack)
            this.metadata = ((PackExtension)((PackLoadingManager.AbstractPack)this.pack).pack).getFusionMetadata();
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/AbstractGui;blit(Lcom/mojang/blaze3d/matrix/MatrixStack;IIFFIIII)V",
            shift = At.Shift.BEFORE,
            ordinal = 0
        )
    )
    private void renderBackground(MatrixStack poseStack, int entryIndex, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTicks, CallbackInfo ci){
        if(this.metadata == null)
            return;
        ResourcePackListTipRenderer.renderBackground(this.metadata, this.pack.getCompatibility().isCompatible(), poseStack, left, top, width, height);
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/AbstractGui;blit(Lcom/mojang/blaze3d/matrix/MatrixStack;IIFFIIII)V",
            shift = At.Shift.AFTER,
            ordinal = 0
        )
    )
    private void renderIcon(MatrixStack poseStack, int entryIndex, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTicks, CallbackInfo ci){
        if(this.metadata == null)
            return;
        ResourcePackListTipRenderer.renderIcon(this.metadata, this.pack.getCompatibility().isCompatible(), poseStack, left, top, width, height);
    }

    @ModifyVariable(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/PackLoadingManager$IPack;canSelect()Z",
            shift = At.Shift.BEFORE
        )
    )
    private IBidiRenderer adjustDescription(IBidiRenderer label){
        if(this.metadata == null)
            return label;
        ITextComponent warningMessage = ResourcePackListTipRenderer.getWarningMessage(this.metadata, this.pack.getCompatibility().isCompatible());
        return warningMessage == null ? label : IBidiRenderer.create(Minecraft.getInstance().font, warningMessage, 157, 2);
    }

    @Inject(
        method = "mouseClicked",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/PackLoadingManager$IPack;getCompatibility()Lnet/minecraft/resources/PackCompatibility;",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private void showFusionWarningScreen(CallbackInfoReturnable<Boolean> ci){
        if(this.metadata == null)
            return;
        Consumer<Boolean> callback = select -> {
            Minecraft.getInstance().setScreen(this.screen);
            if(select)
                this.pack.select();
        };
        if(ResourcePackListTipRenderer.showWarningScreen(this.metadata, this.pack.getCompatibility().isCompatible(), (PackLoadingManager.AbstractPack)this.pack, callback))
            ci.setReturnValue(false);
    }
}
