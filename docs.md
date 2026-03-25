## SDL_INIT_EVERYTHING removed in SDL3

SDL_INIT_EVERYTHING was a SDL2 convenience macro that no longer exists in SDL3.

SDL2 — existed
```
c:
// SDL2
SDL_Init(SDL_INIT_EVERYTHING);  // initialised all subsystems at once
```

SDL3 — removed, intentionally
The SDL3 migration guide explicitly states it was removed because it was considered bad practice 
you should only init what you actually need. Initialising unused subsystems wastes resources 
and can cause unnecessary failures on systems that don't support certain hardware (e.g. a server with no audio device failing on SDL_INIT_AUDIO).

SDL3 available flags
```
java// org.lwjgl.sdl.SDLInit
SDL_INIT_AUDIO      // audio device
SDL_INIT_VIDEO      // video + window system
SDL_INIT_JOYSTICK   // joystick input
SDL_INIT_HAPTIC     // force feedback
SDL_INIT_GAMEPAD    // gamepad / controller (replaces SDL2's SDL_INIT_GAMECONTROLLER)
SDL_INIT_EVENTS     // event system
SDL_INIT_SENSOR     // sensor devices (gyro, accelerometer)
SDL_INIT_CAMERA     // camera devices — new in SDL3
```

SDL_INIT_GAMECONTROLLER was also renamed to SDL_INIT_GAMEPAD in SDL3.


If you truly want everything, combine them manually
```
java: 

SDLInit.SDL_Init(
SDL_INIT_AUDIO    |
SDL_INIT_VIDEO    |
SDL_INIT_JOYSTICK |
SDL_INIT_HAPTIC   |
SDL_INIT_GAMEPAD  |
SDL_INIT_EVENTS   |
SDL_INIT_SENSOR   |
SDL_INIT_CAMERA
);

```

But realistically for a window + rendering project you only need:
```
java:

SDLInit.SDL_Init(SDL_INIT_VIDEO | SDL_INIT_EVENTS);
```

## RenderCopy to RenderTexture 

SDL2:
```
c// SDL2 - used integer rects
SDL_RenderCopy(renderer, texture, &srcRect, &dstRect);
```

SDL3:
```
java// SDL3 LWJGL - uses float rects now (SDL_FRect)
SDL_RenderTexture(renderer, texture, srcRect, dstRect);
```

The key change is SDL3 uses SDL_FRect (float rectangles) everywhere instead of

## Representing buffer memory data in Java

### The packed int color layout (ARGB8888)

A single int is 32 bits. The color 0xFFFF0000 (opaque red) looks like this in memory:
```
Bit position:  31      24 23      16 15       8 7        0
                ┌─────────┬──────────┬──────────┬─────────┐
color bits:     │   0xFF  │   0xFF   │   0x00   │  0x00   │
                │    A    │    R     │    G     │    B    │
                └─────────┴──────────┴──────────┴─────────┘
        
```

Extracting each channel

Step 1 — Shift right to bring the target byte down to the lowest 8 bits.

Step 2 — Mask with & 0xFF to discard all other bits.

Step 3 — Cast to byte to store just 8 bits.


Extract A — (color >> 24) & 0xFF

```
color    = 0xFF FF 00 00
>> 24      shift right 24 bits
= 0x00 00 00 FF   ← A is now in lowest byte
& 0xFF   = 0x00 00 00 FF
byte a   = 0xFF
```

Extract R — (color >> 16) & 0xFF
```
color    = 0xFF FF 00 00
>> 16      shift right 16 bits
= 0x00 00 FF FF   ← R is now in lowest byte (A is above)
& 0xFF   = 0x00 00 00 FF   ← mask wipes out A, keeps only R
byte r   = 0xFF

```

Extract G — (color >> 8) & 0xFF

```
color    = 0xFF FF 00 00
>> 8       shift right 8 bits
= 0x00 FF FF 00   ← G is now in lowest byte
& 0xFF   = 0x00 00 00 00   ← G was 0x00 in this example
byte g   = 0x00
```

Extract B — (color) & 0xFF
```
color    = 0xFF FF 00 00
no shift needed, B is already in lowest byte
& 0xFF   = 0x00 00 00 00   ← B was 0x00 in this example
byte b   = 0x00
```

Why & 0xFF is necessary
Without the mask, leftover bits from higher channels contaminate the result:
```
color    = 0xFF FF 00 00
>> 16    = 0x00 00 FF FF   ← without mask, this gives 0xFFFF not 0xFF
& 0xFF   = 0x00 00 00 FF   ← mask isolates only the bottom 8 bits ✓
```

## SDL DisplayMode difference

SDL2 used integer indices starting from 0:
```
java// SDL2 — index based
SDL_GetCurrentDisplayMode(0, &mode); // 0 = first display
```

SDL3 changed to display IDs which are opaque non-zero values:
```
java// SDL3 — ID based, ID is never 0
// 0 is always invalid, real IDs are system-assigned values like 1, 72, etc.
SDL_GetCurrentDisplayMode(displayID);
```

So passing 0 in SDL3 is always invalid — you must query the real ID via SDL_GetDisplays() first.


## SDL2 vs SDL3 SDL_SetWindowFullscreen

SDL2 had multiple fullscreen flags causing confusion:
```
java// SDL2 — three different flags
SDL_SetWindowFullscreen(window, SDL_WINDOW_FULLSCREEN);         // true fullscreen, changes resolution
SDL_SetWindowFullscreen(window, SDL_WINDOW_FULLSCREEN_DESKTOP); // borderless windowed fullscreen
SDL_SetWindowFullscreen(window, 0);                             // windowed mode
```

SDL3 simplified it to a simple boolean:
```
java// SDL3 — just a boolean
SDL_SetWindowFullscreen(window, true);  // fullscreen
SDL_SetWindowFullscreen(window, false); // windowed
```

SDL3 fullscreen is always borderless windowed style by default
(like SDL_WINDOW_FULLSCREEN_DESKTOP in SDL2). 
If you want to change resolution (true exclusive fullscreen), SDL3 uses a separate function:
```
java// SDL3 — exclusive fullscreen with specific display mode
SDL_DisplayMode mode = checkSdlError(SDL_GetCurrentDisplayMode(displayID));
SDL_SetWindowFullscreenMode(window, mode); // set the mode first
SDL_SetWindowFullscreen(window, true);     // then go fullscreen
```