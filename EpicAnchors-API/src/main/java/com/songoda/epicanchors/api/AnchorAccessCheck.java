package com.songoda.epicanchors.api;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface AnchorAccessCheck {
    boolean check(@NotNull Anchor anchor, @NotNull UUID uuid);
}
