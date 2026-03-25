package com.pikuma;

import static org.lwjgl.sdl.SDLError.SDL_GetError;

public class Window {

    public boolean initiliazeWindow(){
        return true;
    }




    private  void checkSdlError(boolean success) {
        if (!success) {
            throw new IllegalStateException("SDL error encountered: " + SDL_GetError());
        }
    }

    private  long checkSdlError(long resultPointer) {
        if (resultPointer == 0) {
            throw new IllegalStateException("SDL error encountered: " + SDL_GetError());
        }
        return resultPointer;
    }
}
