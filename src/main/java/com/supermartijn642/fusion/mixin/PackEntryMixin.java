package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.extensions.PackExtension;
import com.supermartijn642.fusion.resources.FusionPackMetadata;
import com.supermartijn642.fusion.resources.ResourcePackListTipRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.gui.screens.packs.TransferableSelectionList;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
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
        method = "renderContent",
        at = @At("HEAD")
    )
    private void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, boolean isHovered, float partialTicks, CallbackInfo ci){
        if(this.metadata == null)
            return;
        //noinspection DataFlowIssue
        TransferableSelectionList.PackEntry entry = (TransferableSelectionList.PackEntry)(Object)this;
        ResourcePackListTipRenderer.renderBackground(this.metadata, this.pack.getCompatibility().isCompatible(), graphics, entry.getContentX(), entry.getContentY(), entry.getContentWidth(), entry.getContentHeight());
    }

    @Inject(
        method = "renderContent",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V",
            shift = At.Shift.AFTER
        )
    )
    private void renderIcon(GuiGraphics graphics, int mouseX, int mouseY, boolean isHovered, float partialTicks, CallbackInfo ci){
        if(this.metadata == null)
            return;
        //noinspection DataFlowIssue
        TransferableSelectionList.PackEntry entry = (TransferableSelectionList.PackEntry)(Object)this;
        ResourcePackListTipRenderer.renderIcon(this.metadata, this.pack.getCompatibility().isCompatible(), graphics, entry.getContentX(), entry.getContentY(), entry.getContentWidth(), entry.getContentHeight());
    }

    @ModifyVariable(
        method = "renderContent",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/packs/PackSelectionModel$Entry;canSelect()Z",
            shift = At.Shift.BEFORE
        )
    )
    private MultiLineLabel adjustDescription(MultiLineLabel label){
        if(this.metadata == null)
            return label;
        Component warningMessage = ResourcePackListTipRenderer.getWarningMessage(this.metadata, this.pack.getCompatibility().isCompatible());
        return warningMessage == null ? label : MultiLineLabel.create(Minecraft.getInstance().font, 157, 2, warningMessage);
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
            Minecraft.getInstance().setScreen(this.parent.screen);
            if(select)
                this.pack.select();
        };
        if(ResourcePackListTipRenderer.showWarningScreen(this.metadata, this.pack.getCompatibility().isCompatible(), (PackSelectionModel.EntryBase)this.pack, callback))
            ci.cancel();
    }
}
