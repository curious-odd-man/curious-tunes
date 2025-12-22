package com.github.curiousoddman.curious_tunes.backend.tags;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlbumCover {
    private byte[] data;
    private DataType type;

    public enum DataType {
        PNG, JPG
    }
}
