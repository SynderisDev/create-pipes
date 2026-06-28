package com.synderisdev.createpipes.content.pipe;

public final class PipeModeRules {
    private PipeModeRules() {
    }

    public static boolean connects(PipeConnectionMode mode) {
        return mode != PipeConnectionMode.NONE;
    }

    public static boolean activelyExtracts(PipeConnectionMode mode) {
        return mode == PipeConnectionMode.PULL;
    }

    public static boolean acceptsActiveInsertion(PipeConnectionMode mode) {
        return mode == PipeConnectionMode.PUSH || mode == PipeConnectionMode.NORMAL;
    }

    public static boolean acceptsExternalInsertion(PipeConnectionMode mode) {
        return mode == PipeConnectionMode.NORMAL;
    }
}
