package com.synderisdev.createpipes.client;

import com.synderisdev.createpipes.client.render.PipeFilterOverlay;
import com.synderisdev.createpipes.client.render.SteamItemPipeRenderer;
import com.synderisdev.createpipes.registry.ModBlockEntityTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class CreatePipesClient {
    private CreatePipesClient() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(CreatePipesClient::registerRenderers);
        NeoForge.EVENT_BUS.addListener(PipeFilterOverlay::tick);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntityTypes.STEAM_ITEM_PIPE.get(), SteamItemPipeRenderer::new);
    }
}
