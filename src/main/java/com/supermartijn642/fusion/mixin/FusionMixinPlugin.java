package com.supermartijn642.fusion.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created 21/06/2023 by SuperMartijn642
 */
public class FusionMixinPlugin implements IMixinConfigPlugin {

    private boolean isModernFixLoaded;
    private boolean isSodiumLoaded;
    private boolean isIndiumLoaded;

    @Override
    public void onLoad(String mixinPackage){
        this.isModernFixLoaded = isClassAvailable("org.embeddedt.modernfix.ModernFix");
        this.isSodiumLoaded = isClassAvailable("me.jellysquid.mods.sodium.client.SodiumClientMod");
        this.isIndiumLoaded = isClassAvailable("link.infra.indium.Indium");
    }

    private static boolean isClassAvailable(String className){
        try{
            MixinService.getService().getBytecodeProvider().getClassNode(className);
            return true;
        }catch(Exception ignored){
            return false;
        }
    }

    @Override
    public String getRefMapperConfig(){
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName){
        if(this.isModernFixLoaded && mixinClassName.endsWith(".TextureAtlasMixin"))
            return false;
        if(this.isSodiumLoaded && mixinClassName.endsWith(".ItemRendererMixin"))
            return false;
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets){
    }

    @Override
    public List<String> getMixins(){
        List<String> mixins = new ArrayList<>();
        if(this.isModernFixLoaded)
            mixins.add("modernfix.TextureAtlasMixinModernFix");
        if(this.isSodiumLoaded){
            mixins.add("sodium.BlockRendererMixinSodium");
            mixins.add("sodium.ItemRendererMixinSodium");
        }
        if(this.isIndiumLoaded){
            mixins.add("indium.BaseQuadRendererMixin");
            mixins.add("indium.ItemRenderContextMixinIndium");
            mixins.add("indium.TerrainRenderContextMixinIndium");
        }
        return mixins;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo){
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo){
    }
}
