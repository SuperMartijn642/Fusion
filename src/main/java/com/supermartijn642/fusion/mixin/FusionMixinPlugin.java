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

    private boolean isSodiumLoaded;

    @Override
    public void onLoad(String mixinPackage){
        this.isSodiumLoaded = isClassAvailable("net.caffeinemc.mods.sodium.client.SodiumClientMod");
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
        if(this.isSodiumLoaded){
            mixins.add("sodium.BlockRendererMixin");
            mixins.add("sodium.ExtendedBlockModelFeatureRendererMixinSodium");
            mixins.add("sodium.ExtendedItemFeatureRendererMixinSodium");
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
