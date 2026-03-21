package com.supermartijn642.fusion.resources;

import com.mojang.blaze3d.platform.GlStateManager;
import com.supermartijn642.fusion.Fusion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resources.ClientResourcePackInfo;
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

    public static void renderBackground(FusionPackMetadata metadata, boolean isVanillaCompatible, int x, int y, int width, int height){
        if(isVanillaCompatible && !metadata.isMinVersionSatisfied()){
            Screen.fill(x - 1, y - 1, x + width - 9, y + height + 1, 255 << 24 | 114 << 16 | 83 << 8);
            GlStateManager.color4f(1, 1, 1, 1);
        }
    }

    public static void renderIcon(FusionPackMetadata metadata, boolean isVanillaCompatible, int x, int y, int width, int height){
        GlStateManager.enableBlend();
        Minecraft.getInstance().textureManager.bind(FUSION_LOGO);
        Screen.blit(x, y, 0, 0, 12, 12, 12, 12);
        GlStateManager.disableBlend();
    }

    public static ITextComponent getWarningMessage(FusionPackMetadata metadata, boolean isVanillaCompatible){
        if(isVanillaCompatible && !metadata.isMinVersionSatisfied())
            return new TranslationTextComponent("fusion.resource_packs.requires_newer_version").withStyle(TextFormatting.GRAY);
        return null;
    }

    public static boolean showWarningScreen(FusionPackMetadata metadata, boolean isVanillaCompatible, ClientResourcePackInfo pack, Consumer<Boolean> confirmation){
        if(!isVanillaCompatible || metadata.isMinVersionSatisfied())
            return false;
        Minecraft.getInstance().setScreen(new MinimumVersionWarningScreen(pack, confirmation));
        return true;
    }
}
