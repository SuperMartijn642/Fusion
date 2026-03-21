package com.supermartijn642.fusion.resources;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.supermartijn642.fusion.Fusion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.PackLoadingManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ColorHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.function.Consumer;

/**
 * Created 21/10/2024 by SuperMartijn642
 */
public class ResourcePackListTipRenderer {

    private static final ResourceLocation FUSION_LOGO = Fusion.identifier("textures/resourcepacks/fusion_icon_blurred.png");

    public static void renderBackground(FusionPackMetadata metadata, boolean isVanillaCompatible, MatrixStack poseStack, int x, int y, int width, int height){
        if(isVanillaCompatible && !metadata.isMinVersionSatisfied())
            Screen.fill(poseStack, x - 1, y - 1, x + width - 9, y + height + 1, ColorHelper.PackedColor.color(255, 114, 83, 0));
    }

    public static void renderIcon(FusionPackMetadata metadata, boolean isVanillaCompatible, MatrixStack poseStack, int x, int y, int width, int height){
        RenderSystem.enableBlend();
        Minecraft.getInstance().textureManager.bind(FUSION_LOGO);
        Screen.blit(poseStack, x, y, 0, 0, 12, 12, 12, 12);
        RenderSystem.disableBlend();
    }

    public static ITextComponent getWarningMessage(FusionPackMetadata metadata, boolean isVanillaCompatible){
        if(isVanillaCompatible && !metadata.isMinVersionSatisfied())
            return new TranslationTextComponent("fusion.resource_packs.requires_newer_version").withStyle(TextFormatting.GRAY);
        return null;
    }

    public static boolean showWarningScreen(FusionPackMetadata metadata, boolean isVanillaCompatible, PackLoadingManager.AbstractPack entry, Consumer<Boolean> confirmation){
        if(!isVanillaCompatible || metadata.isMinVersionSatisfied())
            return false;
        Minecraft.getInstance().setScreen(new MinimumVersionWarningScreen(entry, confirmation));
        return true;
    }
}
