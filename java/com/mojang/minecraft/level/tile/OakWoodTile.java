package com.mojang.minecraft.level.tile;

import com.mojang.minecraft.level.Level;

import java.util.Random;

public class OakWoodTile extends Tile {

    /**
     * Create an oak wood tile with the id
     *
     * @param id The id of the oak wood tile
     */
    protected OakWoodTile(int id) {
        super(id);

        this.textureId = 20;
    }

    @Override
    protected int getTexture(int face) {
        // Texture mapping of the oak wood tile
        return face == 1 || face == 0 ? 21 : 20;
    }

    @Override
    public void onTick(Level level, int x, int y, int z, Random random) {
        // No sunlight dependency, oak wood does not change
    }
}
