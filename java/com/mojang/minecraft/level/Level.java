package com.mojang.minecraft.level;

import com.mojang.minecraft.level.tile.Tile;
import com.mojang.minecraft.level.PerlinNoise;
import com.mojang.minecraft.phys.AABB;
import com.mojang.minecraft.Entity;
import com.mojang.minecraft.User;
import com.mojang.minecraft.level.LevelListener;
import com.mojang.minecraft.level.tile.Tile;
import com.mojang.minecraft.Minecraft;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class Level {

    private static final int TILE_UPDATE_INTERVAL = 400;
    public final int width;
    public final int height;
    public final int depth;

    public int[] blocks;
    private int[] lightDepths;

    private final ArrayList<LevelListener> levelListeners = new ArrayList<>();
    private final Random random = new Random();
    private boolean mapLoaded = false;
    public User user = new User("noname");
    int unprocessed = 0;

    // Noise stuff?!?!?!
    private static final int multiplier = 1664525;
    private static final int addend = 1013904223;

    /**
     * Three dimensional level containing all tiles
     *
     * @param width  Level width
     * @param height Level height
     * @param depth  Level depth
     */
    public Level(int width, int height, int depth) {
        this.width = width;
        this.height = height;
        this.depth = depth;

        this.blocks = new int[width * height * depth];
        this.lightDepths = new int[width * height];

        mapLoaded = checkLevelDat();
        System.out.println("Is map loaded? " + mapLoaded);

        // Generate a new level if file doesn't exists.
        if (!mapLoaded) {
            generateMap();
        
            // Use the blocks array as needed
            System.out.println("Map generated with blocks array of size: " + blocks.length);
        }

        // Load existing level if file does exist.
        if (mapLoaded) {
            load();
        }

        // Calculate light depth of the entire level
        calcLightDepths(0, 0, width, height);
    }

    /**
     * Generate a new level
     */
    public int[] generateMap() {
        int[] firstHeightMap = new PerlinNoisenew(0).getValue(this.width, this.height);
        int[] secondHeightMap = new PerlinNoisenew(0).getValue(this.width, this.height);
        int[] cliffMap = new FloatNoiseMap(0.5F).read(this.width, this.height);
        int[] rockMap = new NoiseMap(1).read(this.width, this.height);
        double[][][] caveMap = generateCaveMap();

        // Generate tiles
        for (int x = 0; x < this.width; ++x) {
            for (int y = 0; y < this.depth; ++y) {
                for (int z = 0; z < this.height; ++z) {
                    // Extract values from height map
                    int firstHeightValue = firstHeightMap[x + z * this.width];
                    int secondHeightValue = secondHeightMap[x + z * this.width];

                    // Change the height map
                    if (cliffMap[x + z * this.width] < 128) {
                        secondHeightValue = firstHeightValue;
                    }

                    // Get max level height at this position
                    int maxLevelHeight = Math.max(secondHeightValue, firstHeightValue) / 8 + this.depth / 3;

                    // Get end of rock layer
                    int maxRockHeight = rockMap[x + z * this.width] / 8 + this.depth / 3;

                    // Keep it below the max height of the level
                    if (maxRockHeight > maxLevelHeight - 2) {
                        maxRockHeight = maxLevelHeight - 2;
                    }

                    // Get block array index
                    int index = (y * this.height + z) * this.width + x;

                    int id = 0;

                    // Carve out caves based on cave map, avoid surface caves and world edges
                    if (y < maxLevelHeight - 5 && caveMap[x][y][z] < -0.1 && x > 1 && x < width - 2 && z > 1 && z < height - 2) { // Lower threshold for cave presence
                        if (this.blocks[index] == Tile.bedrock.id) {
                            continue;
                        }

                        this.blocks[index] = 0; // Set to air
                        continue;
                    }

                    // Grass layer
                    if (y == maxLevelHeight) {
                        id = Tile.grass.id;

                        // Check for rose above grass
                        if (y + 1 < this.depth) {
                            this.blocks[index + this.width] = Tile.rose.id;
                        }

                        if (y + 1 < this.depth && random.nextDouble() < 0.05) {
                            this.blocks[index + this.width] = Tile.rose.id;
                        }
                    }

                    // Dirt layer
                    if (y < maxLevelHeight) {
                        id = Tile.dirt.id;
                    }

                    // Rock layer
                    if (y <= maxRockHeight) {
                        id = Tile.rock.id;
                    }

                    // Set the tile id
                    this.blocks[index] = (byte) id;
                }
            }
        }

        System.out.println("Done generating base tiles!");

        // Place trees
        placeTrees();

        System.out.println("Done generating caves!");

        // Add bedrock
        addBedrock();
        
        save();

        return this.blocks;
    }

    private void placeTrees() {
        for (int x = 0; x < this.width; ++x) {
            for (int z = 0; z < this.height; ++z) {
                for (int y = 0; y < this.depth; ++y) {
                    int index = (y * this.height + z) * this.width + x;

                    if (this.blocks[index] == Tile.grass.id) {
                        // 0.5% chance to place a tree
                        if (random.nextDouble() < 0.005) {
                            generateTree(x, y + 1, z); // Place tree above the grass block
                        }

                        // 1% chance to place a rose cluster
                        if (random.nextDouble() < 0.01) {
                            generateRoseCluster(x, y + 1, z); // Place roses above the grass block
                        }
                    }
                }
            }
        }
    }

    private void generateTree(int x, int y, int z) {
        // Tree trunk
        for (int i = 0; i < 5; i++) {
            if (y + i < this.depth) {
                int index = ((y + i) * this.height + z) * this.width + x;
                this.blocks[index] = Tile.wood.id;
            }
        }

        // Tree leaves
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 3; dy <= 5; dy++) {
                    if (Math.abs(dx) + Math.abs(dz) <= 3) {
                        int leafX = x + dx;
                        int leafY = y + dy;
                        int leafZ = z + dz;

                        if (leafX >= 0 && leafX < this.width && leafY >= 0 && leafY < this.depth && leafZ >= 0 && leafZ < this.height) {
                            int index = (leafY * this.height + leafZ) * this.width + leafX;
                            this.blocks[index] = Tile.leaf.id;
                        }
                    }
                }
            }
        }
    }

    private void generateRoseCluster(int x, int y, int z) {
        int clusterSize = random.nextInt(2) + 1; // Random cluster size between 1 and 2
        int gen = 0;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int i = 0; i < clusterSize; i++) {
                    int roseX = x + dx;
                    int roseY = y + i;
                    int roseZ = z + dz;

                    if (roseX >= 0 && roseX < this.width && roseY >= 0 && roseY < this.depth && roseZ >= 0 && roseZ < this.height) {
                        int index = (roseY * this.height + roseZ) * this.width + roseX;
                        if (this.blocks[index] == 0 && gen != 2) { // Only place roses in air blocks
                            this.blocks[index] = Tile.rose.id;
                            gen += 1;
                        }
                    }
                }
            }
        }
    }

    private void addCaves() {
        for (int x = 0; x < this.width; x++) {
            for (int z = 0; z < this.height; z++) {
                // Generate cave openings with variable size and randomness
                if (random.nextDouble() < 0.05) {
                    int caveHeight = random.nextInt(10) + 5; // Cave height between 5 and 15
                    int caveWidth = random.nextInt(10) + 5;  // Cave width between 5 and 15
                    int caveDepth = random.nextInt(5) + 5;   // Cave depth between 5 and 10
                    int caveStartY = random.nextInt(this.depth - 10) + 5; // Start caves not too close to top or bottom

                    for (int y = caveStartY; y < caveStartY + caveHeight && y < this.depth - 5; y++) {
                        for (int dx = -caveWidth / 2; dx < caveWidth / 2; dx++) {
                            for (int dz = -caveWidth / 2; dz < caveWidth / 2; dz++) {
                                int nx = x + dx;
                                int nz = z + dz;

                                if (nx >= 0 && nx < this.width && nz >= 0 && nz < this.height) {
                                    int index = (y * this.height + nz) * this.width + nx;
                                    this.blocks[index] = 0; // Set to air
                                }
                            }
                        }
                    }
                }
            }
        }

        System.out.println("Done putting caves in.");
    }

    private void addBedrock() {
        for (int x = 0; x < this.width; x++) {
            for (int z = 0; z < this.height; z++) {
                // Fully bedrock at the bottom layer
                int bottomIndex = (0 * this.height + z) * this.width + x;
                this.blocks[bottomIndex] = (byte) Tile.bedrock.id;

                // Randomly add bedrock above the bottom layer
                for (int y = 1; y < 3; y++) {
                    if (random.nextDouble() < 0.5) { // Random chance to place bedrock
                        int index = (y * this.height + z) * this.width + x;
                        this.blocks[index] = (byte) Tile.bedrock.id;
                    }
                }
            }
        }

        System.out.println("Done adding the bedrock layer!");
    }

