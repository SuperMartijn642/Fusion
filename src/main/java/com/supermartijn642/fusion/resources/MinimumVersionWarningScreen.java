package com.supermartijn642.fusion.resources;

import com.supermartijn642.fusion.Fusion;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.extensions.PackExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created 21/10/2024 by SuperMartijn642
 */
public class MinimumVersionWarningScreen extends GuiScreen {

    private static final ResourceLocation FUSION_LOGO = Fusion.identifier("textures/resourcepacks/fusion_icon.png");

    private final ResourcePackRepository.Entry pack;
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
    private final GuiOptionButton confirmButton, cancelButton;

    public MinimumVersionWarningScreen(ResourcePackRepository.Entry pack, Consumer<Boolean> confirmation){
        super();
        this.pack = pack;
        this.confirmation = confirmation;

        // Create the title
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        this.title = new TextComponentTranslation("fusion.resource_packs.warning_screen.title").setStyle(new Style().setColor(TextFormatting.UNDERLINE)).getFormattedText();
        this.titleWidth = font.getStringWidth(this.title);

        // Cache name and description for the correct size
        String title = pack.getResourcePackName();
        int width = font.getStringWidth(title);
        if(width > 157)
            title = font.trimStringToWidth(title, 157 - font.getStringWidth("...")) + "...";
        this.packName = title;
        List<String> lines = font.listFormattedStringToWidth(pack.getTexturePackDescription(), 157);
        this.packDescription = lines.size() > 2 ? lines.subList(0, 2) : lines;

        // Create multiline labels for messages
        this.headerMessage = font.listFormattedStringToWidth(new TextComponentTranslation("fusion.resource_packs.warning_screen.message").getFormattedText(), 220);
        this.headerMessageWidth = this.headerMessage.stream().mapToInt(font::getStringWidth).max().orElse(0);
        this.confirmationMessage = font.listFormattedStringToWidth(new TextComponentTranslation("fusion.resource_packs.warning_screen.confirmation").getFormattedText(), 220);
        this.confirmationMessageWidth = this.confirmationMessage.stream().mapToInt(font::getStringWidth).max().orElse(0);
        this.currentVersionLabel = new TextComponentTranslation("fusion.resource_packs.warning_screen.current_version").getFormattedText();
        this.requiredVersionLabel = new TextComponentTranslation("fusion.resource_packs.warning_screen.required_version").getFormattedText();
        this.versionLabelTextWidth = Math.max(font.getStringWidth(this.currentVersionLabel), font.getStringWidth(this.requiredVersionLabel));

        // Get and format the current and required Fusion versions
        this.currentVersion = new TextComponentString(FusionClient.getFusionVersion()).setStyle(new Style().setColor(TextFormatting.GOLD)).getFormattedText();
        this.requiredVersion = new TextComponentString(((PackExtension)pack).getFusionMetadata().getMinimumVersion()).setStyle(new Style().setColor(TextFormatting.GOLD)).getFormattedText();
        this.versionTextWidth = Math.max(font.getStringWidth(this.currentVersion), font.getStringWidth(this.requiredVersion));

        // Confirmation buttons
        this.confirmButton = new GuiOptionButton(0, 80, 20, new TextComponentTranslation("fusion.resource_packs.warning_screen.confirm").getFormattedText());
        this.cancelButton = new GuiOptionButton(1, 80, 20, new TextComponentTranslation("fusion.resource_packs.warning_screen.cancel").getFormattedText());
    }

    @Override
    public void initGui(){
        super.initGui();
        this.confirmButton.x = this.width / 2 - this.confirmButton.width - 2;
        this.confirmButton.y = this.height / 2 + 110 - this.confirmButton.height;
        this.cancelButton.x = this.width / 2 + 2;
        this.cancelButton.y = this.height / 2 + 110 - this.cancelButton.height;
        this.addButton(this.confirmButton);
        this.addButton(this.cancelButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException{
        if(button == this.confirmButton)
            this.confirmation.accept(true);
        else if(button == this.cancelButton)
            this.confirmation.accept(false);
        else
            super.actionPerformed(button);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks){
        super.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        GlStateManager.pushMatrix();
        GlStateManager.translate(this.width / 2f, this.height / 2f - 110, 0);

        // Title
        int titleLeft = -(this.titleWidth + 17) / 2;
        GlStateManager.enableBlend();
        Minecraft.getMinecraft().getTextureManager().bindTexture(FUSION_LOGO);
        Gui.drawModalRectWithCustomSizedTexture(titleLeft, 0, 0, 0, 12, 12, 12, 12);
        GlStateManager.disableBlend();
        this.drawString(this.fontRenderer, this.title, titleLeft + 17, 2, -1);

        // Content
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        int middleHeight = 98 + this.headerMessage.size() * 10 + this.confirmationMessage.size() * 10;
        GlStateManager.translate(this.width / 2f, (this.height - middleHeight) / 2f, 0);

        Gui.drawRect(-98, 0, 98, 36, 70 << 24 | 255 << 16 | 255 << 8 | 255);
        this.pack.bindTexturePackIcon(Minecraft.getMinecraft().getTextureManager());
        Gui.drawModalRectWithCustomSizedTexture(-96, 2, 0, 0, 32, 32, 32, 32);
        this.fontRenderer.drawStringWithShadow(this.packName, -62, 3, 16777215);
        for(int i = 0; i < this.packDescription.size(); i++)
            this.drawString(this.fontRenderer, this.packDescription.get(i), -62, 14 + i * 10, -8355712);

        this.drawHorizontalLine(-115, 115, 44, 255 << 24 | 255 << 16 | 255 << 8 | 255);

        int textLeft = -Math.max(this.headerMessageWidth, this.confirmationMessageWidth) / 2;
        for(int i = 0; i < this.headerMessage.size(); i++)
            this.drawString(this.fontRenderer, this.headerMessage.get(i), textLeft, 54 + i * 10, -1);
        int textHeight = this.headerMessage.size() * 10;
        for(int i = 0; i < this.confirmationMessage.size(); i++)
            this.drawString(this.fontRenderer, this.confirmationMessage.get(i), textLeft, 58 + textHeight + i * 10, -1);
        textHeight += this.confirmationMessage.size() * 10;

        this.drawHorizontalLine(-115, 115, 66 + textHeight, 255 << 24 | 255 << 16 | 255 << 8 | 255);

        textLeft = -(this.versionLabelTextWidth + 5 + this.versionTextWidth) / 2;
        this.drawString(this.fontRenderer, this.currentVersionLabel, textLeft, 76 + textHeight, 255 << 24 | 180 << 16 | 180 << 8 | 180);
        this.drawString(this.fontRenderer, this.requiredVersionLabel, textLeft, 88 + textHeight, 255 << 24 | 180 << 16 | 180 << 8 | 180);
        this.drawString(this.fontRenderer, this.currentVersion, textLeft + this.versionLabelTextWidth + 5, 76 + textHeight, 16777215);
        this.drawString(this.fontRenderer, this.requiredVersion, textLeft + this.versionLabelTextWidth + 5, 88 + textHeight, 16777215);

        GlStateManager.popMatrix();
    }

    @Override
    protected void keyTyped(char c, int keyCode) throws IOException{
        if(keyCode == 1){ // Escape key
            this.confirmation.accept(false);
            return;
        }
        super.keyTyped(c, keyCode);
    }
}
