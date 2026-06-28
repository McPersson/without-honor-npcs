package com.withouthonor.npcs.common.item;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NpcMoverItem extends Item {

    private static final Map<UUID, UUID> SELECTION = new ConcurrentHashMap<>();

    public NpcMoverItem(Properties props) {
        super(props);
    }

    public static void clearSelection(UUID player) {
        SELECTION.remove(player);
    }

    private static void clearAndNotify(ServerPlayer sp) {
        if (!sp.hasPermissions(2)) {
            return;
        }
        boolean had = SELECTION.remove(sp.getUUID()) != null;
        sp.displayClientMessage(Component.translatable(had ? "wh_npcs.msg.mover.cleared" : "wh_npcs.msg.mover.no_target"), true);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide && player instanceof ServerPlayer sp) {
                clearAndNotify(sp);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (ctx.getPlayer() instanceof ServerPlayer sp) {
            if (sp.isShiftKeyDown()) {
                clearAndNotify(sp);
                return InteractionResult.CONSUME;
            }
            tryMove(sp, ctx.getClickedPos().relative(ctx.getClickedFace()));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                  InteractionHand hand) {
        if (target instanceof CompanionEntity npc) {
            if (!player.level().isClientSide && player instanceof ServerPlayer sp && sp.hasPermissions(2)) {
                SELECTION.put(sp.getUUID(), npc.getUUID());
                sp.displayClientMessage(Component.translatable("wh_npcs.msg.mover.selected", npc.getName().getString()), true);
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        return InteractionResult.PASS;
    }

    public static boolean tryMove(ServerPlayer sp, BlockPos feet) {
        if (!sp.hasPermissions(2)) {
            return false;
        }
        UUID sel = SELECTION.get(sp.getUUID());
        if (sel == null) {
            sp.displayClientMessage(Component.translatable("wh_npcs.msg.mover.select_first"), true);
            return true;
        }
        Entity e = ((ServerLevel) sp.level()).getEntity(sel);
        if (!(e instanceof CompanionEntity npc)) {
            sp.displayClientMessage(Component.translatable("wh_npcs.msg.mover.not_found"), true);
            return true;
        }
        npc.getNavigation().stop();

        npc.teleportTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
        sp.displayClientMessage(Component.translatable("wh_npcs.msg.mover.moved", npc.getName().getString()), true);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("wh_npcs.tip.npc_mover.1"));
        tip.add(Component.translatable("wh_npcs.tip.npc_mover.2"));
        tip.add(Component.translatable("wh_npcs.tip.npc_mover.3"));
        tip.add(Component.translatable("wh_npcs.tip.npc_mover.4"));
    }
}
