package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.extensions.PackExtension;
import com.supermartijn642.fusion.resources.FusionPackMetadata;
import com.supermartijn642.fusion.resources.ResourcePackListTipRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import net.minecraft.client.resources.ResourcePackListEntry;
import net.minecraft.client.resources.ResourcePackListEntryFound;
import net.minecraft.client.resources.ResourcePackRepository;
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

import java.util.List;
import java.util.function.Consumer;

/**
 * Created 21/10/2024 by SuperMartijn642
 */
@Mixin(ResourcePackListEntry.class)
public class PackEntryMixin {

    @Final
    @Shadow
    protected GuiScreenResourcePacks resourcePacksGUI;
    @Unique
    private boolean cached = false;
    @Unique
    private ResourcePackRepository.Entry resourcePack;
    @Unique
    private FusionPackMetadata metadata;

    @Unique
    private void cacheFusionMetadata(){
        if(!this.cached){
            this.cached = true;
            //noinspection ConstantValue
            if((Object)this instanceof ResourcePackListEntryFound){
                this.resourcePack = ((ResourcePackListEntryFound)(Object)this).getResourcePackEntry();
                this.metadata = ((PackExtension)this.resourcePack).getFusionMetadata();
            }
        }
    }

    @Inject(
        method = "drawEntry",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;drawModalRectWithCustomSizedTexture(IIFFIIFF)V",
            shift = At.Shift.BEFORE,
            ordinal = 0
        )
    )
    private void renderBackground(int entryIndex, int left, int top, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTicks, CallbackInfo ci){
        this.cacheFusionMetadata();
        if(this.metadata == null)
            return;
        ResourcePackListTipRenderer.renderBackground(this.metadata, this.resourcePack.getPackFormat() == 3, left, top, width, height);
    }

    @Inject(
        method = "drawEntry",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;drawModalRectWithCustomSizedTexture(IIFFIIFF)V",
            shift = At.Shift.AFTER,
            ordinal = 0
        )
    )
    private void renderIcon(int entryIndex, int left, int top, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTicks, CallbackInfo ci){
        this.cacheFusionMetadata();
        if(this.metadata == null)
            return;
        ResourcePackListTipRenderer.renderIcon(this.metadata, this.resourcePack.getPackFormat() == 3, left, top, width, height);
    }

    @ModifyVariable(
        method = "drawEntry",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resources/ResourcePackListEntry;canMoveRight()Z",
            shift = At.Shift.BEFORE
        ),
        ordinal = 1
    )
    private String adjustDescription(String label){
        this.cacheFusionMetadata();
        if(this.metadata == null)
            return label;
        ITextComponent warningMessage = ResourcePackListTipRenderer.getWarningMessage(this.metadata, this.resourcePack.getPackFormat() == 3);
        return warningMessage == null ? label : warningMessage.getFormattedText();
    }

    @Inject(
        method = "mousePressed",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resources/ResourcePackListEntry;getResourcePackFormat()I",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private void showFusionWarningScreen(CallbackInfoReturnable<Boolean> ci){
        this.cacheFusionMetadata();
        if(this.metadata == null)
            return;
        final int targetIndex = this.resourcePacksGUI.getSelectedResourcePacks().get(0).isServerPack() ? 1 : 0;
        Consumer<Boolean> callback = select -> {
            //noinspection DataFlowIssue
            List<ResourcePackListEntry> packs = this.resourcePacksGUI.getListContaining((ResourcePackListEntry)(Object)this);
            Minecraft.getMinecraft().displayGuiScreen(this.resourcePacksGUI);
            if(select){
                //noinspection DataFlowIssue
                packs.remove((ResourcePackListEntry)(Object)this);
                //noinspection DataFlowIssue
                this.resourcePacksGUI.getSelectedResourcePacks().add(targetIndex, (ResourcePackListEntry)(Object)this);
            }
        };
        if(ResourcePackListTipRenderer.showWarningScreen(this.metadata, this.resourcePack.getPackFormat() == 3, this.resourcePack, callback))
            ci.setReturnValue(false);
    }
}
