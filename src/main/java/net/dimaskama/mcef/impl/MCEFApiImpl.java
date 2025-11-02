package net.dimaskama.mcef.impl;

import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.EnumProgress;
import me.friwi.jcefmaven.IProgressHandler;
import net.dimaskama.mcef.api.MCEFApi;
import net.dimaskama.mcef.api.MCEFBrowser;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefRequestContext;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class MCEFApiImpl implements MCEFApi {

    private static volatile InitializationImpl initialization;
    private final CefApp cefApp;
    private final CefClient client;

    private MCEFApiImpl(InitializationImpl initialization) throws Exception {
        CefAppBuilder cefAppBuilder = new CefAppBuilder();
        cefAppBuilder.setInstallDir(MCEFModern.JCEF_PATH.toFile());
        cefAppBuilder.setProgressHandler(initialization);
        cefAppBuilder.addJcefArgs(
                "--autoplay-policy=no-user-gesture-required",
                "--disable-web-security",
                "--enable-widevine-cdm"
        );
        cefApp = cefAppBuilder.build();

        CefSettings cefSettings = new CefSettings();
        cefSettings.user_agent_product = "MCEF-Modern/0";
        cefSettings.root_cache_path = MCEFModern.CACHE_PATH.toAbsolutePath().toString();
        cefApp.setSettings(cefSettings);

        client = cefApp.createClient();
    }

    public static Initialization initialize() {
        if (initialization == null) {
            synchronized (MCEFApiImpl.class) {
                if (initialization == null) {
                    initialization = new InitializationImpl();
                }
            }
        }
        return initialization;
    }

    @Nullable
    public static Initialization getInitialization() {
        return initialization;
    }

    @Override
    public MCEFBrowser createBrowser(String url, boolean transparent) {
        MCEFBrowserImpl browser = new MCEFBrowserImpl(
                client,
                url,
                transparent,
                CefRequestContext.getGlobalContext(),
                null
        );
        browser.setCloseAllowed();
        browser.createImmediately();
        return browser;
    }

    public void close() {
        cefApp.dispose();
    }

    private static class InitializationImpl implements Initialization, IProgressHandler {

        private final CompletableFuture<MCEFApi> future;
        private volatile Stage stage = Stage.NOT_STARTED;
        private volatile float percentage = -1.0F;

        private InitializationImpl() {
            future = CompletableFuture.supplyAsync(() -> {
                try {
                    return new MCEFApiImpl(this);
                } catch (Throwable e) {
                    MCEFModern.LOGGER.error("Failed to initialize MCEF Modern", e);
                    stage = Stage.DONE;
                    percentage = -1;
                    throw new RuntimeException("Failed to initialize MCEF Modern", e);
                }
            });
        }

        @Override
        public Stage getStage() {
            return stage;
        }

        @Override
        public float getPercentage() {
            return percentage;
        }

        @Override
        public CompletableFuture<MCEFApi> getFuture() {
            return future;
        }

        @Override
        public void handleProgress(EnumProgress state, float percent) {
            stage = switch (state) {
                case LOCATING -> Stage.NOT_STARTED;
                case DOWNLOADING -> Stage.DOWNLOADING;
                case EXTRACTING -> Stage.EXTRACTING;
                case INSTALL -> Stage.INSTALL;
                case INITIALIZING -> Stage.INITIALIZING;
                case INITIALIZED -> Stage.DONE;
            };
            percentage = percent;
        }

    }

}
