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

    private boolean isEmbeddiumLoaded;
    private boolean isRubidiumLoaded;

    @Override
    public void onLoad(String mixinPackage){
        this.isEmbeddiumLoaded = isClassAvailable("org.embeddedt.embeddium.api.eventbus.EmbeddiumEvent");
        this.isRubidiumLoaded = !this.isEmbeddiumLoaded && isClassAvailable("me.jellysquid.mods.sodium.client.SodiumClientMod");
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
        if(this.isEmbeddiumLoaded && mixinClassName.endsWith(".ItemRendererMixin"))
            return false;
        if(this.isRubidiumLoaded && mixinClassName.endsWith(".ItemRendererMixin"))
            return false;
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets){
    }

    @Override
    public List<String> getMixins(){
        List<String> mixins = new ArrayList<>();
        if(this.isEmbeddiumLoaded){
            mixins.add("embeddium.BlockRendererMixinEmbeddium");
            mixins.add("embeddium.SpriteContentsInterpolationDataMixinEmbeddium");
            mixins.add("embeddium.WorldSliceMixinEmbeddium");
        }
        if(this.isRubidiumLoaded){
            mixins.add("rubidium.BlockRendererMixinRubidium");
            mixins.add("rubidium.SpriteContentsInterpolationDataMixinRubidium");
            mixins.add("rubidium.WorldSliceMixinRubidium");
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
