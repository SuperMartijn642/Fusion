package com.supermartijn642.fusion.resources;

import com.supermartijn642.fusion.Fusion;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.extensions.PackExtension;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.render.state.GuiTextRenderState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2fStack;

import java.util.function.Consumer;

/**
 * Created 21/10/2024 by SuperMartijn642
 */
public class MinimumVersionWarningScreen extends Screen {

    private static final Identifier FUSION_LOGO = Fusion.identifier("textures/resourcepacks/fusion_icon.png");

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
        this.packDescription = MultiLineLabel.create(font, 157, 2, pack.getExtendedDescription());

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
        Matrix3x2fStack poseStack = graphics.pose();
        poseStack.pushMatrix();
        poseStack.translate(this.width / 2f, this.height / 2f - 110);

        // Title
        int titleLeft = -(this.titleWidth + 17) / 2;
        graphics.blit(FUSION_LOGO, titleLeft, 0, 0, 0, 12, 12, 12, 12);
        graphics.drawString(this.font, this.title, titleLeft + 17, 2, -1);

        // Content
        poseStack.popMatrix();
        poseStack.pushMatrix();
        int middleHeight = 98 + this.headerMessage.getLineCount() * 10 + this.confirmationMessage.getLineCount() * 10;
        poseStack.translate(this.width / 2f, (this.height - middleHeight) / 2f);

        graphics.fill(-98, 0, 98, 36, ARGB.color(70, 255, 255, 255));
        graphics.blit(RenderPipelines.GUI_TEXTURED, this.pack.getIconTexture(), -96, 2, 0, 0, 32, 32, 32, 32);
        graphics.drawString(this.font, this.packName, -62, 3, ARGB.color(255, 16777215));
        ActiveTextCollector dummyTextCollector = graphics.textRenderer();
        this.packDescription.visitLines(TextAlignment.LEFT, -62, 14, 10, new ActiveTextCollector() { // I want not-white text, so now I have to do this garbage
            @Override
            public Parameters defaultParameters(){
                return dummyTextCollector.defaultParameters();
            }

            @Override
            public void defaultParameters(Parameters parameters){
                dummyTextCollector.defaultParameters(parameters);
            }

            @Override
            public void accept(TextAlignment alignment, int x, int y, Parameters parameters, FormattedCharSequence text){
                int left = alignment.calculateLeft(x, Minecraft.getInstance().font, text);
                GuiTextRenderState textRenderState = new GuiTextRenderState(
                    Minecraft.getInstance().font, text, parameters.pose(), left, y, ARGB.color(parameters.opacity(), -8355712), 0, true, true, parameters.scissor()
                );
                if(ARGB.as8BitChannel(parameters.opacity()) != 0)
                    graphics.guiRenderState.submitText(textRenderState);

                ActiveTextCollector.findElementUnderCursor(textRenderState, mouseX, mouseY, s -> {});
            }

            @Override
            public void acceptScrolling(Component component, int i, int j, int k, int l, int m, Parameters parameters){
                dummyTextCollector.acceptScrolling(component, i, j, k, l, m, parameters);
            }
        });

        graphics.hLine(-115, 115, 44, ARGB.color(255, 255, 255));

        int textLeft = -Math.max(this.headerMessage.getWidth(), this.confirmationMessage.getWidth()) / 2;
        this.headerMessage.visitLines(TextAlignment.LEFT, textLeft, 54, 10, graphics.textRenderer());
        int textHeight = this.headerMessage.getLineCount() * 10;
        this.confirmationMessage.visitLines(TextAlignment.LEFT, textLeft, 58 + textHeight, 10, graphics.textRenderer());
        textHeight += this.confirmationMessage.getLineCount() * 10;

        graphics.hLine(-115, 115, 66 + textHeight, ARGB.color(255, 255, 255));

        textLeft = -(this.versionLabelTextWidth + 5 + this.versionTextWidth) / 2;
        graphics.drawString(this.font, this.currentVersionLabel, textLeft, 76 + textHeight, ARGB.color(180, 180, 180));
        graphics.drawString(this.font, this.requiredVersionLabel, textLeft, 88 + textHeight, ARGB.color(180, 180, 180));
        graphics.drawString(this.font, this.currentVersion, textLeft + this.versionLabelTextWidth + 5, 76 + textHeight, ARGB.color(255, 16777215));
        graphics.drawString(this.font, this.requiredVersion, textLeft + this.versionLabelTextWidth + 5, 88 + textHeight, ARGB.color(255, 16777215));

        poseStack.popMatrix();
    }

    @Override
    public boolean shouldCloseOnEsc(){
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent){
        if(keyEvent.isEscape()){
            this.confirmation.accept(false);
            return true;
        }
        return super.keyPressed(keyEvent);
    }
}
