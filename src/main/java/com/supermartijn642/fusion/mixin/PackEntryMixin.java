package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.extensions.PackExtension;
import com.supermartijn642.fusion.resources.FusionPackMetadata;
import com.supermartijn642.fusion.resources.ResourcePackListTipRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.gui.screens.packs.TransferableSelectionList;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Created 21/10/2024 by SuperMartijn642
 */
@Mixin(TransferableSelectionList.PackEntry.class)
public class PackEntryMixin {

    @Final
    @Shadow
    private PackSelectionModel.Entry pack;
    @Final
    @Shadow
    private TransferableSelectionList parent;
    @Final
    @Shadow
    private MultiLineTextWidget descriptionWidget;
    @Unique
    private FusionPackMetadata metadata;

    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    private void cacheFusionMetadata(CallbackInfo ci){
        if(this.pack instanceof PackSelectionModel.EntryBase)
            this.metadata = ((PackExtension)((PackSelectionModel.EntryBase)this.pack).pack).getFusionMetadata();
    }

    @Inject(
        method = "extractContent",
        at = @At("HEAD")
    )
    private void renderBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean isHovered, float partialTicks, CallbackInfo ci){
        if(this.metadata == null)
            return;
        //noinspection DataFlowIssue
        TransferableSelectionList.PackEntry entry = (TransferableSelectionList.PackEntry)(Object)this;
        ResourcePackListTipRenderer.renderBackground(this.metadata, this.pack.getCompatibility().isCompatible(), graphics, entry.getContentX(), entry.getContentY(), entry.getContentWidth(), entry.getContentHeight());
    }

    @Inject(
        method = "extractContent",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V",
            shift = At.Shift.AFTER
        )
    )
    private void renderIcon(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean isHovered, float partialTicks, CallbackInfo ci){
        if(this.metadata == null)
            return;
        //noinspection DataFlowIssue
        TransferableSelectionList.PackEntry entry = (TransferableSelectionList.PackEntry)(Object)this;
        ResourcePackListTipRenderer.renderIcon(this.metadata, this.pack.getCompatibility().isCompatible(), graphics, entry.getContentX(), entry.getContentY(), entry.getContentWidth(), entry.getContentHeight());
    }

    @Inject(
        method = "extractContent",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/packs/PackSelectionModel$Entry;canSelect()Z",
            shift = At.Shift.BEFORE
        )
    )
    private void adjustDescription(CallbackInfo ci){
        if(this.metadata == null)
            return;
        Component warningMessage = ResourcePackListTipRenderer.getWarningMessage(this.metadata, this.pack.getCompatibility().isCompatible());
        if(warningMessage != null)
            this.descriptionWidget.setMessage(warningMessage);
    }

    @Inject(
        method = "handlePackSelection",
        at = @At("HEAD"),
        cancellable = true
    )
    private void showFusionWarningScreen(CallbackInfo ci){
        if(this.metadata == null)
            return;
        Consumer<Boolean> callback = select -> {
            Minecraft.getInstance().gui.setScreen(this.parent.screen);
            if(select)
                this.pack.select();
        };
        if(ResourcePackListTipRenderer.showWarningScreen(this.metadata, this.pack.getCompatibility().isCompatible(), (PackSelectionModel.EntryBase)this.pack, callback))
            ci.cancel();
    }
}
