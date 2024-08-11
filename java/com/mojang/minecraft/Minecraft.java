package com.mojang.minecraft;

import com.mojang.minecraft.character.Zombie;
import com.mojang.minecraft.level.*;
import com.mojang.minecraft.level.tile.Tile;
import com.mojang.minecraft.gui.Font;
import com.mojang.minecraft.gui.PauseScreen;
import com.mojang.minecraft.gui.Screen;
import com.mojang.minecraft.particle.ParticleEngine;
import com.mojang.minecraft.phys.AABB;
import com.mojang.minecraft.Textures;
import com.mojang.minecraft.User;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import javax.swing.*;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.Canvas;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.time.OffsetDateTime;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.util.glu.GLU.gluPerspective;
import static org.lwjgl.util.glu.GLU.gluPickMatrix;

import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.RichPresence;

public class Minecraft implements Runnable {

    private final Timer timer = new Timer(20);
    public static final String VERSION_STRING = "0.0.3A-PUB2";
    private String fpsString = "";

    private Level level;
    private LevelRenderer levelRenderer;
    private Player player;
    private int yMouseAxis = 1;
    private boolean verboseMode = false;

    private final List<Zombie> zombies = new ArrayList<>();
    public int MAX_ZOMBIES = 255;
    private ParticleEngine particleEngine;
    public Textures textures;
    public User user = new User("noname");

    private boolean shouldTakeScreenshot = false;
    private Tile[] tileSelectionOrder = {Tile.grass, Tile.rock, Tile.dirt, Tile.stoneBrick, Tile.wood, Tile.bush, Tile.rose, Tile.sand, Tile.brick};

    private static final long CLIENT_ID = 1255593789746319360L;
    private static IPCClient client;

    public int SIZE_X = 256;
    public int SIZE_Z = 64;

    public Screen screen = null;

    /**
     * Fog
     */
    private final FloatBuffer fogColorDaylight = BufferUtils.createFloatBuffer(4);
    private final FloatBuffer fogColorShadow = BufferUtils.createFloatBuffer(4);
    private final FloatBuffer colorBuffer = BufferUtils.createFloatBuffer(16);

    /**
     * Tile picking
     */
    private final IntBuffer viewportBuffer = BufferUtils.createIntBuffer(16);
    private final IntBuffer selectBuffer = BufferUtils.createIntBuffer(2000);
    private HitResult hitResult;

    /**
     * HUD rendering
     */
    private final Tessellator tessellator = new Tessellator();
    public Font font;
    private int editMode = 0;

    /**
     * Selected tile in hand
     */
    private int selectedTileId = Tile.grass.id;

    /**
     * Canvas
     */
    public final Canvas parent;
    public int width;
    public int height;
    private final boolean fullscreen;
    public boolean appletMode;

    /**
     * Game state
     */
    public volatile boolean running;
    public volatile boolean pause = false;

    /**
     * Create Minecraft instance and render it on a canvas
     *
     * @param parent     The canvas as render target
     * @param width      Canvas width
     * @param height     Canvas height
     * @param fullscreen Is in fullscreen
     */
    public Minecraft(Canvas parent, int width, int height, boolean fullscreen) {
        this.parent = parent;
        this.width = width;
        this.height = height;
        this.fullscreen = fullscreen;
        this.textures = new Textures();
    }

    /**
     * Initialize the game.
     * Setup display, keyboard, mouse, rendering and camera
     *
     * @throws LWJGLException Game could not be initialized
     */
    public void init() throws LWJGLException {
        // Write fog color for daylight
        this.fogColorDaylight.put(new float[]{
                254.0F / 255.0F,
                251.0F / 255.0F,
                250.0F / 255.0F,
                255.0F / 255.0F
        }).flip();

        // Write fog color for shadow
        this.fogColorShadow.put(new float[]{
                14.0F / 255.0F,
                11.0F / 255.0F,
                10.0F / 255.0F,
                255.0F / 255.0F
        }).flip();

        if (this.parent == null) {
            if (this.fullscreen) {
                // Set in fullscreen
                Display.setFullscreen(true);

                // Set monitor size
                this.width = Display.getDisplayMode().getWidth();
                this.height = Display.getDisplayMode().getHeight();
            } else {
                // Set defined window size
                Display.setDisplayMode(new DisplayMode(this.width, this.height));
            }
        } else {
            // Set canvas parent
            Display.setParent(this.parent);
        }

        // Setup I/O
        Display.create();
        Keyboard.create();
        Mouse.create();

        // Initialize Discord Rich Presence
        initDiscordRPC();

        // Setup game name
        Display.setTitle("Classicircle 0.0.3A-PUB2");

        // Setup texture and color
        glEnable(GL_TEXTURE_2D);
        glShadeModel(GL_SMOOTH);
        glClearColor(0.5F, 0.8F, 1.0F, 0.0F);
        glClearDepth(1.0);

        // Setup depth
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glDepthFunc(GL_LEQUAL);

        // Setup alpha
        glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, 0.5F);

