package com.supermartijn642.fusion.resources;

import com.mojang.blaze3d.platform.GlStateManager;
import com.supermartijn642.fusion.Fusion;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.extensions.PackExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.ClientResourcePackInfo;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created 21/10/2024 by SuperMartijn642
 */
public class MinimumVersionWarningScreen extends Screen {

    private static final ResourceLocation FUSION_LOGO = Fusion.identifier("textures/resourcepacks/fusion_icon.png");

    private final ClientResourcePackInfo pack;
    private final Consumer<Boolean> confirmation;
    private final String title;
    private final int titleWidth;
    private final String packName;
    private final List<String> packDescription;
    private final List<String> headerMessage, confirmationMessage;
    private final int headerMessageWidth, confirmationMessageWidth;
    private final String currentVersionLabel, requiredVersionLabel;
    private final String currentVersion, requiredVersion;
    private final int versionLabelTextWidth, versionTextWidth;
    private final Button confirmButton, cancelButton;

    public MinimumVersionWarningScreen(ClientResourcePackInfo pack, Consumer<Boolean> confirmation){
        super(new TranslationTextComponent("fusion.resource_packs.warning_screen.title"));
        this.pack = pack;
        this.confirmation = confirmation;

        // Create the title
        FontRenderer font = Minecraft.getInstance().font;
        this.title = new TranslationTextComponent("fusion.resource_packs.warning_screen.title").withStyle(TextFormatting.UNDERLINE).getColoredString();
        this.titleWidth = font.width(this.title);

        // Cache name and description for the correct size
        String title = pack.getTitle().getColoredString();
        int width = font.width(title);
        if(width > 157)
            title = font.substrByWidth(title, 157 - font.width("...")) + "...";
        this.packName = title;
        List<String> lines = font.split(pack.getDescription().getColoredString(), 157);
        this.packDescription = lines.size() > 2 ? lines.subList(0, 2) : lines;

        // Create multiline labels for messages
        this.headerMessage = font.split(new TranslationTextComponent("fusion.resource_packs.warning_screen.message").getColoredString(), 220);
        this.headerMessageWidth = this.headerMessage.stream().mapToInt(font::width).max().orElse(0);
        this.confirmationMessage = font.split(new TranslationTextComponent("fusion.resource_packs.warning_screen.confirmation").getColoredString(), 220);
        this.confirmationMessageWidth = this.confirmationMessage.stream().mapToInt(font::width).max().orElse(0);
        this.currentVersionLabel = new TranslationTextComponent("fusion.resource_packs.warning_screen.current_version").getColoredString();
        this.requiredVersionLabel = new TranslationTextComponent("fusion.resource_packs.warning_screen.required_version").getColoredString();
        this.versionLabelTextWidth = Math.max(font.width(this.currentVersionLabel), font.width(this.requiredVersionLabel));

        // Get and format the current and required Fusion versions
        this.currentVersion = new StringTextComponent(FusionClient.getFusionVersion()).withStyle(TextFormatting.GOLD).getColoredString();
        this.requiredVersion = new StringTextComponent(((PackExtension)pack).getFusionMetadata().getMinimumVersion()).withStyle(TextFormatting.GOLD).getColoredString();
        this.versionTextWidth = Math.max(font.width(this.currentVersion), font.width(this.requiredVersion));

        // Confirmation buttons
        this.confirmButton = new Button(0, 0, 80, 20, new TranslationTextComponent("fusion.resource_packs.warning_screen.confirm").getColoredString(), b -> confirmation.accept(true));
        this.cancelButton = new Button(0, 0, 80, 20, new TranslationTextComponent("fusion.resource_packs.warning_screen.cancel").getColoredString(), b -> confirmation.accept(false));
    }

    @Override
    public String getNarrationMessage(){
        return new TranslationTextComponent("fusion.resource_packs.warning_screen.message").append(new TranslationTextComponent("fusion.resource_packs.warning_screen.confirmation")).getString();
    }

    @Override
    protected void init(){
        super.init();
        this.confirmButton.x = this.width / 2 - this.confirmButton.getWidth() - 2;
        this.confirmButton.y = this.height / 2 + 110 - this.confirmButton.getHeight();
        this.cancelButton.x = this.width / 2 + 2;
        this.cancelButton.y = this.height / 2 + 110 - this.cancelButton.getHeight();
        this.addButton(this.confirmButton);
        this.addButton(this.cancelButton);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks){
        super.renderBackground();
        super.render(mouseX, mouseY, partialTicks);
        GlStateManager.pushMatrix();
        GlStateManager.translatef(this.width / 2f, this.height / 2f - 110, 0);

        // Title
        int titleLeft = -(this.titleWidth + 17) / 2;
        GlStateManager.enableBlend();
        Minecraft.getInstance().textureManager.bind(FUSION_LOGO);
        Screen.blit(titleLeft, 0, 0, 0, 12, 12, 12, 12);
        GlStateManager.disableBlend();
        this.drawString(this.font, this.title, titleLeft + 17, 2, -1);

        // Content
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        int middleHeight = 98 + this.headerMessage.size() * 10 + this.confirmationMessage.size() * 10;
        GlStateManager.translatef(this.width / 2f, (this.height - middleHeight) / 2f, 0);

        Screen.fill(-98, 0, 98, 36, 70 << 24 | 255 << 16 | 255 << 8 | 255);
        this.pack.bindIcon(Minecraft.getInstance().textureManager);
        Screen.blit(-96, 2, 0, 0, 32, 32, 32, 32);
        this.font.drawShadow(this.packName, -62, 3, 16777215);
        for(int i = 0; i < this.packDescription.size(); i++)
            this.drawString(this.font, this.packDescription.get(i), -62, 14 + i * 10, -8355712);

        this.hLine(-115, 115, 44, 255 << 24 | 255 << 16 | 255 << 8 | 255);

        int textLeft = -Math.max(this.headerMessageWidth, this.confirmationMessageWidth) / 2;
        for(int i = 0; i < this.headerMessage.size(); i++)
            this.drawString(this.font, this.headerMessage.get(i), textLeft, 54 + i * 10, -1);
        int textHeight = this.headerMessage.size() * 10;
        for(int i = 0; i < this.confirmationMessage.size(); i++)
            this.drawString(this.font, this.confirmationMessage.get(i), textLeft, 58 + textHeight + i * 10, -1);
        textHeight += this.confirmationMessage.size() * 10;

        this.hLine(-115, 115, 66 + textHeight, 255 << 24 | 255 << 16 | 255 << 8 | 255);

        textLeft = -(this.versionLabelTextWidth + 5 + this.versionTextWidth) / 2;
        this.drawString(this.font, this.currentVersionLabel, textLeft, 76 + textHeight, 255 << 24 | 180 << 16 | 180 << 8 | 180);
        this.drawString(this.font, this.requiredVersionLabel, textLeft, 88 + textHeight, 255 << 24 | 180 << 16 | 180 << 8 | 180);
        this.drawString(this.font, this.currentVersion, textLeft + this.versionLabelTextWidth + 5, 76 + textHeight, 16777215);
        this.drawString(this.font, this.requiredVersion, textLeft + this.versionLabelTextWidth + 5, 88 + textHeight, 16777215);

        GlStateManager.popMatrix();
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