/**
 * Generate 3D Perlin noise for caves
 */
private float[][][] generateCaveNoise(int width, int depth, int height, float scale) {
    float[][][] noise = new float[width][depth][height];
    PerlinNoise perlin = new PerlinNoise();

    for (int x = 0; x < width; x++) {
        for (int y = 0; y < depth; y++) {
            for (int z = 0; z < height; z++) {
                noise[x][y][z] = (float) perlin.noise(x * scale, y * scale, z * scale);
            }
        }
    }

    return noise;
}

    private double[][][] generateCaveMap() {
        double[][][] caveMap = new double[this.width][this.depth][this.height];

        for (int x = 0; x < this.width; ++x) {
            for (int y = 0; y < this.depth; ++y) {
                for (int z = 0; z < this.height; ++z) {
                    caveMap[x][y][z] = random.nextGaussian() * 0.2; // Reduced variance for smaller caves
                }
            }
        }

        return caveMap;
    }

    /**
     * Generate a new level FOR THE THING
     */
    public void generateMape() {
        generateMap();

        // Save the newly generated level
        save();
    }

    /**
     * Load blocks from level.dat
     *
     * @return true if loaded successfully, false otherwise
     */
    public boolean load() {
        try (DataInputStream dis = new DataInputStream(new GZIPInputStream(new FileInputStream("level.dat")))) {
            for (int i = 0; i < blocks.length; i++) {
                blocks[i] = dis.readInt();
            }

            calcLightDepths(0, 0, width, height);

            // Notify all tiles changed
            for (LevelListener levelListener : levelListeners) {
                levelListener.allChanged();
            }

            System.out.println("Done loading level.dat");
            // System.out.println(Arrays.toString(blocks));

            return true;
        } catch (FileNotFoundException e) {
            System.err.println("level.dat not found. Generating new level...");
            generateMap();
            return false; // File not found, generate new level
        } catch (EOFException eof) {
            System.err.println("EOFException: End of file reached unexpectedly. Generating new level...");
            generateMap();
            return false; // File empty or corrupted, generate new level
        } catch (IOException e) {
            System.err.println("IOException while loading level:");
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.err.println("Unexpected error while loading level:");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Store blocks in level.dat
     */
    public void save() {
        try {
            DataOutputStream dos = new DataOutputStream(new GZIPOutputStream(new FileOutputStream("level.dat")));

            for (int block : blocks) {
                dos.writeInt(block);
            }

            System.out.println("Done saving to level.dat");
            // System.out.println(Arrays.toString(blocks));

            dos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Calculate light depth of given area
     *
     * @param minX Minimum on X axis
     * @param minZ Minimum on Z axis
     * @param maxX Maximum on X axis
     * @param maxZ Maximum on Z axis
     */
    private void calcLightDepths(int minX, int minZ, int maxX, int maxZ) {
        // For each x/z position in level
        for (int x = minX; x < minX + maxX; x++) {
            for (int z = minZ; z < minZ + maxZ; z++) {

                // Get previous light depth value
                int prevDepth = this.lightDepths[x + z * this.width];

                // Calculate new light depth
                int depth = this.depth - 1;
                while (depth > 0 && !isLightBlocker(x, depth, z)) {
                    depth--;
                }

                // Set new light depth
                this.lightDepths[x + z * this.width] = depth;

                // On light depth change
                if (prevDepth != depth) {
                    // Get changed range
                    int minTileChangeY = Math.min(prevDepth, depth);
                    int maxTileChangeY = Math.max(prevDepth, depth);

                    // Notify tile column changed
                    for (LevelListener levelListener : this.levelListeners) {
                        levelListener.lightColumnChanged(x, z, minTileChangeY, maxTileChangeY);
                    }
                }
            }
        }
    }

    /**
     * Return true if a tile is available at the given location
     *
     * @param x Level position x
     * @param y Level position y
     * @param z Level position z
     * @return Tile available
     */
    public boolean isTile(int x, int y, int z) {
        // Is location out of the level?
        if (x < 0 || y < 0 || z < 0 || x >= this.width || y >= this.depth || z >= this.height) {
            return false;
        }

        // Calculate index from x, y and z
        int index = (y * this.height + z) * this.width + x;

        // Return true if there is a tile at this location
        return this.blocks[index] != 0;
    }

    /**
     * Return the id of the tile at the given location
     *
     * @param x Level position x
     * @param y Level position y
     * @param z Level position z
     * @return Tile id at this location
     */
    public int getTile(int x, int y, int z) {
        // Is location out of the level?
        if (x < 0 || y < 0 || z < 0 || x >= this.width || y >= this.depth || z >= this.height) {
            return 0;
        }

        // Calculate index from x, y and z
        int index = (y * this.height + z) * this.width + x;

        // Return tile id
        return this.blocks[index];
    }

    /**
     * Returns true if tile is solid and not transparent
     *
     * @param x Tile position x
     * @param y Tile position y
     * @param z Tile position z
     * @return Tile is solid
     */
    public boolean isSolidTile(int x, int y, int z) {
        Tile tile = Tile.tiles[this.getTile(x, y, z)];
        return tile != null && tile.isSolid();
    }

    /**
     * Returns true if the tile is blocking the light
     *
     * @param x Tile position x
     * @param y Tile position y
     * @param z Tile position z
     * @return Tile blocks the light
     */
    public boolean isLightBlocker(final int x, final int y, final int z) {
        Tile tile = Tile.tiles[this.getTile(x, y, z)];
        return tile != null && tile.blocksLight();
    }

    /**
     * Get bounding box of all tiles surrounded by the given bounding box
     *
     * @param boundingBox Target bounding box located in the level
     * @return List of bounding boxes representing the tiles around the given bounding box
     */
    public ArrayList<AABB> getCubes(AABB boundingBox) {
        ArrayList<AABB> boundingBoxList = new ArrayList<>();

        int minX = (int) (Math.floor(boundingBox.minX) - 1);
        int maxX = (int) (Math.ceil(boundingBox.maxX) + 1);
        int minY = (int) (Math.floor(boundingBox.minY) - 1);
        int maxY = (int) (Math.ceil(boundingBox.maxY) + 1);
        int minZ = (int) (Math.floor(boundingBox.minZ) - 1);
        int maxZ = (int) (Math.ceil(boundingBox.maxZ) + 1);

        // Minimum level position
        minX = Math.max(0, minX);
        minY = Math.max(0, minY);
        minZ = Math.max(0, minZ);

        // Maximum level position
        maxX = Math.min(this.width, maxX);
        maxY = Math.min(this.depth, maxY);
        maxZ = Math.min(this.height, maxZ);

        // Include all surrounding tiles
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {

                    // Get tile this location
                    Tile tile = Tile.tiles[this.getTile(x, y, z)];
                    if (tile != null) {

                        // Get bounding box of the the tile
                        AABB aabb = tile.getAABB(x, y, z);
                        if (aabb != null) {
                            boundingBoxList.add(aabb);
                        }
                    }
                }
            }
        }
        return boundingBoxList;
    }

    /**
     * Set tile at position
     *
     * @param x  Tile position x
     * @param y  Tile position y
     * @param z  Tile position z
     * @param id Type of tile
     * @return Tile changed
     */
    public boolean setTile(int x, int y, int z, int id) {
        // Check if position is out of level
        if (x < 0 || y < 0 || z < 0 || x >= this.width || y >= this.depth || z >= this.height) {
            return false;
        }

        // Get index of this position
        int index = (y * this.height + z) * this.width + x;

        // Check if type changed
        if (id == this.blocks[index])
            return false;

        // Set tile
        this.blocks[index] = id;

        // Update lightning
        this.calcLightDepths(x, z, 1, 1);

        // Notify tile changed
        for (LevelListener levelListener : this.levelListeners) {
            levelListener.tileChanged(x, y, z);
        }

        return true;
    }

    /**
     * Register a level listener
     *
     * @param levelListener Listener interface
     */
    public void addListener(LevelListener levelListener) {
        this.levelListeners.add(levelListener);
    }

    /**
     * Check if the given tile position is in the sun
     *
     * @param x Tile position x
     * @param y Tile position y
     * @param z Tile position z
     * @return Tile is in the sun
     */
    public boolean isLit(int x, int y, int z) {
        return x < 0 || y < 0 || z < 0 || x >= this.width || y >= this.depth || z >= this.height || y >= this.lightDepths[x + z * this.width];
    }

    /**
     * Tick a random tile in the level
     */
    public void onTick() {
        // Amount of tiles in this level
        int totalTiles = this.width * this.height * this.depth;

        // Amount of tiles to process for this tick
        int ticks = totalTiles / 400;

        // Tick multiple tiles in one game tick
        for (int i = 0; i < ticks; ++i) {
            // Get random position of the tile
            int x = this.random.nextInt(this.width);
            int y = this.random.nextInt(this.depth);
            int z = this.random.nextInt(this.height);

            // Get tile type
            Tile tile = Tile.tiles[this.getTile(x, y, z)];
            if (tile != null) {
                // Tick tile
                tile.onTick(this, x, y, z, this.random);
            }
        }
    }

    public static boolean checkLevelDat() {
        File levelDat = new File("level.dat");
        return levelDat.exists();
    }
}
