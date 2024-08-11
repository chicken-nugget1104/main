package com.mojang.minecraft.level;

import com.mojang.minecraft.level.ImprovedNoisen;
import com.mojang.minecraft.level.Syntha;
import java.util.Random;

public class PerlinNoisenew extends Syntha {

   private ImprovedNoisen[] noiseLevels;
   private int levels;

   // Static constants for noise manipulation
   private static final int multiplier = 1664525;
   private static final int addend = 1013904223;

   public PerlinNoisenew(int levels) {
      this(new Random(), levels);
   }

   public PerlinNoisenew(Random random, int levels) {
      this.levels = levels;
      this.noiseLevels = new ImprovedNoisen[levels];

      for (int i = 0; i < levels; ++i) {
         this.noiseLevels[i] = new ImprovedNoisen(random);
      }
   }

   @Override
   public double getValue(double x, double y) {
      // Basic implementation to satisfy the abstract method in Syntha
      return 0.0;
   }

   // Generate a heightmap
   public int[] getValue(int width, int height) {
      int[] values = new int[width * height];

      for (int y = 0; y < height; y++) {
         for (int x = 0; x < width; x++) {
            int value = 0;
            int pow = 1;

            for (int i = 0; i < this.levels; ++i) {
               // Apply a controlled perturbation to x and y using multiplier and addend
               double adjustedX = x + (x * multiplier + addend) % 1000 / 1000.0; // small perturbation
               double adjustedY = y + (y * multiplier + addend) % 1000 / 1000.0; // small perturbation

               value += this.noiseLevels[i].getValue(adjustedX / pow, adjustedY / pow) * pow;
               pow *= 2;
            }

            values[x + y * width] = value;
         }
      }

      return values;
   }
}
