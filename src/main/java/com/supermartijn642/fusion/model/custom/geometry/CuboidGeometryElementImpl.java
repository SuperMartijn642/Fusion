package com.supermartijn642.fusion.model.custom.geometry;

import com.google.common.collect.ImmutableMap;
import com.mojang.math.Vector3f;
import com.supermartijn642.fusion.api.model.custom.ModelProperty;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.core.Direction;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created 03/05/2026 by SuperMartijn642
 */
public class CuboidGeometryElementImpl implements CuboidModelGeometry.Element {

    public static CuboidModelGeometry.Element.Builder builder(){
        return new Builder();
    }

    public static CuboidModelGeometry.Element of(BlockElement element){
        return builder()
            .fromTo(element.from, element.to)
            .rotation(element.rotation)
            .face(Direction.UP, CuboidModelGeometry.Face.of(element.faces.get(Direction.UP)))
            .face(Direction.DOWN, CuboidModelGeometry.Face.of(element.faces.get(Direction.DOWN)))
            .face(Direction.NORTH, CuboidModelGeometry.Face.of(element.faces.get(Direction.NORTH)))
            .face(Direction.EAST, CuboidModelGeometry.Face.of(element.faces.get(Direction.EAST)))
            .face(Direction.SOUTH, CuboidModelGeometry.Face.of(element.faces.get(Direction.SOUTH)))
            .face(Direction.WEST, CuboidModelGeometry.Face.of(element.faces.get(Direction.WEST)))
            .shade(element.shade ? null : false)
            .build();
    }

    private final Vector3f from, to;
    private final BlockElementRotation rotation;
    private final Map<Direction,CuboidModelGeometry.Face> faces;
    private final Boolean shade;
    private final Integer lightEmission;
    private final Boolean emissive;
    private final Map<ModelProperty<?,?>,Function<?,?>> properties;

    private CuboidGeometryElementImpl(Vector3f from, Vector3f to, BlockElementRotation rotation, Map<Direction,CuboidModelGeometry.Face> faces, Boolean shade, Integer lightEmission, Boolean emissive, Map<ModelProperty<?,?>,Function<?,?>> properties){
        this.from = from;
        this.to = to;
        this.rotation = rotation;
        this.faces = faces;
        this.shade = shade;
        this.lightEmission = lightEmission;
        this.emissive = emissive;
        this.properties = properties;
    }

    @Override
    public Vector3f from(){
        return this.from;
    }

    @Override
    public Vector3f to(){
        return this.to;
    }

    @Override
    public BlockElementRotation rotation(){
        return this.rotation;
    }

    @Override
    public CuboidModelGeometry.Face face(Direction side){
        return this.faces.get(side);
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
    public Boolean emissive(){
        return this.emissive;
    }

    @Override
    public <X, C> Optional<X> getProperty(ModelProperty<X,C> property, C context){
        Function<?,?> function = this.properties.get(property);
        //noinspection unchecked,rawtypes
        return function == null ?
            Optional.empty() :
            Optional.ofNullable((X)((Function)function).apply(context));
    }

    private static class Builder implements CuboidModelGeometry.Element.Builder {

        private Vector3f from, to;
        private BlockElementRotation rotation;
        private final Map<Direction,CuboidModelGeometry.Face> faces = new EnumMap<>(Direction.class);
        private Boolean shade;
        private Integer lightEmission;
        private Boolean emissive;
        private final ImmutableMap.Builder<ModelProperty<?,?>,Function<?,?>> properties = ImmutableMap.builder();

        @Override
        public Builder fromTo(Vector3f from, Vector3f to){
            this.from = from;
            this.to = to;
            return this;
        }

        @Override
        public Builder rotation(BlockElementRotation rotation){
            this.rotation = rotation;
            return this;
        }

        @Override
        public Builder face(Direction side, CuboidModelGeometry.Face face){
            if(face == null)
                this.faces.remove(side);
            else
                this.faces.put(side, face);
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
                throw new IllegalArgumentException("Light emission must be between 0 and 15!");
            this.lightEmission = lightEmission;
            return this;
        }

        @Override
        public Builder emissive(Boolean emissive){
            this.emissive = emissive;
            return this;
        }

        @Override
        public <X> Builder property(ModelProperty<X,?> property, X value){
            this.properties.put(property, p -> value);
            return this;
        }

        @Override
        public <X> Builder property(ModelProperty<X,?> property, Supplier<X> value){
            this.properties.put(property, p -> value.get());
            return this;
        }

        @Override
        public <X, C> Builder property(ModelProperty<X,C> property, Function<C,X> value){
            this.properties.put(property, value);
            return this;
        }

        @Override
        public CuboidModelGeometry.Element build(){
            if(this.from == null || this.to == null)
                throw new IllegalStateException("Element from and to positions have not been set!");
            if(this.faces.isEmpty())
                throw new IllegalStateException("Element must have at least one face!");
            return new CuboidGeometryElementImpl(
                this.from, this.to,
                this.rotation,
                this.faces,
                this.shade,
                this.lightEmission,
                this.emissive,
                this.properties.build()
            );
        }
    }
}
