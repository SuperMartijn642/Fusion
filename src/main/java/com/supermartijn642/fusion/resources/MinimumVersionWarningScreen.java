package com.supermartijn642.fusion.resources;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.extensions.PackExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IBidiRenderer;
import net.minecraft.client.gui.screen.PackLoadingManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ColorHelper;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;

import java.util.function.Consumer;

/**
 * Created 21/10/2024 by SuperMartijn642
 */
public class MinimumVersionWarningScreen extends Screen {

    private static final ResourceLocation FUSION_LOGO = new ResourceLocation("fusion", "textures/resourcepacks/fusion_icon.png");

    private final PackLoadingManager.AbstractPack pack;
    private final Consumer<Boolean> confirmation;
    private final ITextComponent title;
    private final int titleWidth;
    private final IReorderingProcessor packName;
    private final IBidiRenderer packDescription;
    private final IBidiRenderer headerMessage, confirmationMessage;
    private final int headerMessageWidth, confirmationMessageWidth;
    private final ITextComponent currentVersionLabel, requiredVersionLabel;
    private final ITextComponent currentVersion, requiredVersion;
    private final int versionLabelTextWidth, versionTextWidth;
    private final Button cancelButton, confirmButton;

    public MinimumVersionWarningScreen(PackLoadingManager.AbstractPack pack, Consumer<Boolean> confirmation){
        super(new TranslationTextComponent("fusion.resource_packs.warning_screen.title"));
        this.pack = pack;
        this.confirmation = confirmation;

        // Create the title
        FontRenderer font = Minecraft.getInstance().font;
        this.title = new TranslationTextComponent("fusion.resource_packs.warning_screen.title").withStyle(TextFormatting.UNDERLINE);
        this.titleWidth = font.width(this.title);

        // Cache name and description for the correct size
        int width = font.width(pack.getTitle());
        if(width > 157){
            ITextProperties croppedTitle = ITextProperties.composite(font.substrByWidth(pack.getTitle(), 157 - font.width("...")), ITextProperties.of("..."));
            this.packName = LanguageMap.getInstance().getVisualOrder(croppedTitle);
        }else
            this.packName = pack.getTitle().getVisualOrderText();
        this.packDescription = IBidiRenderer.create(font, pack.getExtendedDescription(), 157, 2);

        // Create multiline labels for messages
        this.headerMessage = IBidiRenderer.create(font, new TranslationTextComponent("fusion.resource_packs.warning_screen.message"), 220);
        this.headerMessageWidth = font.split(new TranslationTextComponent("fusion.resource_packs.warning_screen.message"), 220).stream().mapToInt(font::width).max().orElse(0);
        this.confirmationMessage = IBidiRenderer.create(font, new TranslationTextComponent("fusion.resource_packs.warning_screen.confirmation"), 220);
        this.confirmationMessageWidth = font.split(new TranslationTextComponent("fusion.resource_packs.warning_screen.confirmation"), 220).stream().mapToInt(font::width).max().orElse(0);
        this.currentVersionLabel = new TranslationTextComponent("fusion.resource_packs.warning_screen.current_version");
        this.requiredVersionLabel = new TranslationTextComponent("fusion.resource_packs.warning_screen.required_version");
        this.versionLabelTextWidth = Math.max(font.width(this.currentVersionLabel), font.width(this.requiredVersionLabel));

        // Get and format the current and required Fusion versions
        this.currentVersion = new StringTextComponent(FusionClient.getFusionVersion()).withStyle(TextFormatting.GOLD);
        this.requiredVersion = new StringTextComponent(((PackExtension)pack.pack).getFusionMetadata().getMinimumVersion()).withStyle(TextFormatting.GOLD);
        this.versionTextWidth = Math.max(font.width(this.currentVersion), font.width(this.requiredVersion));

        // Confirmation buttons
        this.cancelButton = new Button(0, 0, 80, 20, new TranslationTextComponent("fusion.resource_packs.warning_screen.cancel"), b -> confirmation.accept(false));
        this.confirmButton = new Button(0, 0, 80, 20, new TranslationTextComponent("fusion.resource_packs.warning_screen.confirm"), b -> confirmation.accept(true));
    }

