package com.mojang.minecraft.level;

import java.util.Random;

public class PerlinNoiseFilter {

    private Random random = new Random();
    private int seed;
    private int octave;
    private static final int FUZZINESS = 16;

    /**
     * Perlin noise generator
     *
     * @param octave The strength of the noise
     */
    public PerlinNoiseFilter(int octave) {
        this.seed = random.nextInt();
        this.octave = octave;
    }

    /**
     * Read random noise values with given dimensions
     *
     * @param width  Noise width
     * @param height Noise height
     * @return Noise map
     */
    public int[] read(int width, int height) {
        int[] table = new int[width * height];
        int step = width >> this.octave;

        // Generate initial random values based on seed
        for (int y = 0; y < height; y += step) {
            for (int x = 0; x < width; x += step) {
                table[x + y * width] = (random.nextInt(256) - 128) * FUZZINESS;
            }
        }

        // Mutate values in table
        for (step = width >> this.octave; step > 1; step /= 2) {
            int max = 256 * (step << this.octave);
            int halfStep = step / 2;

            // First mutation
            for (int y = 0; y < height; y += step) {
                for (int x = 0; x < width; x += step) {
                    int value = table[x % width + y % height * width];
                    int stepValueX = table[(x + step) % width + y % height * width];
                    int stepValueY = table[x % width + (y + step) % height * width];
                    int stepValueXY = table[(x + step) % width + (y + step) % height * width];

                    int mutatedValue = (value + stepValueY + stepValueX + stepValueXY) / 4 + random.nextInt(max * 2) - max;

                    table[x + halfStep + (y + halfStep) * width] = mutatedValue;
                }
            }

            // Second mutation
            for (int y = 0; y < height; y += step) {
                for (int x = 0; x < width; x += step) {
                    int value = table[x + y * width];
                    int stepValueX = table[(x + step) % width + y * width];
                    int stepValueY = table[x + (y + step) % width * width];
                    int halfStepValueXPos = table[(x + halfStep & width - 1) + (y + halfStep - step & height - 1) * width];
                    int halfStepValueYPos = table[(x + halfStep - step & width - 1) + (y + halfStep & height - 1) * width];
                    int halfStepValue = table[(x + halfStep) % width + (y + halfStep) % height * width];

                    int mutatedValueX = (value + stepValueX + halfStepValue + halfStepValueXPos) / 4 + random.nextInt(max * 2) - max;
                    int mutatedValueY = (value + stepValueY + halfStepValue + halfStepValueYPos) / 4 + random.nextInt(max * 2) - max;

                    table[x + halfStep + y * width] = mutatedValueX;
                    table[x + (y + halfStep) * width] = mutatedValueY;
                }
            }
        }

        // Create result array
        int[] result = new int[width * height];

        // Generate output values
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[x + y * width] = table[x % width + y % height * width] / 512 + 128;
            }
        }

        return result;
    }

    /**
     * Get the seed used for generating the noise
     *
     * @return The seed value
     */
    public int getSeed() {
        return seed;
    }
}
