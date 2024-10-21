package com.supermartijn642.fusion.resources;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.extensions.PackExtension;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;

import java.util.function.Consumer;

/**
 * Created 21/10/2024 by SuperMartijn642
 */
public class MinimumVersionWarningScreen extends Screen {

    private static final ResourceLocation FUSION_LOGO = new ResourceLocation("fusion", "textures/resourcepacks/fusion_icon.png");

    private final PackSelectionModel.EntryBase pack;
    private final Consumer<Boolean> confirmation;
    private final Component title;
    private final int titleWidth;
    private final FormattedCharSequence packName;
    private final MultiLineLabel packDescription;
    private final MultiLineLabel headerMessage, confirmationMessage;
    private final Component currentVersionLabel, requiredVersionLabel;
    private final Component currentVersion, requiredVersion;
    private final int versionLabelTextWidth, versionTextWidth;
    private final Button confirmButton, cancelButton;

    public MinimumVersionWarningScreen(PackSelectionModel.EntryBase pack, Consumer<Boolean> confirmation){
        super(Component.translatable("fusion.resource_packs.warning_screen.title"));
        this.pack = pack;
        this.confirmation = confirmation;

        // Create the title
        Font font = Minecraft.getInstance().font;
        this.title = Component.translatable("fusion.resource_packs.warning_screen.title").withStyle(ChatFormatting.UNDERLINE);
        this.titleWidth = font.width(this.title);

        // Cache name and description for the correct size
        int width = font.width(pack.getTitle());
        if(width > 157){
            FormattedText croppedTitle = FormattedText.composite(font.substrByWidth(pack.getTitle(), 157 - font.width("...")), FormattedText.of("..."));
            this.packName = Language.getInstance().getVisualOrder(croppedTitle);
        }else
            this.packName = pack.getTitle().getVisualOrderText();
        this.packDescription = MultiLineLabel.create(font, pack.getExtendedDescription(), 157, 2);

        // Create multiline labels for messages
        this.headerMessage = MultiLineLabel.create(font, Component.translatable("fusion.resource_packs.warning_screen.message"), 220);
        this.confirmationMessage = MultiLineLabel.create(font, Component.translatable("fusion.resource_packs.warning_screen.confirmation"), 220);
        this.currentVersionLabel = Component.translatable("fusion.resource_packs.warning_screen.current_version");
        this.requiredVersionLabel = Component.translatable("fusion.resource_packs.warning_screen.required_version");
        this.versionLabelTextWidth = Math.max(font.width(this.currentVersionLabel), font.width(this.requiredVersionLabel));

        // Get and format the current and required Fusion versions
        this.currentVersion = Component.literal(FusionClient.getFusionVersion()).withStyle(ChatFormatting.GOLD);
        this.requiredVersion = Component.literal(((PackExtension)pack.pack).getFusionMetadata().getMinimumVersion()).withStyle(ChatFormatting.GOLD);
        this.versionTextWidth = Math.max(font.width(this.currentVersion), font.width(this.requiredVersion));

        // Confirmation buttons
        this.confirmButton = Button.builder(Component.translatable("fusion.resource_packs.warning_screen.confirm"), b -> confirmation.accept(true)).width(80).build();
        this.cancelButton = Button.builder(Component.translatable("fusion.resource_packs.warning_screen.cancel"), b -> confirmation.accept(false)).width(80).build();
    }

    @Override
    public Component getNarrationMessage(){
        return Component.translatable("fusion.resource_packs.warning_screen.message").append(Component.translatable("fusion.resource_packs.warning_screen.confirmation"));
    }

    @Override
    protected void init(){
        super.init();
        this.confirmButton.setPosition(this.width / 2 - this.confirmButton.getWidth() - 2, this.height / 2 + 110 - this.confirmButton.getHeight());
        this.cancelButton.setPosition(this.width / 2 + 2, this.height / 2 + 110 - this.cancelButton.getHeight());
        this.addRenderableWidget(this.confirmButton);
        this.addRenderableWidget(this.cancelButton);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks){
        super.render(graphics, mouseX, mouseY, partialTicks);
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(this.width / 2f, this.height / 2f - 110, 0);

        // Title
        int titleLeft = -(this.titleWidth + 17) / 2;
        RenderSystem.enableBlend();
        graphics.blit(FUSION_LOGO, titleLeft, 0, 0, 0, 12, 12, 12, 12);
        RenderSystem.disableBlend();
        graphics.drawString(this.font, this.title, titleLeft + 17, 2, -1);

        // Content
        poseStack.popPose();
        poseStack.pushPose();
        int middleHeight = 98 + this.headerMessage.getLineCount() * 10 + this.confirmationMessage.getLineCount() * 10;
        poseStack.translate(this.width / 2f, (this.height - middleHeight) / 2f, 0);

        graphics.fill(-98, 0, 98, 36, FastColor.ARGB32.color(70, 255, 255, 255));
        graphics.blit(this.pack.getIconTexture(), -96, 2, 0, 0, 32, 32, 32, 32);
        graphics.drawString(this.font, this.packName, -62, 3, 16777215);
        this.packDescription.renderLeftAligned(graphics, -62, 14, 10, -8355712);

        graphics.hLine(-115, 115, 44, FastColor.ARGB32.color(255, 255, 255));

        int textLeft = -Math.max(this.headerMessage.getWidth(), this.confirmationMessage.getWidth()) / 2;
        this.headerMessage.renderLeftAligned(graphics, textLeft, 54, 10, -1);
        int textHeight = this.headerMessage.getLineCount() * 10;
        this.confirmationMessage.renderLeftAligned(graphics, textLeft, 58 + textHeight, 10, -1);
        textHeight += this.confirmationMessage.getLineCount() * 10;

        graphics.hLine(-115, 115, 66 + textHeight, FastColor.ARGB32.color(255, 255, 255));

        textLeft = -(this.versionLabelTextWidth + 5 + this.versionTextWidth) / 2;
        graphics.drawString(this.font, this.currentVersionLabel, textLeft, 76 + textHeight, FastColor.ARGB32.color(180, 180, 180));
        graphics.drawString(this.font, this.requiredVersionLabel, textLeft, 88 + textHeight, FastColor.ARGB32.color(180, 180, 180));
        graphics.drawString(this.font, this.currentVersion, textLeft + this.versionLabelTextWidth + 5, 76 + textHeight, 16777215);
        graphics.drawString(this.font, this.requiredVersion, textLeft + this.versionLabelTextWidth + 5, 88 + textHeight, 16777215);

        poseStack.popPose();
    }

    @Override
    public boolean shouldCloseOnEsc(){
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers){
        if(keyCode == InputConstants.KEY_ESCAPE){
            this.confirmation.accept(false);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
