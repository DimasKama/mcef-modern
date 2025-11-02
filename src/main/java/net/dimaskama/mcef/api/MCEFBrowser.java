package net.dimaskama.mcef.api;

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.cef.browser.CefBrowser;
import org.jetbrains.annotations.Nullable;

public interface MCEFBrowser extends AutoCloseable {

    void resize(int width, int height);

    void setFocus(boolean isFocused);

    void onMouseClicked(MouseButtonEvent event, boolean doubled);

    void onMouseReleased(MouseButtonEvent event);

    void onMouseScrolled(int x, int y, double amount);

    void onMouseMoved(int x, int y);

    void onKeyPressed(KeyEvent event);

    void onKeyReleased(KeyEvent event);

    void onCharTyped(CharacterEvent event);

    @Nullable
    GpuTexture getTexture();

    @Nullable
    GpuTextureView getTextureView();

    CursorType getCursorType();

    @Override
    void close();

    CefBrowser getCefBrowser();

}
