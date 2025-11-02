package net.dimaskama.mcef.impl;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.cef.CefBrowserSettings;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefRequestContext;
import org.cef.browser.CustomCefBrowserOsr;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL12;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.nio.ByteBuffer;

public class MCEFBrowserImpl extends CustomCefBrowserOsr implements MCEFBrowser {

    @Nullable
    private GpuTexture gpuTexture;
    @Nullable
    private GpuTextureView gpuTextureView;
    private int lastPressedMouseButton = MouseEvent.NOBUTTON;
    private boolean lastMouseEntered;
    private int cursorType = Cursor.DEFAULT_CURSOR;

    public MCEFBrowserImpl(CefClient client, String url, boolean transparent, CefRequestContext context, CefBrowserSettings settings) {
        super(client, url, transparent, context, settings);
    }

    @Override
    public void resize(int width, int height) {
        browserRect.setBounds(0, 0, width, height);
        wasResized(width, height);
    }

    @Override
    public void onMouseClicked(MouseButtonEvent event, boolean doubled) {
        int btn = toAwtMouseButton(event.button());
        lastPressedMouseButton = btn;
        sendMouseEvent(new MouseEvent(
                component,
                MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(),
                toAwtInputModifiers(event.modifiers()),
                (int) event.x(),
                (int) event.y(),
                doubled ? 2 : 1,
                false,
                btn
        ));
    }

    @Override
    public void onMouseReleased(MouseButtonEvent event) {
        int btn = toAwtMouseButton(event.button());
        if (btn == lastPressedMouseButton) {
            lastPressedMouseButton = MouseEvent.NOBUTTON;
        }
        sendMouseEvent(new MouseEvent(
                component,
                MouseEvent.MOUSE_RELEASED,
                System.currentTimeMillis(),
                toAwtInputModifiers(event.modifiers()),
                (int) event.x(),
                (int) event.y(),
                1,
                false,
                btn
        ));
    }

    @Override
    public void onMouseScrolled(int x, int y, double amount) {
        sendMouseWheelEvent(new MouseWheelEvent(
                component,
                MouseWheelEvent.WHEEL_UNIT_SCROLL,
                System.currentTimeMillis(),
                0,
                x,
                y,
                0,
                false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL,
                100,
                (int) Math.signum(amount)
        ));
    }

    @Override
    public void onMouseMoved(int x, int y) {
        boolean mouseEntered = browserRect.contains(x, y);
        if (mouseEntered != lastMouseEntered) {
            lastMouseEntered = mouseEntered;
            sendMouseEvent(new MouseEvent(
                    component,
                    mouseEntered ? MouseEvent.MOUSE_ENTERED : MouseEvent.MOUSE_EXITED,
                    System.currentTimeMillis(),
                    0,
                    x,
                    y,
                    0,
                    false,
                    MouseEvent.NOBUTTON
            ));
        }
        boolean dragging = lastPressedMouseButton != MouseEvent.NOBUTTON;
        sendMouseEvent(new MouseEvent(
                component,
                dragging ? MouseEvent.MOUSE_DRAGGED : MouseEvent.MOUSE_MOVED,
                System.currentTimeMillis(),
                0,
                x,
                y,
                0,
                false,
                dragging ? lastPressedMouseButton : MouseEvent.NOBUTTON
        ));
    }

    @Override
    public void onKeyPressed(net.minecraft.client.input.KeyEvent event) {
        int key = toAwtKeyCode(event.key());
        sendKeyEvent(new KeyEvent(
                component,
                KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                toAwtInputModifiers(event.modifiers()),
                key,
                (char) key
        ));
    }

    @Override
    public void onKeyReleased(net.minecraft.client.input.KeyEvent event) {
        int key = toAwtKeyCode(event.key());
        sendKeyEvent(new KeyEvent(
                component,
                KeyEvent.KEY_RELEASED,
                System.currentTimeMillis(),
                toAwtInputModifiers(event.modifiers()),
                key,
                (char) key
        ));
    }

    @Override
    public void onCharTyped(CharacterEvent event) {
        sendKeyEvent(new KeyEvent(
                component,
                KeyEvent.KEY_TYPED,
                System.currentTimeMillis(),
                toAwtInputModifiers(event.modifiers()),
                KeyEvent.VK_UNDEFINED,
                (char) event.codepoint()
        ));
    }

    @Override
    @Nullable
    public GpuTexture getTexture() {
        return gpuTexture;
    }

    @Override
    @Nullable
    public GpuTextureView getTextureView() {
        return gpuTextureView;
    }

