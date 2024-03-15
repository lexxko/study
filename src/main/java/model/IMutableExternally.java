package model;

import org.jetbrains.annotations.NotNull;

public interface IMutableExternally extends IMutable {
    @NotNull
    String getLastChangeInitiator();
}
