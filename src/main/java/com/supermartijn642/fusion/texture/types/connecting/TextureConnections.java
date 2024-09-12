package com.supermartijn642.fusion.texture.types.connecting;

import javax.annotation.Nonnull;
import java.util.Iterator;

/**
 * Created 08/09/2024 by SuperMartijn642
 */
public class TextureConnections {

    public static Iterable<TextureConnections> iterateAll(){
        return new Iterable<>() {
            @Nonnull
            @Override
            public Iterator<TextureConnections> iterator(){
                return new Iterator<>() {
                    private int index = 0;

                    @Override
                    public boolean hasNext(){
                        return this.index < 256;
                    }

                    @Override
                    public TextureConnections next(){
                        TextureConnections connections = new TextureConnections(
                            (this.index & 1) != 0,
                            ((this.index >> 1) & 1) != 0,
                            ((this.index >> 2) & 1) != 0,
                            ((this.index >> 3) & 1) != 0,
                            ((this.index >> 4) & 1) != 0,
                            ((this.index >> 5) & 1) != 0,
                            ((this.index >> 6) & 1) != 0,
                            ((this.index >> 7) & 1) != 0
                        );
                        this.index++;
                        return connections;
                    }
                };
            }
        };
    }

    public final boolean top;
    public final boolean topRight;
    public final boolean right;
    public final boolean bottomRight;
    public final boolean bottom;
    public final boolean bottomLeft;
    public final boolean left;
    public final boolean topLeft;

    public TextureConnections(boolean top, boolean topRight, boolean right, boolean bottomRight, boolean bottom, boolean bottomLeft, boolean left, boolean topLeft){
        this.top = top;
        this.topRight = topRight;
        this.right = right;
        this.bottomRight = bottomRight;
        this.bottom = bottom;
        this.bottomLeft = bottomLeft;
        this.left = left;
        this.topLeft = topLeft;
    }
}