    @Override
    public CursorType getCursorType() {
        return switch (cursorType) {
            case Cursor.CROSSHAIR_CURSOR -> CursorTypes.CROSSHAIR;
            case Cursor.TEXT_CURSOR -> CursorTypes.IBEAM;
            case Cursor.SW_RESIZE_CURSOR, Cursor.NE_RESIZE_CURSOR -> ExtraCursorTypes.RESIZE_NESW;
            case Cursor.SE_RESIZE_CURSOR, Cursor.NW_RESIZE_CURSOR -> ExtraCursorTypes.RESIZE_NWSE;
            case Cursor.N_RESIZE_CURSOR, Cursor.S_RESIZE_CURSOR -> CursorTypes.RESIZE_NS;
            case Cursor.W_RESIZE_CURSOR, Cursor.E_RESIZE_CURSOR -> CursorTypes.RESIZE_EW;
            case Cursor.HAND_CURSOR -> CursorTypes.POINTING_HAND;
            case Cursor.MOVE_CURSOR -> CursorTypes.ARROW;
            default -> CursorTypes.ARROW;
        };
    }

    @Override
    public void close() {
        if (gpuTextureView != null) {
            gpuTextureView.close();
            gpuTextureView = null;
        }
        if (gpuTexture != null) {
            gpuTexture.close();
            gpuTexture = null;
        }
        close(true);
    }

    @Override
    public CefBrowser getCefBrowser() {
        return this;
    }

    private static int toAwtInputModifiers(int mod) {
        int awtMod = 0;
        if ((mod & GLFW.GLFW_MOD_SHIFT) != 0)
            awtMod |= InputEvent.SHIFT_DOWN_MASK;
        if ((mod & GLFW.GLFW_MOD_CONTROL) != 0)
            awtMod |= InputEvent.CTRL_DOWN_MASK;
        if ((mod & GLFW.GLFW_MOD_ALT) != 0)
            awtMod |= InputEvent.ALT_DOWN_MASK;
        if ((mod & GLFW.GLFW_MOD_SUPER) != 0)
            awtMod |= InputEvent.META_DOWN_MASK;
        return awtMod;
    }

    private static int toAwtMouseButton(int button) {
        return switch (button) {
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> MouseEvent.BUTTON3;
            case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> MouseEvent.BUTTON2;
            default -> MouseEvent.BUTTON1;
        };
    }

