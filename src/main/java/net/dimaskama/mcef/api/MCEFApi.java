package net.dimaskama.mcef.api;

import net.dimaskama.mcef.impl.MCEFApiImpl;

import java.util.concurrent.CompletableFuture;

public interface MCEFApi {

    static Initialization initialize() {
        return MCEFApiImpl.initialize();
    }

    static CompletableFuture<MCEFApi> getInstanceFuture() {
        return initialize().getFuture();
    }

    static MCEFApi getInstance() {
        return getInstanceFuture().join();
    }

    MCEFBrowser createBrowser(String url, boolean transparent);

    interface Initialization {

        Initialization.Stage getStage();

        float getPercentage();

        CompletableFuture<MCEFApi> getFuture();

        default boolean isDone() {
            return getStage() == Stage.DONE;
        }

        enum Stage {

            NOT_STARTED,
            DOWNLOADING,
            EXTRACTING,
            INSTALL,
            INITIALIZING,
            DONE

        }

    }

}
