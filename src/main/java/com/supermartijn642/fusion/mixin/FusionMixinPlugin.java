package com.supermartijn642.fusion.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created 21/06/2023 by SuperMartijn642
 */
public class FusionMixinPlugin implements IMixinConfigPlugin {

    private boolean isModernFixLoaded;

    @Override
    public void onLoad(String mixinPackage){
        try{
            MixinService.getService().getBytecodeProvider().getClassNode("org.embeddedt.modernfix.ModernFix");
            this.isModernFixLoaded = true;
        }catch(Exception ignored){
            this.isModernFixLoaded = false;
        }
    }

    @Override
    public String getRefMapperConfig(){
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName){
        return !(this.isModernFixLoaded && mixinClassName.endsWith(".TextureAtlasMixin"));
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets){
    }

    @Override
    public List<String> getMixins(){
        return this.isModernFixLoaded ?
            Collections.singletonList("modernfix.TextureAtlasMixinModernFix")
            : null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo){
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo){
    }
}
