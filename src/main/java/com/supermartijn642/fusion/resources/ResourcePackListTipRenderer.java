package com.supermartijn642.fusion.resources;

import com.supermartijn642.fusion.Fusion;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import java.util.function.Consumer;

/**
 * Created 21/10/2024 by SuperMartijn642
 */
public class ResourcePackListTipRenderer {

    private static final Identifier FUSION_LOGO = Fusion.identifier("textures/resourcepacks/fusion_icon_blurred.png");

    public static void renderBackground(FusionPackMetadata metadata, boolean isVanillaCompatible, GuiGraphicsExtractor graphics, int x, int y, int width, int height){
        if(isVanillaCompatible && !metadata.isMinVersionSatisfied())
            graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, ARGB.color(255, 114, 83, 0));
    }

    public static void renderIcon(FusionPackMetadata metadata, boolean isVanillaCompatible, GuiGraphicsExtractor graphics, int x, int y, int width, int height){
        graphics.blit(RenderPipelines.GUI_TEXTURED, FUSION_LOGO, x, y, 0, 0, 12, 12, 12, 12);
    }

    public static Component getWarningMessage(FusionPackMetadata metadata, boolean isVanillaCompatible){
        if(isVanillaCompatible && !metadata.isMinVersionSatisfied())
            return Component.translatable("fusion.resource_packs.requires_newer_version").withStyle(ChatFormatting.GRAY);
        return null;
    }

    public static boolean showWarningScreen(FusionPackMetadata metadata, boolean isVanillaCompatible, PackSelectionModel.EntryBase entry, Consumer<Boolean> confirmation){
        if(!isVanillaCompatible || metadata.isMinVersionSatisfied())
            return false;
        Minecraft.getInstance().gui.setScreen(new MinimumVersionWarningScreen(entry, confirmation));
        return true;
    }
}
