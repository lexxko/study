package model;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

public interface IDbRecord<T> {
    @NotNull
    T getId();

    @NotNull
    LocalDateTime getDateCreated();

    @NotNull
    Boolean isDeprecated();
}
