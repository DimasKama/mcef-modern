package net.dimaskama.mcef.impl;

import com.mojang.blaze3d.platform.cursor.CursorType;
import org.lwjgl.glfw.GLFW;

public class ExtraCursorTypes {

    public static final CursorType RESIZE_NWSE = CursorType.createStandardCursor(GLFW.GLFW_RESIZE_NWSE_CURSOR, "resize_nwse", CursorType.DEFAULT);
    public static final CursorType RESIZE_NESW = CursorType.createStandardCursor(GLFW.GLFW_RESIZE_NESW_CURSOR, "resize_nesw", CursorType.DEFAULT);

}