        // Setup camera
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glMatrixMode(GL_MODELVIEW);

        // Create level and player (Has to be in main thread)
        this.level = new Level(SIZE_X, SIZE_X, SIZE_Z);
        this.levelRenderer = new LevelRenderer(this.level);
        this.player = new Player(this.level);
        this.particleEngine = new ParticleEngine(this.level);
        this.font = new Font("/default.gif", this.textures);

        // Grab mouse cursor
        Mouse.setGrabbed(true);

        // Spawn some zombies
        for (int i = 0; i < 10; ++i) {
            if(zombies.size() == 254)
            {
            }
            else
            {
                    Zombie zombie = new Zombie(this.level, 128.0F, 0.0F, 129.0F);
                    zombie.resetPosition();
                    this.zombies.add(zombie);
            }
        }
    }

    /**
     * Destroy mouse, keyboard and display
     */
    public void destroy() {
        this.level.save();

        Mouse.destroy();
        Keyboard.destroy();
        Display.destroy();
    }

    /**
     * Main game thread
     * Responsible for the game loop
     */
    @Override
    public void run() {
        // Game is running
        this.running = true;

        try {
            // Initialize the game
            init();
        } catch (Exception e) {
            // Show error message dialog and stop the game
            JOptionPane.showMessageDialog(null, e, "Failed to start Minecraft", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        // To keep track of framerate
        int frames = 0;
        long lastTime = System.currentTimeMillis();

        try {
            // Start the game loop
            while (this.running) {
                // On close window
                if (this.parent == null && Display.isCloseRequested()) {
                    this.stop();
                }

                // Update the timer
                this.timer.advanceTime();

                // Call the tick to reach updates 20 per seconds
                for (int i = 0; i < this.timer.ticks; ++i) {
                    onTick();
                }

                // Render the game
                render(this.timer.partialTicks);

                // Increase rendered frame
                frames++;

                // Loop if a second passed
                while (System.currentTimeMillis() >= lastTime + 1000L) {
                    // Show FPS chunk updates and zombie size.
                    this.fpsString = frames + " FPS, " + Chunk.updates + " Chunk Updates";

                    // Reset global rebuild stats
                    Chunk.updates = 0;

                    // Increase last time printed and reset frame counter
                    lastTime += 1000L;
                    frames = 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Destroy I/O and save game
            destroy();
        }
    }

    /**
     * Stop the game
     */
    public void stop() {
        this.running = false;
    }

    /**
     * Set the game's favicon
     */
    private static void setFavicon() {
        try {
            // Load the favicon.ico file from resources
            InputStream iconStream = Minecraft.class.getResourceAsStream("/favicon.ico");
            if (iconStream != null) {
                BufferedImage image = ImageIO.read(iconStream);
                if (image != null) {
                    Display.setIcon(new ByteBuffer[]{iconToByteBuffer(image)});
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static ByteBuffer iconToByteBuffer(BufferedImage image) throws IOException {
        int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());

        ByteBuffer buffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4);

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = pixels[y * image.getWidth() + x];
                buffer.put((byte) ((pixel >> 16) & 0xFF));  // Red component
                buffer.put((byte) ((pixel >> 8) & 0xFF));   // Green component
                buffer.put((byte) (pixel & 0xFF));          // Blue component
                buffer.put((byte) ((pixel >> 24) & 0xFF));  // Alpha component
            }
        }

        buffer.flip();
        return buffer;
    }

    public void setScreen(Screen screen) {
        this.screen = screen;
        if(screen != null) {
            int screenWidth = this.width * 240 / this.height;
            int screenHeight = this.height * 240 / this.height;
            screen.init(this, screenWidth, screenHeight);
        }
    }

    public void pausemenushow() {
        this.setScreen(new PauseScreen());
        Mouse.setGrabbed(false);
    }

    public void pausemenuhide() {
        this.setScreen((Screen)null);
        Mouse.setGrabbed(true);
    }

    /**
     * Game tick, called exactly 20 times per second
     */
    private void onTick() {
        // Listen for keyboard inputs
        while (Keyboard.next()) {
            if (Keyboard.getEventKeyState()) {
                int key = Keyboard.getEventKey(); //this.setScreen(new PauseScreen());

                // Pause game
                if (key == 1) { // Escape
                    pausemenushow();
                }

                // Save the level
                if (key == 28) { // Enter
                    this.level.save();
                }

                // Make new level
                if (key == Keyboard.KEY_N) { // Disabled for being laggy.
                    // this.level.generateMape();
                }

                // Tile selection
                if (key == 2) { // 1
                    this.selectedTileId = Tile.grass.id;
                }
                if (key == 3) { // 2
                    this.selectedTileId = Tile.rock.id;
                }
                if (key == 4) { // 3
                    this.selectedTileId = Tile.dirt.id;
                }
                if (key == 5) { // 4
                    this.selectedTileId = Tile.stoneBrick.id;
                }
                if (key == 6) { // 5
                    this.selectedTileId = Tile.woodplank.id;
                }
                if (key == 7) { // 6
                    this.selectedTileId = Tile.bush.id;
                }
                if (key == 8) { // 7
                    this.selectedTileId = Tile.rose.id;
                }
                if (key == 9) { // 8
                    this.selectedTileId = Tile.sand.id;
                }
                if (key == 10) { // 9
                    this.selectedTileId = Tile.brick.id;
                }

                // Invert mouse input on Y axis
                if (key == 21) {
                    this.yMouseAxis *= -1;
                }

                // Spawn zombie
                if (key == 34) { // G
                    this.zombies.add(new Zombie(this.level, this.player.x, this.player.y, this.player.z));
                }

                // Max zombie stuff
                if (key == Keyboard.KEY_F5) {
                    MAX_ZOMBIES--;
                }

                if (key == Keyboard.KEY_F6) {
                    MAX_ZOMBIES++;
                }
            }
        }

        // Handle scroll wheel for tile selection
        while (Mouse.next()) {
            //if(!this.mouseGrabbed && Mouse.getEventButtonState()) {
            //    this.grabMouse();
            //} else {
            //    if(Mouse.getEventButton() == 0 && Mouse.getEventButtonState()) {
            //        this.handleMouseClick();
            //}

            if(Mouse.getEventButton() == 1 && Mouse.getEventButtonState()) {
                this.editMode = (this.editMode + 1) % 2;
            }

            if (Mouse.getEventDWheel() != 0) {
                // Scroll wheel event
                int scrollDirection = Mouse.getEventDWheel();
                if (scrollDirection > 0) {
                    // Scroll up
                    this.selectedTileId++;
                    if (this.selectedTileId >= tileSelectionOrder.length) {
                        this.selectedTileId = 0; // Wrap around to the first tile
                    }
                } else if (scrollDirection < 0) {
                    // Scroll down
                    this.selectedTileId--;
                    if (this.selectedTileId < 0) {
                        this.selectedTileId = tileSelectionOrder.length - 1; // Wrap around to the last tile
                    }
                }
            }
        }

        if(this.screen != null) {
            this.screen.updateEvents();
            if(this.screen != null) {
                this.screen.tick();
            }
        }

        // Tick random tile in level
        this.level.onTick();

        // Tick particles
        this.particleEngine.onTick();

        // Tick zombies
        Iterator<Zombie> iterator = this.zombies.iterator();
        while (iterator.hasNext()) {
            Zombie zombie = iterator.next();

            // Tick zombie
            zombie.onTick();

            // Remove zombie
            if (zombie.removed) {
                iterator.remove();
            }
        }

        // Tick player
        this.player.onTick();

        // Check if the number of zombies exceeds the cap
        if (this.zombies.size() > MAX_ZOMBIES) {
            Iterator<Zombie> newZombiesIterator = this.zombies.iterator();
            int count = 0;
            while (newZombiesIterator.hasNext()) {
                Zombie zombie = newZombiesIterator.next();
                count++;
                if (count > MAX_ZOMBIES) {
                    newZombiesIterator.remove();
                }
            }
        }
    }

    /**
     * Move and rotate the camera to players location and rotation
     *
     * @param partialTicks Overflow ticks to interpolate
     */
    private void moveCameraToPlayer(float partialTicks) {
        Entity player = this.player;

        // Eye height
        glTranslatef(0.0f, 0.0f, -0.3f);

        // Rotate camera
        glRotatef(player.xRotation, 1.0f, 0.0f, 0.0f);
        glRotatef(player.yRotation, 0.0f, 1.0f, 0.0f);

        // Smooth movement
        double x = this.player.prevX + (this.player.x - this.player.prevX) * partialTicks;
        double y = this.player.prevY + (this.player.y - this.player.prevY) * partialTicks;
        double z = this.player.prevZ + (this.player.z - this.player.prevZ) * partialTicks;

        // Move camera to players location
        glTranslated(-x, -y, -z);
    }

    /**
     * Setup the normal player camera
     *
     * @param partialTicks Overflow ticks to interpolate
     */
    private void setupCamera(float partialTicks) {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();

        // Set camera perspective
        gluPerspective(70, width / (float) height, 0.05F, 1000F);

        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        // Move camera to middle of level
        moveCameraToPlayer(partialTicks);
    }

    /**
     * Setup tile picking camera
     *
     * @param partialTicks Overflow ticks to calculate smooth a movement
     * @param x            Screen position x
     * @param y            Screen position y
     */
    private void setupPickCamera(float partialTicks, int x, int y) {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();

        // Reset buffer
        this.viewportBuffer.clear();

        // Get viewport value
        glGetInteger(GL_VIEWPORT, this.viewportBuffer);

        // Flip
        this.viewportBuffer.flip();
        this.viewportBuffer.limit(16);

        // Set matrix and camera perspective
        gluPickMatrix(x, y, 5.0F, 5.0F, this.viewportBuffer);
        gluPerspective(70.0F, this.width / (float) this.height, 0.05F, 1000.0F);

        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        // Move camera to middle of level
        moveCameraToPlayer(partialTicks);
    }

    /**
     * @param partialTicks Overflow ticks to interpolate
     */
    private void pick(float partialTicks) {
        // Reset select buffer
        this.selectBuffer.clear();

        glSelectBuffer(this.selectBuffer);
        glRenderMode(GL_SELECT);

        // Setup pick camera
        this.setupPickCamera(partialTicks, this.width / 2, this.height / 2);

        // Render all possible pick selection faces to the target
        this.levelRenderer.pick(this.player, Frustum.getFrustum());

        // Flip buffer
        this.selectBuffer.flip();
        this.selectBuffer.limit(this.selectBuffer.capacity());

        long closest = 0L;
        int[] names = new int[10];
        int hitNameCount = 0;

        // Get amount of hits
        int hits = glRenderMode(GL_RENDER);
        for (int hitIndex = 0; hitIndex < hits; hitIndex++) {

            // Get name count
            int nameCount = this.selectBuffer.get();
            long minZ = this.selectBuffer.get();
            this.selectBuffer.get();

            // Check if the hit is closer to the camera
            if (minZ < closest || hitIndex == 0) {
                closest = minZ;
                hitNameCount = nameCount;

                // Fill names
                for (int nameIndex = 0; nameIndex < nameCount; nameIndex++) {
                    names[nameIndex] = this.selectBuffer.get();
                }
            } else {
                // Skip names
                for (int nameIndex = 0; nameIndex < nameCount; ++nameIndex) {
                    this.selectBuffer.get();
                }
            }
        }

        // Update hit result
        if (hitNameCount > 0) {
            this.hitResult = new HitResult(names[0], names[1], names[2], names[3], names[4]);
        } else {
            this.hitResult = null;
        }
    }


    /**
     * Rendering the game
     *
     * @param partialTicks Overflow ticks to interpolate
     */
    private void render(float partialTicks) {
        // Get mouse motion
        float motionX = Mouse.getDX();
        float motionY = Mouse.getDY();

        // Fix mouse issues in applet mode
        if (this.appletMode) {
            // Update mouse data
            Display.processMessages();
            Mouse.poll();

            // Calculate mouse motion
            motionX = (float) (Mouse.getX() - this.width / 2);
            motionY = (float) (Mouse.getY() - this.height / 2);

            // Reset cursor position
            Mouse.setCursorPosition(this.width / 2, this.height / 2);
        }

        // Rotate the camera using the mouse motion input
        this.player.turn(motionX, motionY * this.yMouseAxis);

        // Pick tile
        pick(partialTicks);

        // Listen for mouse inputs
        while (Mouse.next()) {
            // Right click
            if (Mouse.getEventButton() == 1 && Mouse.getEventButtonState() && this.hitResult != null) {
                Tile previousTile = Tile.tiles[this.level.getTile(this.hitResult.x, this.hitResult.y, this.hitResult.z)];

                // Destroy the tile
                boolean tileChanged = this.level.setTile(this.hitResult.x, this.hitResult.y, this.hitResult.z, 0);

                // Create particles for this tile
                if (previousTile != null && tileChanged) {
                    previousTile.onDestroy(this.level, this.hitResult.x, this.hitResult.y, this.hitResult.z, this.particleEngine);
                }
            }

            // Left click
            if (Mouse.getEventButton() == 0 && Mouse.getEventButtonState() && this.hitResult != null) {
                // Get target tile position
                int x = this.hitResult.x;
                int y = this.hitResult.y;
                int z = this.hitResult.z;

                // Get position of the tile using face direction
                if (this.hitResult.face == 0) y--;
                if (this.hitResult.face == 1) y++;
                if (this.hitResult.face == 2) z--;
                if (this.hitResult.face == 3) z++;
                if (this.hitResult.face == 4) x--;
                if (this.hitResult.face == 5) x++;

                // Set the tile
                this.level.setTile(x, y, z, this.selectedTileId);
            }
        }

        // Clear color and depth buffer and reset the camera
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Get current frustum
        Frustum frustum = Frustum.getFrustum();

        // Setup normal player camera
        setupCamera(partialTicks);
        glEnable(GL_CULL_FACE);

        // Update dirty chunks
        this.levelRenderer.updateDirtyChunks(this.player);

        // Setup daylight fog
        setupFog(0);
        glEnable(GL_FOG);

        // Render bright tiles
        this.levelRenderer.render(0);

        // Render zombies in sunlight
        for (Zombie zombie : this.zombies) {
            if (zombie.isLit() && frustum.isVisible(zombie.boundingBox)) {
                zombie.render(partialTicks);
            }
        }

        // Render particles in sunlight
        this.particleEngine.render(this.player, this.tessellator, partialTicks, 0);

        // Setup shadow fog
        setupFog(1);

        // Render dark tiles in shadow
        this.levelRenderer.render(1);

        // Render zombies in shadow
        for (Zombie zombie : this.zombies) {
            if (!zombie.isLit() && frustum.isVisible(zombie.boundingBox)) {
                zombie.render(partialTicks);
            }
        }

        // Render particles in shadow
        this.particleEngine.render(this.player, this.tessellator, partialTicks, 1);

        // Finish rendering
        glDisable(GL_LIGHTING);
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_FOG);

        // Render the actual hit
        if (this.hitResult != null) {
            glDisable(GL_ALPHA_TEST);
            this.levelRenderer.renderHit(this.hitResult);
            glEnable(GL_ALPHA_TEST);
        }

        // Draw player HUD
        drawGui(partialTicks);

        // Update the display
        Display.update();
    }

    /**
     * Draw HUD
     *
     * @param partialTicks Overflow ticks to interpolate
     */
    private void drawGui(float partialTicks) {
        // Clear depth
        glClear(GL_DEPTH_BUFFER_BIT);

        // Setup HUD camera
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();

        int screenWidth = this.width * 240 / this.height;
        int screenHeight = this.height * 240 / this.height;
        int xMouse = Mouse.getX() * screenWidth / this.width;
        int yMouse = screenHeight - Mouse.getY() * screenHeight / this.height - 1;

        // Set camera perspective
        glOrtho(0.0, screenWidth, screenHeight, 0.0, 100.0F, 300.0F);

        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        // Move camera to Z level -200
        glTranslatef(0.0f, 0.0f, -200.0f);

        // Start tile display
        glPushMatrix();

        // Transform tile position to the top right corner
        glTranslated(screenWidth - 16, 16.0F, 0.0F);
        glScalef(16.0F, 16.0F, 16.0F);
        glRotatef(30.0F, 1.0F, 0.0F, 0.0F);
        glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
        glTranslatef(-1.5F, 0.5F, -0.5F);
        glScalef(-1.0F, -1.0F, 1.0F);

        // Setup tile rendering
        int id = Textures.loadTexture("/terrain.png", 9728);
        glBindTexture(GL_TEXTURE_2D, id);
        glEnable(GL_TEXTURE_2D);

        // Enable blending for flashing effect
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA); // Standard alpha blending

        // Calculate flashing alpha
        float time = System.currentTimeMillis() / 100.0F;
        float alpha = ((float) Math.sin(time) * 0.2F + 0.5F); // Adjust flashing effect

        // Set color with flashing alpha
        glColor4f(1.0F, 1.0F, 1.0F, alpha);

        // Render selected tile in hand
        this.tessellator.init();
        Tile.tiles[this.selectedTileId].render(this.tessellator, this.level, 0, -2, 0, 0);
        this.tessellator.flush();

        // Finish tile rendering
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glPopMatrix();

        //Just some basic cool text
        this.font.drawShadow(this.VERSION_STRING, 2, 2, 16777215);
        this.font.drawShadow(this.fpsString, 2, 12, 16777215);
        this.font.drawShadow("UNFINISHED WIP!", 2, 22, 16777215);

        // Cross hair position
        int x = screenWidth / 2;
        int y = screenHeight / 2;

        // Cross hair color
        glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        // Render cross hair
        this.tessellator.init();
        this.tessellator.vertex((float) (x + 1), (float) (y - 4), 0.0F);
        this.tessellator.vertex((float) (x - 0), (float) (y - 4), 0.0F);
        this.tessellator.vertex((float) (x - 0), (float) (y + 5), 0.0F);
        this.tessellator.vertex((float) (x + 1), (float) (y + 5), 0.0F);
        this.tessellator.vertex((float) (x + 5), (float) (y - 0), 0.0F);
        this.tessellator.vertex((float) (x - 4), (float) (y - 0), 0.0F);
        this.tessellator.vertex((float) (x - 4), (float) (y + 1), 0.0F);
        this.tessellator.vertex((float) (x + 5), (float) (y + 1), 0.0F);
        this.tessellator.flush();
        if(this.screen != null) {
            this.screen.render(xMouse, yMouse);
        }
    }

    /**
     * Setup fog with type
     *
     * @param fogType Type of the fog. (0: daylight, 1: shadow)
     */
    private void setupFog(int fogType) {
        // Daylight fog
        if (fogType == 0) {
            // Fog distance
            glFogi(GL_FOG_MODE, GL_VIEWPORT_BIT);
            glFogf(GL_FOG_DENSITY, 0.003F);

            // Set fog color
            glFog(GL_FOG_COLOR, this.fogColorDaylight);

            glDisable(GL_LIGHTING);
        }

        // Shadow fog
        if (fogType == 1) {
            // Fog distance
            glFogi(GL_FOG_MODE, GL_VIEWPORT_BIT);
            glFogf(GL_FOG_DENSITY, 0.06F);

            // Set fog color
            glFog(GL_FOG_COLOR, this.fogColorShadow);

            glEnable(GL_LIGHTING);
            glEnable(GL_COLOR_MATERIAL);

            float brightness = 0.6F;
            glLightModel(GL_LIGHT_MODEL_AMBIENT, this.getBuffer(brightness, brightness, brightness, 1.0F));
        }
    }

    /**
     * Fill float buffer with color values and return it
     *
     * @param red   Red value
     * @param green Green value
     * @param blue  Blue value
     * @param alpha Alpha value
     * @return Float buffer filled in RGBA order
     */
    private FloatBuffer getBuffer(float red, float green, float blue, float alpha) {
        this.colorBuffer.clear();
        this.colorBuffer.put(red).put(green).put(blue).put(alpha);
        this.colorBuffer.flip();
        return this.colorBuffer;
    }

    /**
     * Entry point of the game
     *
     * @param args Program arguments (unused)
     */
    public static void main(String[] args) {
        boolean fullScreen = false;

        // Find fullscreen argument
        for (String arg : args) {
            if (arg.equalsIgnoreCase("-fullscreen")) {
                fullScreen = true;
                break;
            }
        }

        // Launch
        new Thread(new Minecraft(null, 854, 480, fullScreen)).start();
    }

    private static void initDiscordRPC() {
        client = new IPCClient(CLIENT_ID);

        client.setListener(new IPCListener() {
            @Override
            public void onReady(IPCClient client) {
                System.out.println("Connected to Discord!");
                updateDiscordPresence("Classicircle", "Playing a world");
            }
        });

        try {
            client.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            client.close();
        }));
    }

    private static void updateDiscordPresence(String state, String details) {
        RichPresence.Builder builder = new RichPresence.Builder();
        builder.setState(state)
               .setDetails(details)
               .setStartTimestamp(OffsetDateTime.now())  // Use OffsetDateTime.now()
               .setLargeImage("large_image_key", "Large Image Text")
               .setSmallImage("small_image_key", "Small Image Text");

        client.sendRichPresence(builder.build());
    }
}
