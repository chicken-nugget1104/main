package com.mojang.minecraft.level;

import java.util.Random;

public class FloatNoiseMap {

    Random random = new Random();
    int seed;
    float levels;
    int fuzz;

    public FloatNoiseMap(float levels) {
        this.seed = this.random.nextInt();
        this.levels = levels;
        this.fuzz = 16;
    }

    public int[] read(int width, int height) {
        Random random = new Random();
        int[] tmp = new int[width * height];
        float level = this.levels;
        float result = width >> (int) level;

        int y;
        int x;
        for(y = 0; y < height; y += result) {
            for(x = 0; x < width; x += result) {
                tmp[x + y * width] = (random.nextInt(256) - 128) * this.fuzz;
            }
        }

        for(result = width >> (int) level; result > 1; result /= 2) {
            y = (int) (256 * (result * (1 << (int) level)));
            x = (int) (result / 2);

            int y1;
            int x1;
            int c;
            int r;
            int d;
            int mu;
            int ml;
            for(y1 = 0; y1 < height; y1 += result) {
                for(x1 = 0; x1 < width; x1 += result) {
                    c = tmp[(x1 + 0) % width + (y1 + 0) % height * width];
                    r = tmp[(x1 + (int) result) % width + (y1 + 0) % height * width];
                    d = tmp[(x1 + 0) % width + (y1 + (int) result) % height * width];
                    mu = tmp[(x1 + (int) result) % width + (y1 + (int) result) % height * width];
                    ml = (c + d + r + mu) / 4 + random.nextInt(y * 2) - y;
                    tmp[x1 + x + (y1 + x) * width] = ml;
                }
            }

            for(y1 = 0; y1 < height; y1 += result) {
                for(x1 = 0; x1 < width; x1 += result) {
                    c = tmp[x1 + y1 * width];
                    r = tmp[(x1 + (int) result) % width + y1 * width];
                    d = tmp[x1 + (y1 + (int) result) % height * width];
                    mu = tmp[(x1 + x & width - 1) + (y1 + x - (int) result & height - 1) * width];
                    ml = tmp[(x1 + x - (int) result & width - 1) + (y1 + x & height - 1) * width];
                    int m = tmp[(x1 + x) % width + (y1 + x) % height * width];
                    int u = (c + r + m + mu) / 4 + random.nextInt(y * 2) - y;
                    int l = (c + d + m + ml) / 4 + random.nextInt(y * 2) - y;
                    tmp[x1 + x + y1 * width] = u;
                    tmp[x1 + (y1 + x) * width] = l;
                }
            }
        }

        int[] var19 = new int[width * height];

        for(y = 0; y < height; ++y) {
            for(x = 0; x < width; ++x) {
                var19[x + y * width] = tmp[x % width + y % height * width] / 512 + 128;
            }
        }

        return var19;
    }
}