    @Override
    public String getNarrationMessage(){
        return new TranslationTextComponent("fusion.resource_packs.warning_screen.message").append(new TranslationTextComponent("fusion.resource_packs.warning_screen.confirmation")).getString();
    }

    @Override
    protected void init(){
        super.init();
        this.cancelButton.x = this.width / 2 - this.cancelButton.getWidth() - 2;
        this.cancelButton.y = this.height / 2 + 110 - this.cancelButton.getHeight();
        this.confirmButton.x = this.width / 2 + 2;
        this.confirmButton.y = this.height / 2 + 110 - this.confirmButton.getHeight();
        this.addButton(this.cancelButton);
        this.addButton(this.confirmButton);
    }

    @Override
    public void render(MatrixStack poseStack, int mouseX, int mouseY, float partialTicks){
        super.renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTicks);
        poseStack.pushPose();
        poseStack.translate(this.width / 2f, this.height / 2f - 110, 0);

        // Title
        int titleLeft = -(this.titleWidth + 17) / 2;
        RenderSystem.enableBlend();
        Minecraft.getInstance().textureManager.bind(FUSION_LOGO);
        Screen.blit(poseStack, titleLeft, 0, 0, 0, 12, 12, 12, 12);
        RenderSystem.disableBlend();
        Screen.drawString(poseStack, this.font, this.title, titleLeft + 17, 2, -1);

        // Content
        poseStack.popPose();
        poseStack.pushPose();
        int middleHeight = 98 + this.headerMessage.getLineCount() * 10 + this.confirmationMessage.getLineCount() * 10;
        poseStack.translate(this.width / 2f, (this.height - middleHeight) / 2f, 0);

        Screen.fill(poseStack, -98, 0, 98, 36, ColorHelper.PackedColor.color(70, 255, 255, 255));
        Minecraft.getInstance().textureManager.bind(this.pack.getIconTexture());
        Screen.blit(poseStack, -96, 2, 0, 0, 32, 32, 32, 32);
        this.font.drawShadow(poseStack, this.packName, -62, 3, 16777215);
        this.packDescription.renderLeftAligned(poseStack, -62, 14, 10, -8355712);

        this.hLine(poseStack, -115, 115, 44, ColorHelper.PackedColor.color(255, 255, 255, 255));

        int textLeft = -Math.max(this.headerMessageWidth, this.confirmationMessageWidth) / 2;
        this.headerMessage.renderLeftAligned(poseStack, textLeft, 54, 10, -1);
        int textHeight = this.headerMessage.getLineCount() * 10;
        this.confirmationMessage.renderLeftAligned(poseStack, textLeft, 58 + textHeight, 10, -1);
        textHeight += this.confirmationMessage.getLineCount() * 10;

        this.hLine(poseStack, -115, 115, 66 + textHeight, ColorHelper.PackedColor.color(255, 255, 255, 255));

        textLeft = -(this.versionLabelTextWidth + 5 + this.versionTextWidth) / 2;
        Screen.drawString(poseStack, this.font, this.currentVersionLabel, textLeft, 76 + textHeight, ColorHelper.PackedColor.color(255, 180, 180, 180));
        Screen.drawString(poseStack, this.font, this.requiredVersionLabel, textLeft, 88 + textHeight, ColorHelper.PackedColor.color(255, 180, 180, 180));
        Screen.drawString(poseStack, this.font, this.currentVersion, textLeft + this.versionLabelTextWidth + 5, 76 + textHeight, 16777215);
        Screen.drawString(poseStack, this.font, this.requiredVersion, textLeft + this.versionLabelTextWidth + 5, 88 + textHeight, 16777215);

        poseStack.popPose();
    }

    @Override
    public boolean shouldCloseOnEsc(){
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers){
        if(keyCode == 256){ // Escape key
            this.confirmation.accept(false);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