    private static int toAwtKeyCode(int glfwKey) {
        return switch (glfwKey) {
            case GLFW.GLFW_KEY_SPACE -> KeyEvent.VK_SPACE;
            case GLFW.GLFW_KEY_APOSTROPHE -> KeyEvent.VK_QUOTE;
            case GLFW.GLFW_KEY_COMMA -> KeyEvent.VK_COMMA;
            case GLFW.GLFW_KEY_MINUS -> KeyEvent.VK_MINUS;
            case GLFW.GLFW_KEY_PERIOD -> KeyEvent.VK_PERIOD;
            case GLFW.GLFW_KEY_SLASH -> KeyEvent.VK_SLASH;

            case GLFW.GLFW_KEY_0 -> KeyEvent.VK_0;
            case GLFW.GLFW_KEY_1 -> KeyEvent.VK_1;
            case GLFW.GLFW_KEY_2 -> KeyEvent.VK_2;
            case GLFW.GLFW_KEY_3 -> KeyEvent.VK_3;
            case GLFW.GLFW_KEY_4 -> KeyEvent.VK_4;
            case GLFW.GLFW_KEY_5 -> KeyEvent.VK_5;
            case GLFW.GLFW_KEY_6 -> KeyEvent.VK_6;
            case GLFW.GLFW_KEY_7 -> KeyEvent.VK_7;
            case GLFW.GLFW_KEY_8 -> KeyEvent.VK_8;
            case GLFW.GLFW_KEY_9 -> KeyEvent.VK_9;

            case GLFW.GLFW_KEY_A -> KeyEvent.VK_A;
            case GLFW.GLFW_KEY_B -> KeyEvent.VK_B;
            case GLFW.GLFW_KEY_C -> KeyEvent.VK_C;
            case GLFW.GLFW_KEY_D -> KeyEvent.VK_D;
            case GLFW.GLFW_KEY_E -> KeyEvent.VK_E;
            case GLFW.GLFW_KEY_F -> KeyEvent.VK_F;
            case GLFW.GLFW_KEY_G -> KeyEvent.VK_G;
            case GLFW.GLFW_KEY_H -> KeyEvent.VK_H;
            case GLFW.GLFW_KEY_I -> KeyEvent.VK_I;
            case GLFW.GLFW_KEY_J -> KeyEvent.VK_J;
            case GLFW.GLFW_KEY_K -> KeyEvent.VK_K;
            case GLFW.GLFW_KEY_L -> KeyEvent.VK_L;
            case GLFW.GLFW_KEY_M -> KeyEvent.VK_M;
            case GLFW.GLFW_KEY_N -> KeyEvent.VK_N;
            case GLFW.GLFW_KEY_O -> KeyEvent.VK_O;
            case GLFW.GLFW_KEY_P -> KeyEvent.VK_P;
            case GLFW.GLFW_KEY_Q -> KeyEvent.VK_Q;
            case GLFW.GLFW_KEY_R -> KeyEvent.VK_R;
            case GLFW.GLFW_KEY_S -> KeyEvent.VK_S;
            case GLFW.GLFW_KEY_T -> KeyEvent.VK_T;
            case GLFW.GLFW_KEY_U -> KeyEvent.VK_U;
            case GLFW.GLFW_KEY_V -> KeyEvent.VK_V;
            case GLFW.GLFW_KEY_W -> KeyEvent.VK_W;
            case GLFW.GLFW_KEY_X -> KeyEvent.VK_X;
            case GLFW.GLFW_KEY_Y -> KeyEvent.VK_Y;
            case GLFW.GLFW_KEY_Z -> KeyEvent.VK_Z;

            case GLFW.GLFW_KEY_ESCAPE -> KeyEvent.VK_ESCAPE;
            case GLFW.GLFW_KEY_ENTER -> KeyEvent.VK_ENTER;
            case GLFW.GLFW_KEY_TAB -> KeyEvent.VK_TAB;
            case GLFW.GLFW_KEY_BACKSPACE -> KeyEvent.VK_BACK_SPACE;
            case GLFW.GLFW_KEY_INSERT -> KeyEvent.VK_INSERT;
            case GLFW.GLFW_KEY_DELETE -> KeyEvent.VK_DELETE;
            case GLFW.GLFW_KEY_RIGHT -> KeyEvent.VK_RIGHT;
            case GLFW.GLFW_KEY_LEFT -> KeyEvent.VK_LEFT;
            case GLFW.GLFW_KEY_DOWN -> KeyEvent.VK_DOWN;
            case GLFW.GLFW_KEY_UP -> KeyEvent.VK_UP;
            case GLFW.GLFW_KEY_PAGE_UP -> KeyEvent.VK_PAGE_UP;
            case GLFW.GLFW_KEY_PAGE_DOWN -> KeyEvent.VK_PAGE_DOWN;
            case GLFW.GLFW_KEY_HOME -> KeyEvent.VK_HOME;
            case GLFW.GLFW_KEY_END -> KeyEvent.VK_END;
            case GLFW.GLFW_KEY_CAPS_LOCK -> KeyEvent.VK_CAPS_LOCK;
            case GLFW.GLFW_KEY_SCROLL_LOCK -> KeyEvent.VK_SCROLL_LOCK;
            case GLFW.GLFW_KEY_NUM_LOCK -> KeyEvent.VK_NUM_LOCK;
            case GLFW.GLFW_KEY_PRINT_SCREEN -> KeyEvent.VK_PRINTSCREEN;
            case GLFW.GLFW_KEY_PAUSE -> KeyEvent.VK_PAUSE;

            case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> KeyEvent.VK_SHIFT;
            case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> KeyEvent.VK_CONTROL;
            case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> KeyEvent.VK_ALT;
            case GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER -> KeyEvent.VK_META;

            case GLFW.GLFW_KEY_F1 -> KeyEvent.VK_F1;
            case GLFW.GLFW_KEY_F2 -> KeyEvent.VK_F2;
            case GLFW.GLFW_KEY_F3 -> KeyEvent.VK_F3;
            case GLFW.GLFW_KEY_F4 -> KeyEvent.VK_F4;
            case GLFW.GLFW_KEY_F5 -> KeyEvent.VK_F5;
            case GLFW.GLFW_KEY_F6 -> KeyEvent.VK_F6;
            case GLFW.GLFW_KEY_F7 -> KeyEvent.VK_F7;
            case GLFW.GLFW_KEY_F8 -> KeyEvent.VK_F8;
            case GLFW.GLFW_KEY_F9 -> KeyEvent.VK_F9;
            case GLFW.GLFW_KEY_F10 -> KeyEvent.VK_F10;
            case GLFW.GLFW_KEY_F11 -> KeyEvent.VK_F11;
            case GLFW.GLFW_KEY_F12 -> KeyEvent.VK_F12;

            case GLFW.GLFW_KEY_KP_0 -> KeyEvent.VK_NUMPAD0;
            case GLFW.GLFW_KEY_KP_1 -> KeyEvent.VK_NUMPAD1;
            case GLFW.GLFW_KEY_KP_2 -> KeyEvent.VK_NUMPAD2;
            case GLFW.GLFW_KEY_KP_3 -> KeyEvent.VK_NUMPAD3;
            case GLFW.GLFW_KEY_KP_4 -> KeyEvent.VK_NUMPAD4;
            case GLFW.GLFW_KEY_KP_5 -> KeyEvent.VK_NUMPAD5;
            case GLFW.GLFW_KEY_KP_6 -> KeyEvent.VK_NUMPAD6;
            case GLFW.GLFW_KEY_KP_7 -> KeyEvent.VK_NUMPAD7;
            case GLFW.GLFW_KEY_KP_8 -> KeyEvent.VK_NUMPAD8;
            case GLFW.GLFW_KEY_KP_9 -> KeyEvent.VK_NUMPAD9;
            case GLFW.GLFW_KEY_KP_DECIMAL -> KeyEvent.VK_DECIMAL;
            case GLFW.GLFW_KEY_KP_DIVIDE -> KeyEvent.VK_DIVIDE;
            case GLFW.GLFW_KEY_KP_MULTIPLY -> KeyEvent.VK_MULTIPLY;
            case GLFW.GLFW_KEY_KP_SUBTRACT -> KeyEvent.VK_SUBTRACT;
            case GLFW.GLFW_KEY_KP_ADD -> KeyEvent.VK_ADD;
            case GLFW.GLFW_KEY_KP_ENTER -> KeyEvent.VK_ENTER;
            case GLFW.GLFW_KEY_KP_EQUAL -> KeyEvent.VK_EQUALS;

            case GLFW.GLFW_KEY_SEMICOLON -> KeyEvent.VK_SEMICOLON;
            case GLFW.GLFW_KEY_EQUAL -> KeyEvent.VK_EQUALS;
            case GLFW.GLFW_KEY_LEFT_BRACKET -> KeyEvent.VK_OPEN_BRACKET;
            case GLFW.GLFW_KEY_RIGHT_BRACKET -> KeyEvent.VK_CLOSE_BRACKET;
            case GLFW.GLFW_KEY_BACKSLASH -> KeyEvent.VK_BACK_SLASH;
            case GLFW.GLFW_KEY_GRAVE_ACCENT -> KeyEvent.VK_BACK_QUOTE;
            default -> KeyEvent.VK_UNDEFINED;
        };
    }

