package com.supermartijn642.fusion.model.custom;

import com.google.common.collect.ImmutableList;
import com.supermartijn642.fusion.api.model.custom.CullableQuads;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import net.minecraft.util.EnumFacing;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created 06/05/2026 by SuperMartijn642
 */
public class CullableQuadsImpl implements CullableQuads {

    public static Builder builder(){
        return new BuilderImpl();
    }

    private final List<QuadAccess> all;
    private final List<QuadAccess> unculled;
    private final List<QuadAccess> up;
    private final List<QuadAccess> down;
    private final List<QuadAccess> north;
    private final List<QuadAccess> east;
    private final List<QuadAccess> south;
    private final List<QuadAccess> west;

    private CullableQuadsImpl(List<QuadAccess> unculled, List<QuadAccess> up, List<QuadAccess> down, List<QuadAccess> north, List<QuadAccess> east, List<QuadAccess> south, List<QuadAccess> west){
        this.unculled = ImmutableList.copyOf(unculled);
        this.up = ImmutableList.copyOf(up);
        this.down = ImmutableList.copyOf(down);
        this.north = ImmutableList.copyOf(north);
        this.east = ImmutableList.copyOf(east);
        this.south = ImmutableList.copyOf(south);
        this.west = ImmutableList.copyOf(west);
        List<QuadAccess> all = new ArrayList<>(unculled.size() + up.size() + down.size() + north.size() + east.size() + south.size() + west.size());
        all.addAll(unculled);
        all.addAll(up);
        all.addAll(down);
        all.addAll(north);
        all.addAll(east);
        all.addAll(south);
        all.addAll(west);
        this.all = ImmutableList.copyOf(all);
    }

    @Override
    public List<QuadAccess> get(@Nullable EnumFacing cullDirection){
        if(cullDirection == null)
            return this.unculled;
        switch(cullDirection){
            case UP:
                return this.up;
            case DOWN:
                return this.down;
            case NORTH:
                return this.north;
            case EAST:
                return this.east;
            case SOUTH:
                return this.south;
            case WEST:
                return this.west;
        }
        throw new AssertionError();
    }

    @Override
    public List<QuadAccess> all(){
        return this.all;
    }

    @Override
    public List<QuadAccess> unculled(){
        return this.unculled;
    }

    @Override
    public List<QuadAccess> up(){
        return this.up;
    }

    @Override
    public List<QuadAccess> down(){
        return this.down;
    }

    @Override
    public List<QuadAccess> north(){
        return this.north;
    }

    @Override
    public List<QuadAccess> east(){
        return this.east;
    }

    @Override
    public List<QuadAccess> south(){
        return this.south;
    }

    @Override
    public List<QuadAccess> west(){
        return this.west;
    }

    private static class BuilderImpl implements Builder {

        private final List<QuadAccess> unculled = new ArrayList<>();
        private final Map<EnumFacing,List<QuadAccess>> culled = new EnumMap<>(EnumFacing.class);

        @Override
        public Builder add(@Nullable EnumFacing cullDirection, List<QuadAccess> quads){
            if(cullDirection == null)
                this.unculled.addAll(quads);
            else
                this.culled.computeIfAbsent(cullDirection, d -> new ArrayList<>()).addAll(quads);
            return this;
        }

        @Override
        public Builder add(@Nullable EnumFacing cullDirection, QuadAccess quad){
            this.add(cullDirection, ImmutableList.of(quad));
            return this;
        }

        @Override
        public Builder mutateQuads(QuadMutator mutator){
            MutableQuad mutableQuad = MutableQuad.create();
            for(int i = this.unculled.size() - 1; i >= 0; i--){
                mutableQuad.copyFrom(this.unculled.get(i));
                if(mutator.mutate(null, mutableQuad))
                    this.unculled.set(i, mutableQuad.createCopy());
                else
                    this.unculled.remove(i++);
            }
            for(EnumFacing cullDirection : EnumFacing.values()){
                List<QuadAccess> quads = this.culled.getOrDefault(cullDirection, Collections.emptyList());
                for(int i = quads.size() - 1; i >= 0; i--){
                    mutableQuad.copyFrom(quads.get(i));
                    if(mutator.mutate(null, mutableQuad))
                        quads.set(i, mutableQuad.createCopy());
                    else
                        quads.remove(i++);
                }
            }
            return this;
        }

        @Override
        public CullableQuads build(){
            return new CullableQuadsImpl(
                this.unculled,
                this.culled.getOrDefault(EnumFacing.UP, Collections.emptyList()),
                this.culled.getOrDefault(EnumFacing.DOWN, Collections.emptyList()),
                this.culled.getOrDefault(EnumFacing.NORTH, Collections.emptyList()),
                this.culled.getOrDefault(EnumFacing.EAST, Collections.emptyList()),
                this.culled.getOrDefault(EnumFacing.SOUTH, Collections.emptyList()),
                this.culled.getOrDefault(EnumFacing.WEST, Collections.emptyList())
            );
        }
    }
}
