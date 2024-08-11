package com.mojang.minecraft.level.tile;

import com.mojang.minecraft.level.Level;
import com.mojang.minecraft.level.Tessellator;
import com.mojang.minecraft.phys.AABB;

import java.util.Random;

public class Glass extends Tile {

    /**
     * Create a rose tile with given id
     *
     * @param id Id of the tile
     */
    protected Glass(int id) {
        super(id);

        // Set texture slot id
        this.textureId = 49;
    }

    @Override
    public void onTick(Level level, int x, int y, int z, Random random) {
        int tileIdBelow = level.getTile(x, y - 1, z);
    }

    @Override
    public void render(Tessellator tessellator, Level level, int layer, int x, int y, int z) {
        // Render in correct layer
        if (level.isLit(x, y, z) ^ layer != 1) {
            return;
        }

        // Texture id
        int textureId = this.getTexture(this.textureId);
    }

    @Override
    public boolean blocksLight() {
        return false;
    }
}
