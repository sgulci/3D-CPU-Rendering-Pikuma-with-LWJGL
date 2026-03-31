package com.pikuma;

import org.lwjgl.sdl.*;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.sdl.SDLBlendMode.SDL_BLENDMODE_BLEND;
import static org.lwjgl.sdl.SDLEvents.*;
import static org.lwjgl.sdl.SDLInit.SDL_INIT_EVENTS;
import static org.lwjgl.sdl.SDLInit.SDL_INIT_VIDEO;
import static org.lwjgl.sdl.SDLRender.SDL_LOGICAL_PRESENTATION_LETTERBOX;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_ESCAPE;
import static org.lwjgl.sdl.SDLVideo.SDL_WINDOW_RESIZABLE;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * SDL3 window using LWJGL bindings.
 *
 * Lifecycle:
 *   1. SDLInit.SDL_Init             – initialise SDL subsystems
 *   2. SDLVideo.SDL_CreateWindow    – create the OS window
 *   3. SDLRender.SDL_CreateRenderer – hardware-accelerated 2D renderer
 *   4. Event loop                   – poll events, clear + present each frame
 *   5. Cleanup                      – destroy renderer → window → quit SDL
 *
 * LWJGL SDL3 class mapping:
 *   SDLInit     → SDL_Init, SDL_Quit
 *   SDLVideo    → SDL_CreateWindow, SDL_DestroyWindow, SDL_GetWindowSize
 *   SDLRender   → SDL_CreateRenderer, SDL_DestroyRenderer, SDL_RenderClear,
 *                 SDL_RenderPresent, SDL_RenderFillRect, SDL_SetRenderDrawColorFloat
 *   SDLEvents   → SDL_PollEvent, SDL_GetEventType, SDL_EVENT_* constants
 *   SDLKeyboard → SDL_GetKeyboardScancode
 *   SDLTimer    → SDL_GetTicks, SDL_Delay
 *   SDLError    → SDL_GetError
 *   SDLBlendmode→ SDL_BLENDMODE_BLEND
 *   SDLScancode → SDL_SCANCODE_* constants
 */
public class SDLWindow {

    // ── Window configuration ─────────────────────────────────────────────────
    private static final String TITLE      = "LWJGL + SDL3 Window";
    private static final int    WIDTH      = 1280;
    private static final int    HEIGHT     = 720;
    private static final int    TARGET_FPS = 60;
    private static final long   FRAME_MS   = 1000L / TARGET_FPS;

    // ── SDL handles (0 == null / invalid) ────────────────────────────────────
    private long window;
    private long renderer;

    // ── State ────────────────────────────────────────────────────────────────
    private boolean running = false;
    private float   hue     = 0.0f;

    // ────────────────────────────────────────────────────────────────────────
    //  Public API
    // ────────────────────────────────────────────────────────────────────────

