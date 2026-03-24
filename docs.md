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