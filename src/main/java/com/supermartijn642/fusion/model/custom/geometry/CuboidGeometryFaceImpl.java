package com.supermartijn642.fusion.model.custom.geometry;

import com.google.common.collect.ImmutableMap;
import com.supermartijn642.fusion.api.model.custom.DefaultModelProperties;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.util.Property;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.core.Direction;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created 03/05/2026 by SuperMartijn642
 */
public class CuboidGeometryFaceImpl implements CuboidModelGeometry.Face {

    public static CuboidModelGeometry.Face.Builder builder(){
        return new Builder();
    }

    public static CuboidModelGeometry.Face of(BlockElementFace face){
        return builder()
            .material(face.texture)
            .uv(face.uv.uvs == null ? null : new UV(face.uv.uvs[0], face.uv.uvs[1], face.uv.uvs[2], face.uv.uvs[3]))
            .rotation(Rotation.byAngle(face.uv.rotation))
            .cullDirection(face.cullForDirection)
            .tintIndex(face.tintIndex == -1 ? null : face.tintIndex)
            .build();
    }

    private final String material;
    private final UV uv;
    private final Rotation rotation;
    private final Direction cullDirection;
    private final Integer tintIndex;
    private final Boolean shade;
    private final Integer lightEmission;
    private final Boolean ambientOcclusion;
    private final Boolean emissive;
    private final Map<Property<?,?>,Function<?,?>> properties;

    public CuboidGeometryFaceImpl(String material, UV uv, Rotation rotation, Direction cullDirection, Integer tintIndex, Boolean shade, Integer lightEmission, Boolean ambientOcclusion, Boolean emissive, Map<Property<?,?>,Function<?,?>> properties){
        this.material = material;
        this.uv = uv;
        this.rotation = rotation;
        this.cullDirection = cullDirection;
        this.tintIndex = tintIndex;
        this.shade = shade;
        this.lightEmission = lightEmission;
        this.ambientOcclusion = ambientOcclusion;
        this.emissive = emissive;
        this.properties = properties;
    }

    @Override
    public String material(){
        return this.material;
    }

    @Override
    public UV uv(){
        return this.uv;
    }

    @Override
    public Rotation rotation(){
        return this.rotation;
    }

    @Override
    public Direction cullDirection(){
        return this.cullDirection;
    }

    @Override
    public Integer tintIndex(){
        return this.tintIndex;
    }

    @Override
    public Boolean shade(){
        return this.shade;
    }

    @Override
    public Integer lightEmission(){
        return this.lightEmission;
    }

    @Override
    public Boolean ambientOcclusion(){
        return this.ambientOcclusion;
    }

    @Override
    public Boolean emissive(){
        return this.emissive;
    }

    @Override
    public <X, C> Optional<X> getProperty(Property<X,C> property, C context){
        Function<?,?> function = this.properties.get(property);
        if(function == null && property == DefaultModelProperties.FACE_MATERIAL_KEY)
            return DefaultModelProperties.FACE_MATERIAL_KEY.cast(this.material);
        //noinspection unchecked,rawtypes
        return function == null ?
            Optional.empty() :
            Optional.ofNullable((X)((Function)function).apply(context));
    }

    private static class Builder implements CuboidModelGeometry.Face.Builder {

        private String material;
        private UV uv;
        private Rotation rotation;
        private Direction cullDirection;
        private Integer tintIndex;
        private Boolean shade;
        private Integer lightEmission;
        private Boolean ambientOcclusion;
        private Boolean emissive;
        private final ImmutableMap.Builder<Property<?,?>,Function<?,?>> properties = ImmutableMap.builder();

        @Override
        public Builder material(String key){
            this.material = key;
            return this;
        }

        @Override
        public CuboidModelGeometry.Face.Builder uv(UV uv){
            this.uv = uv;
            return this;
        }

        @Override
        public Builder rotation(Rotation quadrant){
            this.rotation = quadrant;
            return this;
        }

        @Override
        public Builder cullDirection(Direction direction){
            this.cullDirection = direction;
            return this;
        }

        @Override
        public Builder tintIndex(Integer tintIndex){
            if(tintIndex != null && tintIndex < 0)
                tintIndex = null;
            this.tintIndex = tintIndex;
            return this;
        }

        @Override
        public Builder shade(Boolean shade){
            this.shade = shade;
            return this;
        }

        @Override
        public Builder lightEmission(Integer lightEmission){
            if(lightEmission != null && (lightEmission < 0 || lightEmission > 15))
                throw new IllegalArgumentException("Light emission must be between 0 and 15, not '" + lightEmission + "'!");
            this.lightEmission = lightEmission;
            return this;
        }

        @Override
        public Builder ambientOcclusion(Boolean ambientOcclusion){
            this.ambientOcclusion = ambientOcclusion;
            return this;
        }

        @Override
        public Builder emissive(Boolean emissive){
            this.emissive = emissive;
            return this;
        }

        @Override
        public <X> Builder property(Property<X,?> property, X value){
            this.properties.put(property, p -> value);
            return this;
        }

        @Override
        public <X> Builder property(Property<X,?> property, Supplier<X> value){
            this.properties.put(property, p -> value.get());
            return this;
        }

        @Override
        public <X, C> Builder property(Property<X,C> property, Function<C,X> value){
            this.properties.put(property, value);
            return this;
        }

        @Override
        public CuboidModelGeometry.Face build(){
            if(this.material == null)
                throw new IllegalArgumentException("No material has been set!");
            return new CuboidGeometryFaceImpl(
                this.material,
                this.uv,
                this.rotation,
                this.cullDirection,
                this.tintIndex,
                this.shade,
                this.lightEmission,
                this.ambientOcclusion,
                this.emissive,
                this.properties.build()
            );
        }
    }
}
