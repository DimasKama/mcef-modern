package net.dimaskama.mcef.impl;

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
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
import org.lwjgl.sdl.SDLKeycode;
import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.sdl.SDLScancode;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

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
                0,
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
        if ((mod & SDLKeycode.SDL_KMOD_SHIFT) != 0)
            awtMod |= InputEvent.SHIFT_DOWN_MASK;
        if ((mod & SDLKeycode.SDL_KMOD_CTRL) != 0)
            awtMod |= InputEvent.CTRL_DOWN_MASK;
        if ((mod & SDLKeycode.SDL_KMOD_ALT) != 0)
            awtMod |= InputEvent.ALT_DOWN_MASK;
        if ((mod & SDLKeycode.SDL_KMOD_GUI) != 0)
            awtMod |= InputEvent.META_DOWN_MASK;
        return awtMod;
    }

    private static int toAwtMouseButton(int button) {
        return switch (button) {
            case SDLMouse.SDL_BUTTON_RIGHT -> MouseEvent.BUTTON3;
            case SDLMouse.SDL_BUTTON_MIDDLE -> MouseEvent.BUTTON2;
            default -> MouseEvent.BUTTON1;
        };
    }

    private static int toAwtKeyCode(int sdlScancode) {
        return switch (sdlScancode) {
            case SDLScancode.SDL_SCANCODE_SPACE -> KeyEvent.VK_SPACE;
            case SDLScancode.SDL_SCANCODE_APOSTROPHE -> KeyEvent.VK_QUOTE;
            case SDLScancode.SDL_SCANCODE_COMMA -> KeyEvent.VK_COMMA;
            case SDLScancode.SDL_SCANCODE_MINUS -> KeyEvent.VK_MINUS;
            case SDLScancode.SDL_SCANCODE_PERIOD -> KeyEvent.VK_PERIOD;
            case SDLScancode.SDL_SCANCODE_SLASH -> KeyEvent.VK_SLASH;

            case SDLScancode.SDL_SCANCODE_0 -> KeyEvent.VK_0;
            case SDLScancode.SDL_SCANCODE_1 -> KeyEvent.VK_1;
            case SDLScancode.SDL_SCANCODE_2 -> KeyEvent.VK_2;
            case SDLScancode.SDL_SCANCODE_3 -> KeyEvent.VK_3;
            case SDLScancode.SDL_SCANCODE_4 -> KeyEvent.VK_4;
            case SDLScancode.SDL_SCANCODE_5 -> KeyEvent.VK_5;
            case SDLScancode.SDL_SCANCODE_6 -> KeyEvent.VK_6;
            case SDLScancode.SDL_SCANCODE_7 -> KeyEvent.VK_7;
            case SDLScancode.SDL_SCANCODE_8 -> KeyEvent.VK_8;
            case SDLScancode.SDL_SCANCODE_9 -> KeyEvent.VK_9;

            case SDLScancode.SDL_SCANCODE_A -> KeyEvent.VK_A;
            case SDLScancode.SDL_SCANCODE_B -> KeyEvent.VK_B;
            case SDLScancode.SDL_SCANCODE_C -> KeyEvent.VK_C;
            case SDLScancode.SDL_SCANCODE_D -> KeyEvent.VK_D;
            case SDLScancode.SDL_SCANCODE_E -> KeyEvent.VK_E;
            case SDLScancode.SDL_SCANCODE_F -> KeyEvent.VK_F;
            case SDLScancode.SDL_SCANCODE_G -> KeyEvent.VK_G;
            case SDLScancode.SDL_SCANCODE_H -> KeyEvent.VK_H;
            case SDLScancode.SDL_SCANCODE_I -> KeyEvent.VK_I;
            case SDLScancode.SDL_SCANCODE_J -> KeyEvent.VK_J;
            case SDLScancode.SDL_SCANCODE_K -> KeyEvent.VK_K;
            case SDLScancode.SDL_SCANCODE_L -> KeyEvent.VK_L;
            case SDLScancode.SDL_SCANCODE_M -> KeyEvent.VK_M;
            case SDLScancode.SDL_SCANCODE_N -> KeyEvent.VK_N;
            case SDLScancode.SDL_SCANCODE_O -> KeyEvent.VK_O;
            case SDLScancode.SDL_SCANCODE_P -> KeyEvent.VK_P;
            case SDLScancode.SDL_SCANCODE_Q -> KeyEvent.VK_Q;
            case SDLScancode.SDL_SCANCODE_R -> KeyEvent.VK_R;
            case SDLScancode.SDL_SCANCODE_S -> KeyEvent.VK_S;
            case SDLScancode.SDL_SCANCODE_T -> KeyEvent.VK_T;
            case SDLScancode.SDL_SCANCODE_U -> KeyEvent.VK_U;
            case SDLScancode.SDL_SCANCODE_V -> KeyEvent.VK_V;
            case SDLScancode.SDL_SCANCODE_W -> KeyEvent.VK_W;
            case SDLScancode.SDL_SCANCODE_X -> KeyEvent.VK_X;
            case SDLScancode.SDL_SCANCODE_Y -> KeyEvent.VK_Y;
            case SDLScancode.SDL_SCANCODE_Z -> KeyEvent.VK_Z;

            case SDLScancode.SDL_SCANCODE_ESCAPE -> KeyEvent.VK_ESCAPE;
            case SDLScancode.SDL_SCANCODE_RETURN -> KeyEvent.VK_ENTER;
            case SDLScancode.SDL_SCANCODE_TAB -> KeyEvent.VK_TAB;
            case SDLScancode.SDL_SCANCODE_BACKSPACE -> KeyEvent.VK_BACK_SPACE;
            case SDLScancode.SDL_SCANCODE_INSERT -> KeyEvent.VK_INSERT;
            case SDLScancode.SDL_SCANCODE_DELETE -> KeyEvent.VK_DELETE;
            case SDLScancode.SDL_SCANCODE_RIGHT -> KeyEvent.VK_RIGHT;
            case SDLScancode.SDL_SCANCODE_LEFT -> KeyEvent.VK_LEFT;
            case SDLScancode.SDL_SCANCODE_DOWN -> KeyEvent.VK_DOWN;
            case SDLScancode.SDL_SCANCODE_UP -> KeyEvent.VK_UP;
            case SDLScancode.SDL_SCANCODE_PAGEUP -> KeyEvent.VK_PAGE_UP;
            case SDLScancode.SDL_SCANCODE_PAGEDOWN -> KeyEvent.VK_PAGE_DOWN;
            case SDLScancode.SDL_SCANCODE_HOME -> KeyEvent.VK_HOME;
            case SDLScancode.SDL_SCANCODE_END -> KeyEvent.VK_END;
            case SDLScancode.SDL_SCANCODE_CAPSLOCK -> KeyEvent.VK_CAPS_LOCK;
            case SDLScancode.SDL_SCANCODE_SCROLLLOCK -> KeyEvent.VK_SCROLL_LOCK;
            case SDLScancode.SDL_SCANCODE_NUMLOCKCLEAR -> KeyEvent.VK_NUM_LOCK;
            case SDLScancode.SDL_SCANCODE_PRINTSCREEN -> KeyEvent.VK_PRINTSCREEN;
            case SDLScancode.SDL_SCANCODE_PAUSE -> KeyEvent.VK_PAUSE;

            case SDLScancode.SDL_SCANCODE_LSHIFT, SDLScancode.SDL_SCANCODE_RSHIFT -> KeyEvent.VK_SHIFT;
            case SDLScancode.SDL_SCANCODE_LCTRL, SDLScancode.SDL_SCANCODE_RCTRL -> KeyEvent.VK_CONTROL;
            case SDLScancode.SDL_SCANCODE_LALT, SDLScancode.SDL_SCANCODE_RALT -> KeyEvent.VK_ALT;
            case SDLScancode.SDL_SCANCODE_LGUI, SDLScancode.SDL_SCANCODE_RGUI -> KeyEvent.VK_META;

            case SDLScancode.SDL_SCANCODE_F1 -> KeyEvent.VK_F1;
            case SDLScancode.SDL_SCANCODE_F2 -> KeyEvent.VK_F2;
            case SDLScancode.SDL_SCANCODE_F3 -> KeyEvent.VK_F3;
            case SDLScancode.SDL_SCANCODE_F4 -> KeyEvent.VK_F4;
            case SDLScancode.SDL_SCANCODE_F5 -> KeyEvent.VK_F5;
            case SDLScancode.SDL_SCANCODE_F6 -> KeyEvent.VK_F6;
            case SDLScancode.SDL_SCANCODE_F7 -> KeyEvent.VK_F7;
            case SDLScancode.SDL_SCANCODE_F8 -> KeyEvent.VK_F8;
            case SDLScancode.SDL_SCANCODE_F9 -> KeyEvent.VK_F9;
            case SDLScancode.SDL_SCANCODE_F10 -> KeyEvent.VK_F10;
            case SDLScancode.SDL_SCANCODE_F11 -> KeyEvent.VK_F11;
            case SDLScancode.SDL_SCANCODE_F12 -> KeyEvent.VK_F12;

            case SDLScancode.SDL_SCANCODE_KP_0 -> KeyEvent.VK_NUMPAD0;
            case SDLScancode.SDL_SCANCODE_KP_1 -> KeyEvent.VK_NUMPAD1;
            case SDLScancode.SDL_SCANCODE_KP_2 -> KeyEvent.VK_NUMPAD2;
            case SDLScancode.SDL_SCANCODE_KP_3 -> KeyEvent.VK_NUMPAD3;
            case SDLScancode.SDL_SCANCODE_KP_4 -> KeyEvent.VK_NUMPAD4;
            case SDLScancode.SDL_SCANCODE_KP_5 -> KeyEvent.VK_NUMPAD5;
            case SDLScancode.SDL_SCANCODE_KP_6 -> KeyEvent.VK_NUMPAD6;
            case SDLScancode.SDL_SCANCODE_KP_7 -> KeyEvent.VK_NUMPAD7;
            case SDLScancode.SDL_SCANCODE_KP_8 -> KeyEvent.VK_NUMPAD8;
            case SDLScancode.SDL_SCANCODE_KP_9 -> KeyEvent.VK_NUMPAD9;
            case SDLScancode.SDL_SCANCODE_KP_PERIOD -> KeyEvent.VK_DECIMAL;
            case SDLScancode.SDL_SCANCODE_KP_DIVIDE -> KeyEvent.VK_DIVIDE;
            case SDLScancode.SDL_SCANCODE_KP_MULTIPLY -> KeyEvent.VK_MULTIPLY;
            case SDLScancode.SDL_SCANCODE_KP_MINUS -> KeyEvent.VK_SUBTRACT;
            case SDLScancode.SDL_SCANCODE_KP_PLUS -> KeyEvent.VK_ADD;
            case SDLScancode.SDL_SCANCODE_KP_ENTER -> KeyEvent.VK_ENTER;
            case SDLScancode.SDL_SCANCODE_KP_EQUALS -> KeyEvent.VK_EQUALS;

            case SDLScancode.SDL_SCANCODE_SEMICOLON -> KeyEvent.VK_SEMICOLON;
            case SDLScancode.SDL_SCANCODE_EQUALS -> KeyEvent.VK_EQUALS;
            case SDLScancode.SDL_SCANCODE_LEFTBRACKET -> KeyEvent.VK_OPEN_BRACKET;
            case SDLScancode.SDL_SCANCODE_RIGHTBRACKET -> KeyEvent.VK_CLOSE_BRACKET;
            case SDLScancode.SDL_SCANCODE_BACKSLASH -> KeyEvent.VK_BACK_SLASH;
            case SDLScancode.SDL_SCANCODE_GRAVE -> KeyEvent.VK_BACK_QUOTE;
            default -> KeyEvent.VK_UNDEFINED;
        };
    }

    //TODO Popups

    @Override
    public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
        if (dirtyRects.length != 0 && !popup) {
            ByteBuffer copy = copyBgraToRgba(buffer, width * height);
            Minecraft.getInstance().submit(() -> onPaintInternal(copy, width, height));
        }
        super.onPaint(browser, popup, dirtyRects, buffer, width, height);
    }

    // CEF paints in BGRA, but GpuFormat has no BGRA format
    private static ByteBuffer copyBgraToRgba(ByteBuffer bgra, int pixelCount) {
        ByteBuffer rgba = MemoryUtil.memAlloc(pixelCount * 4);
        IntBuffer src = bgra.duplicate().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
        IntBuffer dst = rgba.duplicate().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
        for (int i = 0; i < pixelCount; i++) {
            int argb = src.get(i);
            dst.put(i, (argb & 0xFF00FF00) | ((argb >> 16) & 0xFF) | ((argb & 0xFF) << 16));
        }
        return rgba;
    }

    private void onPaintInternal(ByteBuffer buffer, int width, int height) {
        try {
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
                        GpuFormat.RGBA8_UNORM,
                        width,
                        height,
                        1,
                        1
                );
                gpuTextureView = RenderSystem.getDevice().createTextureView(gpuTexture);
            }
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(gpuTexture, buffer, 0, 0, 0, 0, width, height);
        } finally {
            MemoryUtil.memFree(buffer);
        }
    }

    @Override
    public boolean onCursorChange(CefBrowser browser, int cursorType) {
        this.cursorType = cursorType;
        return super.onCursorChange(browser, cursorType);
    }

}
