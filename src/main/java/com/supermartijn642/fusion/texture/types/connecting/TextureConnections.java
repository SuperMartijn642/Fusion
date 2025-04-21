package com.supermartijn642.fusion.texture.types.connecting;

import javax.annotation.Nonnull;
import java.util.Iterator;

/**
 * Created 08/09/2024 by SuperMartijn642
 */
public final class TextureConnections {

    public static Iterable<TextureConnections> iterateAll(){
        return new Iterable<TextureConnections>() {
            @Nonnull
            @Override
            public Iterator<TextureConnections> iterator(){
                return new Iterator<TextureConnections>() {
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

    @Override
    public boolean equals(Object o){
        if(!(o instanceof TextureConnections)) return false;

        TextureConnections that = (TextureConnections)o;
        return this.top == that.top && this.topRight == that.topRight && this.right == that.right && this.bottomRight == that.bottomRight && this.bottom == that.bottom && this.bottomLeft == that.bottomLeft && this.left == that.left && this.topLeft == that.topLeft;
    }

    @Override
    public int hashCode(){
        int result = Boolean.hashCode(this.top);
        result = 31 * result + Boolean.hashCode(this.topRight);
        result = 31 * result + Boolean.hashCode(this.right);
        result = 31 * result + Boolean.hashCode(this.bottomRight);
        result = 31 * result + Boolean.hashCode(this.bottom);
        result = 31 * result + Boolean.hashCode(this.bottomLeft);
        result = 31 * result + Boolean.hashCode(this.left);
        result = 31 * result + Boolean.hashCode(this.topLeft);
        return result;
    }
}
