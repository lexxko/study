package model;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

public interface IMutable {
    @NotNull
    LocalDateTime getLastChangeDate();
    @NotNull
    Integer getChangesNumber();
}
