package com.synderisdev.createpipes.registry;

import com.synderisdev.createpipes.CreatePipes;
import com.synderisdev.createpipes.content.intake.SteamIntakeBlockEntity;
import com.synderisdev.createpipes.content.pipe.SteamItemPipeBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreatePipes.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamItemPipeBlockEntity>> STEAM_ITEM_PIPE =
            BLOCK_ENTITY_TYPES.register("steam_item_pipe", () ->
                    BlockEntityType.Builder.of(SteamItemPipeBlockEntity::new, ModBlocks.STEAM_ITEM_PIPE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamIntakeBlockEntity>> STEAM_INTAKE =
            BLOCK_ENTITY_TYPES.register("steam_intake", () ->
                    BlockEntityType.Builder.of(SteamIntakeBlockEntity::new, ModBlocks.STEAM_INTAKE.get()).build(null));

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                STEAM_ITEM_PIPE.get(),
                (pipe, side) -> pipe.getItemHandler(side)
        );
    }

    private ModBlockEntityTypes() {
    }
}
