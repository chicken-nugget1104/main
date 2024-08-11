package com.mojang.minecraft.gui;

import com.mojang.minecraft.gui.Button;
import com.mojang.minecraft.gui.Screen;
import java.util.ArrayList;
import java.util.List;

public class PauseScreen extends Screen {

   private List<Button> buttons = new ArrayList();

   public void init() {
      this.buttons.add(new Button(0, this.width / 2 - 100, this.height / 3 + 0, 200, 20, "Back to game"));
      this.buttons.add(new Button(1, this.width / 2 - 100, this.height / 3 + 32, 200, 20, "Quit without saving"));
   }

   protected void keyPressed(char eventCharacter, int eventKey) {}

   protected void mouseClicked(int x, int y, int buttonNum) {
      if(buttonNum == 0) {
         for(int i = 0; i < this.buttons.size(); ++i) {
            Button button = (Button)this.buttons.get(i);
            if(x >= button.x && y >= button.y && x < button.x + button.w && y < button.y + button.visible) {
               this.buttonClicked(button);
            }
         }
      }

   }

   private void buttonClicked(Button button) {
      if(button.id == 0) {
         this.minecraft.pausemenuhide();
      }

      if(button.id == 1) {
         this.minecraft.pausemenuhide();
         this.minecraft.stop();
      }
   }

   public void render(int xm, int ym) {
      this.fillGradient(0, 0, this.width, this.height, 537199872, -1607454624);

      for(int i = 0; i < this.buttons.size(); ++i) {
         Button button = (Button)this.buttons.get(i);
         this.fill(button.x - 1, button.y - 1, button.x + button.w + 1, button.y + button.visible + 1, -16777216);
         if(xm >= button.x && ym >= button.y && xm < button.x + button.w && ym < button.y + button.visible) {
            this.fill(button.x - 1, button.y - 1, button.x + button.w + 1, button.y + button.visible + 1, -6250336);
            this.fill(button.x, button.y, button.x + button.w, button.y + button.visible, -8355680);
            this.drawCenteredString(button.msg, button.x + button.w / 2, button.y + (button.visible - 8) / 2, 16777120);
         } else {
            this.fill(button.x, button.y, button.x + button.w, button.y + button.visible, -9408400);
            this.drawCenteredString(button.msg, button.x + button.w / 2, button.y + (button.visible - 8) / 2, 14737632);
         }
      }

   }
}