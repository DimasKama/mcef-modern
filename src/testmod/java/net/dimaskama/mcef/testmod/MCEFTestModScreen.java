package net.dimaskama.mcef.testmod;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.dimaskama.mcef.api.MCEFApi;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2f;

public class MCEFTestModScreen extends Screen {

    private final Screen parent;
    private MCEFBrowser browser;

    public MCEFTestModScreen(Screen parent) {
        super(Component.empty());
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (browser == null) {
            browser = MCEFApi.getInstance().createBrowser(
                    "https://youtu.be/nsWBsBAR3AY",
                    false
            );
        }
        browser.resize(width, height);
        browser.setFocus(true);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GpuTextureView gpuTextureView = browser.getTextureView();
        if (gpuTextureView != null) {
            guiGraphics.guiRenderState.submitGuiElement(new BlitRenderState(
                    RenderPipelines.GUI_TEXTURED,
                    TextureSetup.singleTexture(gpuTextureView),
                    new Matrix3x2f(guiGraphics.pose()),
                    0,
                    0,
                    width,
                    height,
                    0.0F,
                    1.0F,
                    0.0F,
                    1.0F,
                    0xFFFFFFFF,
                    guiGraphics.scissorStack.peek()
            ));
        }
        guiGraphics.requestCursor(browser.getCursorType());
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        if (browser != null) {
            browser.close();
            browser = null;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        browser.onMouseClicked(event, doubled);
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        browser.onMouseReleased(event);
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        browser.onMouseScrolled((int) mouseX, (int) mouseY, verticalAmount);
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void mouseMoved(double x, double y) {
        browser.onMouseMoved((int) x, (int) y);
        super.mouseMoved(x, y);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        browser.onKeyPressed(event);
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        browser.onKeyReleased(event);
        return super.keyReleased(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        browser.onCharTyped(event);
        return super.charTyped(event);
    }

}
