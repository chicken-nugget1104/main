package com.mojang.minecraft.level.tile;

import com.mojang.minecraft.level.Level;

import java.util.Random;

public class Sand extends Tile {

    private static final int FALL_DELAY = 5; // Delay in ticks between each fall step
    private int fallCounter = 0;

    /**
     * Create a sand tile with the id
     *
     * @param id The id of the sand tile
     */
    protected Sand(int id) {
        super(id);

        this.textureId = 18;
    }

    @Override
    public void onTick(Level level, int x, int y, int z, Random random) {
        // Increment the fall counter
        fallCounter++;

        // Check if it's time for the sand to fall
        if (fallCounter >= FALL_DELAY) {
            // Reset the fall counter
            fallCounter = 0;

            // Check if the tile below is 0 (air)
            if (level.getTile(x, y - 1, z) == 0) {
                // Fall one block down
                level.setTile(x, y - 1, z, this.id);
                level.setTile(x, y, z, 0); // Set the original position to air
            }
        }
    }
}
