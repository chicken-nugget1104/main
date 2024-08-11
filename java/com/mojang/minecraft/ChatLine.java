package com.mojang.minecraft;

public final class ChatLine {

   float message;
   long time;
   public int c;
   public float d;
   public float e = 1.0F;
   public float f = 0.0F;

   public ChatLine(float var1) {
      this.message = var1;
      this.time = System.nanoTime();
   }
}
