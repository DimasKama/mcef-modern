package net.dimaskama.mcef.impl;

import com.mojang.blaze3d.platform.cursor.CursorType;
import org.lwjgl.sdl.SDLMouse;

public class ExtraCursorTypes {

    public static final CursorType RESIZE_NWSE = CursorType.createStandardCursor(SDLMouse.SDL_SYSTEM_CURSOR_NWSE_RESIZE, "resize_nwse", CursorType.DEFAULT);
    public static final CursorType RESIZE_NESW = CursorType.createStandardCursor(SDLMouse.SDL_SYSTEM_CURSOR_NESW_RESIZE, "resize_nesw", CursorType.DEFAULT);

}