    //TODO Popups

    @Override
    public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
        ByteBuffer copy = MemoryUtil.memAlloc(buffer.capacity());
        MemoryUtil.memCopy(buffer, copy);
        Minecraft.getInstance().submit(() -> onPaintInternal(popup, dirtyRects, copy, width, height));
        super.onPaint(browser, popup, dirtyRects, buffer, width, height);
    }

    private void onPaintInternal(boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
        if (dirtyRects.length == 0) {
            return;
        }

        if (!popup) {
            if (gpuTexture == null || gpuTexture.getWidth(0) != width || gpuTexture.getHeight(0) != height) {
                if (gpuTextureView != null) {
                    gpuTextureView.close();
                }
                if (gpuTexture != null) {
                    gpuTexture.close();
                }
                gpuTexture = RenderSystem.getDevice().createTexture(
                        "MCEFBrowser",
                        GpuTexture.USAGE_COPY_DST
                                | GpuTexture.USAGE_COPY_SRC
                                | GpuTexture.USAGE_TEXTURE_BINDING
                                | GpuTexture.USAGE_RENDER_ATTACHMENT,
                        TextureFormat.RGBA8,
                        width,
                        height,
                        1,
                        1
                );
                gpuTextureView = RenderSystem.getDevice().createTextureView(gpuTexture);
            }
            GlStateManager._bindTexture(((GlTexture) gpuTexture).glId());
            GlStateManager._pixelStore(GlConst.GL_UNPACK_ROW_LENGTH, width);
            GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_PIXELS, 0);
            GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_ROWS, 0);
            GlStateManager._pixelStore(GlConst.GL_UNPACK_ALIGNMENT, 4);
            GlStateManager._texSubImage2D(
                    GlConst.GL_TEXTURE_2D,
                    0,
                    0,
                    0,
                    width,
                    height,
                    GL12.GL_BGRA,
                    GlConst.GL_UNSIGNED_BYTE,
                    buffer
            );
        }

        MemoryUtil.memFree(buffer);
    }

    @Override
    public boolean onCursorChange(CefBrowser browser, int cursorType) {
        this.cursorType = cursorType;
        return super.onCursorChange(browser, cursorType);
    }

}
