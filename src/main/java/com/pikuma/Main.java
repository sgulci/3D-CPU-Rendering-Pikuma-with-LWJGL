package com.pikuma;

import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLRender;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.sdl.SDLError.SDL_GetError;
import static org.lwjgl.sdl.SDLInit.*;
import static org.lwjgl.sdl.SDLProperties.*;
import static org.lwjgl.sdl.SDLVideo.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public class Main {

    // ── Window configuration ─────────────────────────────────────────────────
    private static final String TITLE      = "LWJGL + SDL3 Pikuma Window";
    private static final int    WIDTH      = 1280;
    private static final int    HEIGHT     = 720;
    private static final int    TARGET_FPS = 60;
    private static final long   FRAME_MS   = 1000L / TARGET_FPS;

    // ── SDL handles (0 == null / invalid) ────────────────────────────────────
    private static long windowHandle;
    private static long rendererHandle;

    // ── State ────────────────────────────────────────────────────────────────
    private static boolean running = false;


    public static void main(String[] args) {

       running = initWindow();
    }


    static boolean initWindow() {

        checkSdlError(SDL_Init(SDL_INIT_VIDEO | SDL_INIT_EVENTS));

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

        printWindowInfo();
        return true;
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
}
