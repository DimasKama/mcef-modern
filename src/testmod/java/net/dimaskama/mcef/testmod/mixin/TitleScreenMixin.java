package net.dimaskama.mcef.testmod.mixin;

import net.dimaskama.mcef.testmod.MCEFTestModScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
abstract class TitleScreenMixin extends Screen {

    private TitleScreenMixin(Component component) {
        super(component);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void initTail(CallbackInfo ci) {
        addRenderableWidget(Button.builder(Component.literal("MCEF Test"), narratorButton ->
                        minecraft.setScreen(new MCEFTestModScreen(null)))
                .bounds(width - 120, height / 2 - 10, 100, 20)
                .build());
    }

}
