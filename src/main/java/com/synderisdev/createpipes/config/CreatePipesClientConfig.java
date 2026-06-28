package com.synderisdev.createpipes.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CreatePipesClientConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue SHOW_PIPE_MODE_MARKERS;
    public static final ModConfigSpec.IntValue PIPE_MODE_MARKER_RENDER_DISTANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("visuals");
        SHOW_PIPE_MODE_MARKERS = builder
                .comment("When true, pipe pull/push sides render small colored collars.")
                .define("show_pipe_mode_markers", true);
        PIPE_MODE_MARKER_RENDER_DISTANCE = builder
                .comment("Maximum distance in blocks for pipe pull/push mode markers.")
                .defineInRange("pipe_mode_marker_render_distance", 24, 8, 128);
        builder.pop();

        SPEC = builder.build();
    }

    private CreatePipesClientConfig() {
    }
}
