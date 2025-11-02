package net.dimaskama.mcef.impl;

import net.dimaskama.mcef.api.MCEFApi;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class MCEFModern implements ClientModInitializer {

    public static final String MOD_ID = "mcef-modern";
    public static final Logger LOGGER = LoggerFactory.getLogger("MCEF Modern");
    public static final Path MOD_DIR = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
    public static final Path JCEF_PATH = MOD_DIR.resolve("jcef");
    public static final Path CACHE_PATH = MOD_DIR.resolve("cache");

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STOPPING.register(MCEFModern::onClientStopping);
    }

    private static void onClientStopping(Minecraft mc) {
        MCEFApi.Initialization initialization = MCEFApiImpl.getInitialization();
        if (initialization != null && initialization.getStage() != MCEFApi.Initialization.Stage.DOWNLOADING) {
            ((MCEFApiImpl) initialization.getFuture().join()).close();
        }
    }

}