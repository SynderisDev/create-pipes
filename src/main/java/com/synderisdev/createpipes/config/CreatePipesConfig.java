package com.synderisdev.createpipes.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CreatePipesConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue REQUIRE_STEAM;
    public static final ModConfigSpec.IntValue ITEMS_PER_EXTRACT;
    public static final ModConfigSpec.IntValue EXTRACT_INTERVAL_TICKS;
    public static final ModConfigSpec.DoubleValue STEAM_UNITS_PER_BOILER_LEVEL_PER_TICK;
    public static final ModConfigSpec.DoubleValue STEAM_UNITS_PER_ITEM;
    public static final ModConfigSpec.IntValue NETWORK_STEAM_CAPACITY;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("transport");
        REQUIRE_STEAM = builder
                .comment("When true, item pipe networks need steam from a Steam Intake. When false, pipes run freely.")
                .define("require_steam", false);
        ITEMS_PER_EXTRACT = builder
                .comment("Maximum number of items pulled by one pull-side operation.")
                .defineInRange("items_per_extract", 8, 1, 64);
        EXTRACT_INTERVAL_TICKS = builder
                .comment("Ticks between active extraction attempts for each pipe network.")
                .defineInRange("extract_interval_ticks", 10, 1, 200);
        STEAM_UNITS_PER_BOILER_LEVEL_PER_TICK = builder
                .comment("Internal steam generated per active Create boiler level per tick.")
                .defineInRange("steam_units_per_boiler_level_per_tick", 1.0D, 0.01D, 1000D);
        STEAM_UNITS_PER_ITEM = builder
                .comment("Internal steam consumed for each item moved.")
                .defineInRange("steam_units_per_item", 2.0D, 0.01D, 1000D);
        NETWORK_STEAM_CAPACITY = builder
                .comment("Maximum internal steam buffer for one pipe network.")
                .defineInRange("network_steam_capacity", 1000, 1, 1_000_000);
        builder.pop();

        SPEC = builder.build();
    }

    private CreatePipesConfig() {
    }
}
