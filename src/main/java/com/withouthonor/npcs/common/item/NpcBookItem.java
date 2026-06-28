package com.withouthonor.npcs.common.item;

import com.withouthonor.npcs.network.NetworkHandler;
import com.withouthonor.npcs.network.ProfileSharePackets;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class NpcBookItem extends Item {

    public NpcBookItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            NetworkHandler.sendToServer(new ProfileSharePackets.BookRequest());
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("wh_npcs.tip.npc_book.1"));
        tip.add(Component.translatable("wh_npcs.tip.npc_book.2"));
        tip.add(Component.translatable("wh_npcs.tip.npc_book.3"));
    }
}
