package net.dimaskama.mcef.impl;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

import java.io.IOException;
import java.nio.file.Files;

public class MCEFModernPreLaunch implements PreLaunchEntrypoint {

    @Override
    public void onPreLaunch() {
        System.setProperty("java.awt.headless", "false");
        try {
            Files.createDirectories(MCEFModern.JCEF_PATH);
            Files.createDirectories(MCEFModern.CACHE_PATH);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create MCEF directories", e);
        }
    }

}
