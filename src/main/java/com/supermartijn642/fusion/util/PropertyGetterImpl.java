package com.supermartijn642.fusion.util;

import com.supermartijn642.fusion.api.util.Property;
import com.supermartijn642.fusion.api.util.PropertyGetter;

import java.util.Optional;

/**
 * Created 06/06/2026 by SuperMartijn642
 */
public class PropertyGetterImpl {

    private static final PropertyGetter EMPTY = new PropertyGetter() {
        @Override
        public <X, C> Optional<X> getProperty(Property<X,C> property, C context){
            return Optional.empty();
        }
    };

    public static PropertyGetter empty(){
        return EMPTY;
    }

    public static PropertyGetter compose(PropertyGetter... delegates){
        if(delegates.length == 0)
            return EMPTY;
        if(delegates.length == 1)
            return delegates[0];
        return new PropertyGetter() {
            @Override
            public <X, C> Optional<X> getProperty(Property<X,C> property, C context){
                for(PropertyGetter delegate : delegates){
                    Optional<X> result = delegate.getProperty(property, context);
                    if(result.isPresent())
                        return result;
                }
                return Optional.empty();
            }
        };
    }
}
