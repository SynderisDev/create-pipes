package com.synderisdev.createpipes.registry;

import com.synderisdev.createpipes.CreatePipes;
import com.synderisdev.createpipes.content.intake.SteamIntakeBlock;
import com.synderisdev.createpipes.content.pipe.SteamItemPipeBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreatePipes.MOD_ID);

    public static final DeferredBlock<SteamItemPipeBlock> STEAM_ITEM_PIPE = BLOCKS.registerBlock(
            "steam_item_pipe",
            SteamItemPipeBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.0F, 6.0F)
                    .sound(SoundType.COPPER)
                    .noOcclusion()
    );

    public static final DeferredBlock<SteamIntakeBlock> STEAM_INTAKE = BLOCKS.registerBlock(
            "steam_intake",
            SteamIntakeBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.COPPER)
                    .noOcclusion()
    );

    private ModBlocks() {
    }
}
