package com.synderisdev.createpipes.content.pipe;

import java.util.Locale;

public enum PipeConnectionMode {
    NORMAL,
    PULL,
    PUSH,
    NONE;

    public PipeConnectionMode next() {
        PipeConnectionMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public PipeConnectionMode previous() {
        PipeConnectionMode[] values = values();
        return values[(ordinal() + values.length - 1) % values.length];
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static PipeConnectionMode byName(String name) {
        for (PipeConnectionMode mode : values()) {
            if (mode.serializedName().equals(name)) {
                return mode;
            }
        }
        return NORMAL;
    }
}
