package model;

import org.jetbrains.annotations.NotNull;

public interface IDirectory<T> extends IDbRecord<T> {
    @NotNull
    String getExternalCode();
}
