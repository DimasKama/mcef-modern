package net.dimaskama.mcef.testmod;

import net.dimaskama.mcef.api.MCEFApi;
import net.fabricmc.api.ClientModInitializer;

public class MCEFTestMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MCEFApi.initialize();
    }

}
