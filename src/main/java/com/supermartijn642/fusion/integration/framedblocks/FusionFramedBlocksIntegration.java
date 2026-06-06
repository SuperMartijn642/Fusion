package com.supermartijn642.fusion.integration.framedblocks;

import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Created 06/06/2026 by SuperMartijn642
 */
public class FusionFramedBlocksIntegration {

    private static final boolean IS_PRESENT = ModList.get().isLoaded("framedblocks");
    private static final ModelProperty<Object> CACHE_KEY_PROPERTY = new ModelProperty<>();

    public static boolean isPresent(){
        return IS_PRESENT;
    }

    public static ModelProperty<Object> getCacheKeyProperty(){
        return CACHE_KEY_PROPERTY;
    }

    public static Object getCacheProperty(ModelData modelData){
        return IS_PRESENT ? FusionFramedBlocksIntegrationImpl.getCacheValue(modelData) : null;
    }

    public static Object lazyCacheable(Supplier<?> supplier){
        return new LazyCacheable(supplier);
    }

    private static class LazyCacheable {
        private Supplier<?> supplier;
        private Object value;
        private boolean resolved;

        private LazyCacheable(Supplier<?> supplier){
            this.supplier = supplier;
        }

        private void resolve(){
            if(this.resolved)
                return;
            synchronized(this){
                if(this.resolved)
                    return;
                this.value = this.supplier.get();
                this.supplier = null;
                this.resolved = true;
            }
        }

        @Override
        public final boolean equals(Object o){
            if(!(o instanceof LazyCacheable that)) return false;

            this.resolve();
            return Objects.equals(this.value, that.value);
        }

        @Override
        public int hashCode(){
            this.resolve();
            return Objects.hashCode(this.value);
        }
    }
}
