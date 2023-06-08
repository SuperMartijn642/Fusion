package com.supermartijn642.fusion.core;

import com.google.common.collect.Multimap;
import net.minecraftforge.fml.common.asm.transformers.AccessTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Created 06/09/2022 by SuperMartijn642
 */
public class FusionAccessTransformer extends AccessTransformer {

    private static final Function<Object,String> MODIFIER_NAME;
    private static final BiConsumer<Object,String> SET_MODIFIER_NAME;
    private static final Function<Object,String> MODIFIER_DESC;
    private static final Function<Object,Boolean> MODIFIER_MODIFY_CLASS_VISIBILITY;

    static{
        Class<?> modifier = AccessTransformer.class.getDeclaredClasses()[0];
        try{
            Field name = modifier.getDeclaredField("name");
            name.setAccessible(true);
            MODIFIER_NAME = o -> {
                try{
                    return (String)name.get(o);
                }catch(IllegalAccessException e){
                    throw new RuntimeException(e);
                }
            };
            SET_MODIFIER_NAME = (o, value) -> {
                try{
                    name.set(o, value);
                }catch(IllegalAccessException e){
                    throw new RuntimeException(e);
                }
            };
            Field desc = modifier.getDeclaredField("desc");
            desc.setAccessible(true);
            MODIFIER_DESC = o -> {
                try{
                    return (String)desc.get(o);
                }catch(IllegalAccessException e){
                    throw new RuntimeException(e);
                }
            };
            Field modifyClassVisibility = modifier.getDeclaredField("modifyClassVisibility");
            modifyClassVisibility.setAccessible(true);
            MODIFIER_MODIFY_CLASS_VISIBILITY = o -> {
                try{
                    return (Boolean)modifyClassVisibility.get(o);
                }catch(IllegalAccessException e){
                    throw new RuntimeException(e);
                }
            };
        }catch(NoSuchFieldException e){
            throw new RuntimeException(e);
        }
    }

    public FusionAccessTransformer() throws IOException{
        super("META-INF/fusion-accesstransformer.cfg");

        // Make the access transformer work in dev environment
        if(FMLLaunchHandler.isDeobfuscatedEnvironment())
            this.deobfuscateModifiers();
    }

    private void deobfuscateModifiers(){
        // Obtain the modifiers map
        Multimap<String,Object> modifiers;
        try{
            Field modifiersField = AccessTransformer.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            //noinspection unchecked
            modifiers = (Multimap<String,Object>)modifiersField.get(this);
        }catch(NoSuchFieldException | IllegalAccessException e){
            throw new RuntimeException(e);
        }

        // Loop over all modifiers
        for(Map.Entry<String,Object> entry : modifiers.entries()){
            String className = entry.getKey().replace('.','/');
            Object modifier = entry.getValue();

            // Class names are always the same, so they don't need to be remapped
            if(MODIFIER_MODIFY_CLASS_VISIBILITY.apply(modifier))
                continue;

            String name = MODIFIER_NAME.apply(modifier);
            String desc = MODIFIER_DESC.apply(modifier);

            // Find the deobfuscated name
            String remappedName;
            if(desc.isEmpty()){
                // Remap field name
                remappedName = FMLDeobfuscatingRemapper.INSTANCE.mapFieldName(className, name, null);
            }else{
                // Remap method name
                remappedName = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(className, name, desc);
            }

            // Set the new name
            SET_MODIFIER_NAME.accept(modifier, remappedName);
        }
    }
}
