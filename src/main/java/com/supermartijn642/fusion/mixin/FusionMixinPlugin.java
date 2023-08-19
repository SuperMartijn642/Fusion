package com.supermartijn642.fusion.mixin;

import org.embeddedt.vintagefix.core.VintageFixCore;
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

    private boolean isVintageFixLoaded;

    @Override
    public void onLoad(String mixinPackage){
        try{
            MixinService.getService().getBytecodeProvider().getClassNode("org.embeddedt.vintagefix.VintageFix");
            try{
                VintageFixCore.class.getDeclaredField("FUSION");
            }catch(NoSuchFieldException e){
                this.isVintageFixLoaded = true;
            }
        }catch(Exception ignored){
            this.isVintageFixLoaded = false;
        }
    }

    @Override
    public String getRefMapperConfig(){
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName){
        return !(this.isVintageFixLoaded && mixinClassName.endsWith(".TextureAtlasMixin"));
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets){
    }

    @Override
    public List<String> getMixins(){
        return this.isVintageFixLoaded ?
            Collections.singletonList("vintagefix.TextureAtlasMixinVintageFix")
            : null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo){
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo){
    }
}
