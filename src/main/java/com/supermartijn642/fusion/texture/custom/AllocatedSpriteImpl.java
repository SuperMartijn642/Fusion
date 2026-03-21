package com.supermartijn642.fusion.texture.custom;


import com.supermartijn642.fusion.api.texture.custom.AllocatedSprite;
import net.minecraft.util.ResourceLocation;

/**
 * Created 20/03/2026 by SuperMartijn642
 */
public class AllocatedSpriteImpl implements AllocatedSprite {

    private final ResourceLocation identifier;
    private final int x, y, width, height;
    private final float u0, u1, v0, v1;

    public AllocatedSpriteImpl(ResourceLocation identifier, int x, int y, int width, int height, float u0, float u1, float v0, float v1){
        this.identifier = identifier;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.u0 = u0;
        this.u1 = u1;
        this.v0 = v0;
        this.v1 = v1;
    }

    @Override
    public ResourceLocation identifier(){
        return this.identifier;
    }

    @Override
    public int x(){
        return this.x;
    }

    @Override
    public int y(){
        return this.y;
    }

    @Override
    public int width(){
        return this.width;
    }

    @Override
    public int height(){
        return this.height;
    }

    @Override
    public float u0(){
        return this.u0;
    }

    @Override
    public float u1(){
        return this.u1;
    }

    @Override
    public float v0(){
        return this.v0;
    }

    @Override
    public float v1(){
        return this.v1;
    }
}
