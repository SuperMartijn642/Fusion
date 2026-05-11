package com.supermartijn642.fusion.model.types.connecting;

import com.google.common.collect.ImmutableList;
import com.supermartijn642.fusion.api.model.custom.CullableQuads;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.model.types.connecting.predicates.ConnectionPredicate;
import net.minecraft.util.EnumFacing;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created 06/05/2026 by SuperMartijn642
 */
public class ConnectingModelQuads {

    public static Builder builder(){
        return new Builder();
    }

    private static int cullIndex(EnumFacing direction){
        return direction == null ? 0 : direction.ordinal() + 1;
    }

    private final List<Entry>[] quads;
    private final List<Entry> all;

    public ConnectingModelQuads(List<Entry>[] quads){
        List<Entry> all = new ArrayList<>();
        //noinspection unchecked
        this.quads = new List[7];
        for(int i = 0; i < 7; i++){
            if(quads[i] == null){
                this.quads[i] = Collections.emptyList();
                continue;
            }
            this.quads[i] = ImmutableList.copyOf(quads[i]);
            all.addAll(quads[i]);
        }
        this.all = ImmutableList.copyOf(all);
    }

    public List<Entry> get(@Nullable EnumFacing cullDirection){
        return this.quads[cullIndex(cullDirection)];
    }

    public List<Entry> all(){
        return this.all;
    }

    public static class Entry {

        private final QuadAccess quad;
        private final ConnectionPredicate connectionPredicate;
        private final ConnectingBakedModel.TextureOrientation orientation;
        private final ConnectingBakedModel.QuadPredicatesKey predicatesKey;

        public Entry(QuadAccess quad, ConnectionPredicate connectionPredicate){
            this.quad = quad;
            this.connectionPredicate = connectionPredicate;
            this.orientation = ConnectingBakedModel.findOrientation(quad);
            this.predicatesKey = new ConnectingBakedModel.QuadPredicatesKey(quad.facing(), this.orientation, connectionPredicate);
        }

        public QuadAccess quad(){
            return this.quad;
        }

        public ConnectionPredicate connectionPredicate(){
            return this.connectionPredicate;
        }

        ConnectingBakedModel.TextureOrientation orientation(){
            return this.orientation;
        }

        ConnectingBakedModel.QuadPredicatesKey predicateKey(){
            return this.predicatesKey;
        }
    }

    public static class Builder {

        private final List<Entry>[] quads;

        private Builder(){
            //noinspection unchecked
            this.quads = new List[7];
        }

        public Builder add(@Nullable EnumFacing cullDirection, QuadAccess quad, ConnectionPredicate predicate){
            List<Entry> direction = this.quads[cullIndex(cullDirection)];
            if(direction == null){
                direction = new ArrayList<>();
                this.quads[cullIndex(cullDirection)] = direction;
            }
            direction.add(new Entry(quad, predicate));
            return this;
        }

        public Builder add(CullableQuads quads){
            for(EnumFacing cullDirection : EnumFacing.values()){
                for(QuadAccess quad : quads.get(cullDirection)){
                    this.add(cullDirection, quad, null);
                }
            }
            return this;
        }

        public Builder add(ConnectingModelQuads quads){
            for(EnumFacing cullDirection : EnumFacing.values()){
                for(Entry entry : quads.get(cullDirection)){
                    this.add(cullDirection, entry.quad, entry.connectionPredicate);
                }
            }
            return this;
        }

        public Builder mutateQuads(CullableQuads.QuadMutator mutator){
            MutableQuad mutableQuad = MutableQuad.create();
            for(EnumFacing cullDirection : EnumFacing.values()){
                List<Entry> quads = this.quads[cullIndex(cullDirection)];
                if(quads == null)
                    continue;
                for(int i = 0; i < quads.size(); i++){
                    Entry entry = quads.get(i);
                    mutableQuad.copyFrom(entry.quad);
                    if(mutator.mutate(cullDirection, mutableQuad))
                        quads.set(i, new Entry(mutableQuad.createCopy(), entry.connectionPredicate));
                    else
                        quads.remove(i--);
                }
            }
            return this;
        }

        public ConnectingModelQuads build(){
            return new ConnectingModelQuads(this.quads);
        }
    }
}
