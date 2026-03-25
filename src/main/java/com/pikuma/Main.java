package com.pikuma;

import org.lwjgl.sdl.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.sdl.SDLError.SDL_GetError;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_KEY_DOWN;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_QUIT;
import static org.lwjgl.sdl.SDLInit.*;
import static org.lwjgl.sdl.SDLPixels.SDL_PIXELFORMAT_ARGB8888;
import static org.lwjgl.sdl.SDLProperties.*;
import static org.lwjgl.sdl.SDLRender.SDL_TEXTUREACCESS_STREAMING;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_ESCAPE;
import static org.lwjgl.sdl.SDLVideo.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public class Main {

    // ── Window configuration ─────────────────────────────────────────────────
    private static final String TITLE      = "LWJGL + SDL3 Pikuma Window";
    private static int    WIDTH      = 1280;
    private static int    HEIGHT     = 720;
    private static final int    TARGET_FPS = 60;
    private static final long   FRAME_MS   = 1000L / TARGET_FPS;

    private static ByteBuffer colorPixelBuffer;

    // ── SDL handles (0 == null / invalid) ────────────────────────────────────
    private static long windowHandle;
    private static long rendererHandle;
    private static SDL_Texture colorBufferTextureHandle;

    // ── State ────────────────────────────────────────────────────────────────
    private static boolean running = false;


    public static void main(String[] args) {

       running = initWindow();

       setup();

       while (running) {
           processInput();
           update();
           render();
       }

        cleanup();
    }

    static boolean initWindow() {

        checkSdlError(SDL_Init(SDL_INIT_VIDEO | SDL_INIT_EVENTS));

        // 1. get all display IDs
        IntBuffer displays = checkSdlError(SDL_GetDisplays());

        // 2. get the first display ID
        int displayID = displays.get(0);

        SDL_DisplayMode displayMode =checkSdlError(SDLVideo.SDL_GetCurrentDisplayMode(displayID));
        HEIGHT = displayMode.h();
        WIDTH = displayMode.w();

         int props = SDL_CreateProperties();
        checkSdlError(SDL_SetNumberProperty(props, SDL_PROP_WINDOW_CREATE_X_NUMBER, SDL_WINDOWPOS_CENTERED));
        checkSdlError(SDL_SetNumberProperty(props, SDL_PROP_WINDOW_CREATE_Y_NUMBER, SDL_WINDOWPOS_CENTERED));
        checkSdlError(SDL_SetNumberProperty(props, SDL_PROP_WINDOW_CREATE_WIDTH_NUMBER, WIDTH));
        checkSdlError(SDL_SetNumberProperty(props, SDL_PROP_WINDOW_CREATE_HEIGHT_NUMBER, HEIGHT));
        checkSdlError(SDL_SetStringProperty(props, SDL_PROP_WINDOW_CREATE_TITLE_STRING, TITLE));
        checkSdlError(SDL_SetBooleanProperty(props, SDL_PROP_WINDOW_CREATE_BORDERLESS_BOOLEAN, true));

        // create window

        windowHandle = checkSdlError(SDLVideo.SDL_CreateWindowWithProperties(props));

        // create renderer
        // We do not need to complicate things as selecting rendering pipeline, default selections enough
        rendererHandle = checkSdlError(SDLRender.SDL_CreateRenderer(windowHandle,""));

        SDLVideo.SDL_SetWindowFullscreen(windowHandle,true);
        printWindowInfo();
        return true;
    }

    private static void render() {
        checkSdlError( SDLRender.SDL_SetRenderDrawColorFloat(rendererHandle,  1.0f, 0.0f, 0.0f, 1.0f));
        checkSdlError(SDLRender.SDL_RenderClear(rendererHandle));

        renderColorBuffer();
        clearColorBuffer(0xFFFFFFFF);

        checkSdlError(SDLRender.SDL_RenderPresent(rendererHandle));
    }

    private static void update() {
    }

    private static void processInput() {
      try(MemoryStack stack = stackPush()) {
          SDL_Event event  = SDL_Event.malloc(stack);
          while(SDLEvents.SDL_PollEvent(event)) {
              int type = event.type();

              switch (type) {
                  case SDL_EVENT_QUIT -> {
                      System.out.println("[SDL3] Quit event — shutting down.");
                      running = false;
                  }
                  case SDL_EVENT_KEY_DOWN -> {
                      int scancode = event.key().scancode();
                      System.out.printf("[SDL3] Key down — scancode %d%n", scancode);
                      // Scancode constants live in SDLScancode
                      if (scancode == SDL_SCANCODE_ESCAPE) {
                          running = false;
                      }

                  }
              }
          }
      }
        
    }

    private static void setup() {
        //  Allocate pixel buffer once — reuse every frame
        colorPixelBuffer = MemoryUtil.memAlloc(WIDTH * HEIGHT * 4);
        // texture for show color buffer
        colorBufferTextureHandle = checkSdlError(SDLRender.SDL_CreateTexture(rendererHandle,
                                                            SDL_PIXELFORMAT_ARGB8888,
                                                            SDL_TEXTUREACCESS_STREAMING,
                                                            WIDTH, HEIGHT));
    }

    private static void clearColorBuffer(int color) {
//        colorPixelBuffer.clear();

        // extract ARGB8888 channels from packed int color (0xAARRGGBB)
        byte a = (byte) ((color >> 24) & 0xFF);
        byte r = (byte) ((color >> 16) & 0xFF);
        byte g = (byte) ((color >> 8)  & 0xFF);
        byte b = (byte) ((color)       & 0xFF);


        for (int y = 0; y < HEIGHT ; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int index = (WIDTH * y + x) * 4; // 4 bytes per pixel

                // ARGB8888 little-endian memory layout: [B][G][R][A]
                colorPixelBuffer.put(index,     b);
                colorPixelBuffer.put(index + 1, g);
                colorPixelBuffer.put(index + 2, r);
                colorPixelBuffer.put(index + 3, a);
            }
        }

//        colorPixelBuffer.flip();
    }

    static void renderColorBuffer(){
       checkSdlError(SDLRender.SDL_UpdateTexture(colorBufferTextureHandle,null,colorPixelBuffer,WIDTH *4));
       checkSdlError(SDLRender.SDL_RenderTexture(rendererHandle,colorBufferTextureHandle,null,null));
    }

    private static void printWindowInfo() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            SDLVideo.SDL_GetWindowSize(windowHandle, w, h);
            System.out.printf("""
                    ╔══════════════════════════════════════╗
                    ║  LWJGL + SDL3 Window                 ║
                    ║  Size    : %4d × %-4d               ║
                    ║  FPS cap : %-3d                       ║
                    ║  ESC / ✕ to quit                     ║
                    ╚══════════════════════════════════════╝
                    %n""", w.get(0), h.get(0), TARGET_FPS);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Cleanup
    // ────────────────────────────────────────────────────────────────────────

    private static void cleanup() {
        if (rendererHandle != 0) SDLRender.SDL_DestroyRenderer(rendererHandle);
        if (windowHandle   != 0) SDLVideo.SDL_DestroyWindow(windowHandle);
        if (colorBufferTextureHandle != null)SDLRender.SDL_DestroyTexture(colorBufferTextureHandle);
        SDLInit.SDL_Quit();
        System.out.println("[SDL3] Shutdown complete.");
    }




    private static  void checkSdlError(boolean success) {
        if (!success) {
            throw new IllegalStateException("SDL error encountered: " + SDL_GetError());
        }
    }

    private static  long checkSdlError(long resultPointer) {
        if (resultPointer == 0) {
            throw new IllegalStateException("SDL error encountered: " + SDL_GetError());
        }
        return resultPointer;
    }

    private static <T> T checkSdlError(T result) {
        if (result == null) {
            throw new IllegalStateException("SDL error: " + SDL_GetError());
        }
        return result;
    }
}
