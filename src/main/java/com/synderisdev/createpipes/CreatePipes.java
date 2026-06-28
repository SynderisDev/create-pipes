package com.synderisdev.createpipes;

import com.mojang.logging.LogUtils;
import com.synderisdev.createpipes.client.CreatePipesClient;
import com.synderisdev.createpipes.command.PerfTestCommands;
import com.synderisdev.createpipes.config.CreatePipesClientConfig;
import com.synderisdev.createpipes.config.CreatePipesConfig;
import com.synderisdev.createpipes.registry.ModBlockEntityTypes;
import com.synderisdev.createpipes.registry.ModBlocks;
import com.synderisdev.createpipes.registry.ModCreativeTabs;
import com.synderisdev.createpipes.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.slf4j.Logger;

@Mod(CreatePipes.MOD_ID)
public class CreatePipes {
    public static final String MOD_ID = "create_pipes";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreatePipes(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(this::registerCapabilities);
        NeoForge.EVENT_BUS.addListener(PerfTestCommands::register);
        modContainer.registerConfig(ModConfig.Type.CLIENT, CreatePipesClientConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.SERVER, CreatePipesConfig.SPEC);

        if (FMLEnvironment.dist.isClient()) {
            CreatePipesClient.register(modEventBus);
        }
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        ModBlockEntityTypes.registerCapabilities(event);
    }
}