    public void run() {
        init();
        loop();
        cleanup();
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Initialisation
    // ────────────────────────────────────────────────────────────────────────

    private void init() {
        // SDLInit — subsystem flags live in SDLInit
        if (!SDLInit.SDL_Init(SDL_INIT_VIDEO | SDL_INIT_EVENTS)) {
            throw new RuntimeException("SDL_Init failed: " + SDLError.SDL_GetError());
        }

        // SDLVideo — window creation and management
        window = SDLVideo.SDL_CreateWindow(TITLE, WIDTH, HEIGHT, SDL_WINDOW_RESIZABLE);
        if (window == 0) {
            throw new RuntimeException("SDL_CreateWindow failed: " + SDLError.SDL_GetError());
        }

        // SDLRender — renderer creation and drawing
        // Passing an empty string selects the default renderer without triggering a null CharSequence NPE in LWJGL
        renderer = SDLRender.SDL_CreateRenderer(window, "");
        if (renderer == 0) {
            throw new RuntimeException("SDL_CreateRenderer failed: " + SDLError.SDL_GetError());
        }

        SDLRender.SDL_SetRenderLogicalPresentation(
                renderer, WIDTH, HEIGHT,
                SDL_LOGICAL_PRESENTATION_LETTERBOX
        );

        printWindowInfo();
        running = true;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Main loop
    // ────────────────────────────────────────────────────────────────────────

    private void loop() {
        long lastFrame = SDLTimer.SDL_GetTicks();

        while (running) {
            long now     = SDLTimer.SDL_GetTicks();
            long elapsed = now - lastFrame;
            lastFrame    = now;

            pollEvents();
            update((float) elapsed / 1000.0f);
            render();

            // Manual frame cap
            long frameTime = SDLTimer.SDL_GetTicks() - now;
            if (frameTime < FRAME_MS) {
                SDLTimer.SDL_Delay((int) (FRAME_MS - frameTime));
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Event handling
    // ────────────────────────────────────────────────────────────────────────

    private void pollEvents() {
        try (MemoryStack stack = stackPush()) {
            SDL_Event event = SDL_Event.malloc(stack);
            while (SDLEvents.SDL_PollEvent(event)) {
                handleEvent(event);
            }
        }
    }

    private void handleEvent(SDL_Event event) {
        int type = event.type();
        // Event type constants live in SDLEvents (SDL_EVENT_QUIT, SDL_EVENT_KEY_DOWN, …)
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
            case SDL_EVENT_WINDOW_RESIZED -> {
                try (MemoryStack stack = stackPush()) {
                    IntBuffer w = stack.mallocInt(1);
                    IntBuffer h = stack.mallocInt(1);
                    SDLVideo.SDL_GetWindowSize(window, w, h);
                    System.out.printf("[SDL3] Resized → %d × %d%n", w.get(0), h.get(0));
                }
            }
            case SDL_EVENT_MOUSE_BUTTON_DOWN ->
                    System.out.println("[SDL3] Mouse button pressed.");
            default -> { /* ignore */ }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Update
    // ────────────────────────────────────────────────────────────────────────

    private void update(float dt) {
        hue = (hue + 30.0f * dt) % 360.0f;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Render
    // ────────────────────────────────────────────────────────────────────────

    private void render() {
        float[] rgb = hsvToRgb(hue, 0.6f, 0.25f);
        SDLRender.SDL_SetRenderDrawColorFloat(renderer, rgb[0], rgb[1], rgb[2], 1.0f);
        SDLRender.SDL_RenderClear(renderer);

        drawCentreRect(200, 120, 1.0f, 1.0f, 1.0f, 0.85f);

        float[] accent = hsvToRgb((hue + 180.0f) % 360.0f, 0.9f, 0.9f);
        drawCentreRect(80, 8, accent[0], accent[1], accent[2], 1.0f);

        SDLRender.SDL_RenderPresent(renderer);
    }

    private void drawCentreRect(int w, int h, float r, float g, float b, float a) {
        try (MemoryStack stack = stackPush()) {
            // SDLBlendmode.SDL_BLENDMODE_BLEND — enables alpha blending
            SDLRender.SDL_SetRenderDrawBlendMode(renderer, SDL_BLENDMODE_BLEND);
            SDLRender.SDL_SetRenderDrawColorFloat(renderer, r, g, b, a);

            SDL_FRect rect = SDL_FRect.malloc(stack);
            rect.x((WIDTH  - w) / 2.0f);
            rect.y((HEIGHT - h) / 2.0f);
            rect.w((float) w);
            rect.h((float) h);

            SDLRender.SDL_RenderFillRect(renderer, rect);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Cleanup
    // ────────────────────────────────────────────────────────────────────────

    private void cleanup() {
        if (renderer != 0) SDLRender.SDL_DestroyRenderer(renderer);
        if (window   != 0) SDLVideo.SDL_DestroyWindow(window);
        SDLInit.SDL_Quit();
        System.out.println("[SDL3] Shutdown complete.");
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Utilities
    // ────────────────────────────────────────────────────────────────────────

    private static float[] hsvToRgb(float h, float s, float v) {
        float c      = v * s;
        float x      = c * (1.0f - Math.abs((h / 60.0f) % 2.0f - 1.0f));
        float m      = v - c;
        float r1, g1, b1;
        int   sector = (int) (h / 60.0f);
        switch (sector) {
            case 0  -> { r1 = c; g1 = x; b1 = 0; }
            case 1  -> { r1 = x; g1 = c; b1 = 0; }
            case 2  -> { r1 = 0; g1 = c; b1 = x; }
            case 3  -> { r1 = 0; g1 = x; b1 = c; }
            case 4  -> { r1 = x; g1 = 0; b1 = c; }
            default -> { r1 = c; g1 = 0; b1 = x; }
        }
        return new float[]{ r1 + m, g1 + m, b1 + m };
    }

    private void printWindowInfo() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            SDLVideo.SDL_GetWindowSize(window, w, h);
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
}