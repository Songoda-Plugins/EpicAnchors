package com.songoda.epicanchors;

import com.songoda.epicanchors.api.AnchorManager;

public final class EpicAnchorsApi {
    private static EpicAnchorsApi instance;

    private final AnchorManager anchorManager;

    private EpicAnchorsApi(AnchorManager anchorManager) {
        this.anchorManager = anchorManager;
    }

    public AnchorManager getAnchorManager() {
        return this.anchorManager;
    }

    public static EpicAnchorsApi getApi() {
        return instance;
    }

    static void initApi(AnchorManager anchorManager) {
        if (instance != null) {
            throw new IllegalStateException("EpicAnchorsApi already initialized");
        }

        instance = new EpicAnchorsApi(anchorManager);
    }
}
