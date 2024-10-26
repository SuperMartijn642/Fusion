package com.supermartijn642.fusion.resources;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;

import java.util.function.Consumer;

/**
 * Created 21/10/2024 by SuperMartijn642
 */
public class ResourcePackListTipRenderer {

    private static final ResourceLocation FUSION_LOGO = ResourceLocation.fromNamespaceAndPath("fusion", "textures/resourcepacks/fusion_icon_blurred.png");

    public static void renderBackground(FusionPackMetadata metadata, boolean isVanillaCompatible, GuiGraphics graphics, int x, int y, int width, int height){
        if(isVanillaCompatible && !metadata.isMinVersionSatisfied())
            graphics.fill(x - 1, y - 1, x + width - 3, y + height + 1, ARGB.color(255, 114, 83, 0));
    }

    public static void renderIcon(FusionPackMetadata metadata, boolean isVanillaCompatible, GuiGraphics graphics, int x, int y, int width, int height){
        RenderSystem.enableBlend();
        graphics.blit(RenderType::guiTextured, FUSION_LOGO, x, y, 0, 0, 12, 12, 12, 12);
        RenderSystem.disableBlend();
    }

    public static Component getWarningMessage(FusionPackMetadata metadata, boolean isVanillaCompatible){
        if(isVanillaCompatible && !metadata.isMinVersionSatisfied())
            return Component.translatable("fusion.resource_packs.requires_newer_version").withStyle(ChatFormatting.GRAY);
        return null;
    }

    public static boolean showWarningScreen(FusionPackMetadata metadata, boolean isVanillaCompatible, PackSelectionModel.EntryBase entry, Consumer<Boolean> confirmation){
        if(!isVanillaCompatible || metadata.isMinVersionSatisfied())
            return false;
        Minecraft.getInstance().setScreen(new MinimumVersionWarningScreen(entry, confirmation));
        return true;
    }
}
