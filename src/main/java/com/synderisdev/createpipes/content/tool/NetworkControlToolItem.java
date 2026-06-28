package com.synderisdev.createpipes.content.tool;

import com.synderisdev.createpipes.content.pipe.SteamItemPipeBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public class NetworkControlToolItem extends Item {
    public NetworkControlToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return toggleNetwork(context.getLevel(), context.getClickedPos(), context.getPlayer());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.create_pipes.network_control_tool").withStyle(ChatFormatting.GRAY));
    }

    public static InteractionResult toggleNetwork(Level level, net.minecraft.core.BlockPos pos, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof SteamItemPipeBlockEntity pipe)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            boolean disabled = pipe.toggleNetworkDisabled(serverLevel);
            if (player != null) {
                player.displayClientMessage(Component.translatable(disabled
                        ? "message.create_pipes.network_disabled"
                        : "message.create_pipes.network_enabled"), true);
            }
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4F, disabled ? 0.7F : 1.1F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
