package net.dimaskama.mcef.api;

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.cef.browser.CefBrowser;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an embedded Chromium browser instance managed by the MCEF Modern API.
 * <p>
 * Browsers must be properly closed using {@link #close()} when no longer needed
 * to release native resources and prevent memory leaks.
 */
public interface MCEFBrowser extends AutoCloseable {

    /**
     * Resizes the browser viewport to the specified dimensions.
     * <p>
     * This method must be called right after creation of this instance
     *
     * @param width  the new width of the browser viewport, in pixels
     * @param height the new height of the browser viewport, in pixels
     */
    void resize(int width, int height);

    /**
     * Sets the focus state of the browser.
     *
     * @param isFocused {@code true} to focus the browser, {@code false} to unfocus it
     */
    void setFocus(boolean isFocused);

    /**
     * Handles a mouse click event.
     *
     * @param x         raw mouse position x
     * @param y         raw mouse position y
     * @param button    mouse button
     * @param modifiers input modifiers
     * @param doubled   {@code true} if this is a double-click event
     */
    void onMouseClicked(int x, int y, int button, int modifiers, boolean doubled);

    /**
     * Handles a mouse button release event.
     *
     * @param x         raw mouse position x
     * @param y         raw mouse position y
     * @param button    mouse button
     * @param modifiers input modifiers
     */
    void onMouseReleased(int x, int y, int button, int modifiers);

    /**
     * Handles mouse scroll (wheel) events.
     *
     * @param x      the horizontal cursor position
     * @param y      the vertical cursor position
     * @param amount the scroll delta amount
     */
    void onMouseScrolled(int x, int y, double amount);

    /**
     * Handles mouse movement events.
     *
     * @param x the new horizontal position of the cursor, in pixels
     * @param y the new vertical position of the cursor, in pixels
     */
    void onMouseMoved(int x, int y);

    /**
     * Handles a key press event.
     *
     * @param event the key event representing the pressed key
     */
    void onKeyPressed(KeyEvent event);

    /**
     * Handles a key release event.
     *
     * @param event the key event representing the released key
     */
    void onKeyReleased(KeyEvent event);

    /**
     * Handles a character typing event.
     *
     * @param event the character input event
     */
    void onCharTyped(CharacterEvent event);

    /**
     * Returns the current GPU texture containing the rendered browser output.
     * <p>
     * May return {@code null} if the texture is not yet available or
     * the browser is not currently rendering.
     *
     * @return the {@link GpuTexture} containing the browser's frame, or {@code null} if unavailable
     */
    @Nullable
    GpuTexture getTexture();

    /**
     * Returns the {@link GpuTextureView} associated with the current browser frame.
     * <p>
     * This view may be used for rendering the browser output in GPU pipelines.
     *
     * @return the {@link GpuTextureView} instance, or {@code null} if unavailable
     */
    @Nullable
    GpuTextureView getTextureView();

    /**
     * Returns the current cursor type used by the browser.
     * <p>
     * This reflects the cursor icon the browser requests (e.g., pointer, text, resize).
     *
     * @return the {@link CursorType} currently active in the browser
     */
    CursorType getCursorType();

    /**
     * Closes this browser instance and releases all associated resources.
     * <p>
     * Once closed, the browser cannot be used again.
     * <p>
     * This method should always be called when the browser is no longer displayed
     * to ensure proper cleanup.
     */
    @Override
    void close();

    /**
     * Returns the underlying {@link CefBrowser} instance
     *
     * @return the underlying {@link CefBrowser} instance
     */
    CefBrowser getCefBrowser();

}
