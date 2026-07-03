package com.withouthonor.npcs.common.item;

import com.withouthonor.npcs.common.block.TriggerBlock;
import com.withouthonor.npcs.common.block.TriggerBlockEntity;
import com.withouthonor.npcs.common.block.TriggerHelperBlock;
import com.withouthonor.npcs.common.block.TriggerHelperBlockEntity;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import com.withouthonor.npcs.common.profile.CompanionProfile;
import com.withouthonor.npcs.common.storage.ProfileManager;
import com.withouthonor.npcs.network.EditorDataPacket;
import com.withouthonor.npcs.network.NetworkHandler;
import com.withouthonor.npcs.network.TriggerEditPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class MemorionFeatherItem extends Item {

    public MemorionFeatherItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Block block = level.getBlockState(pos).getBlock();
        if (block instanceof TriggerBlock || block instanceof TriggerHelperBlock) {
            if (!level.isClientSide && ctx.getPlayer() instanceof ServerPlayer sp && sp.hasPermissions(2)) {
                openTriggerEditor(level, pos, block, sp);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (block instanceof com.withouthonor.npcs.common.block.SpawnerBlock) {
            if (!level.isClientSide && ctx.getPlayer() instanceof ServerPlayer sp && sp.hasPermissions(2)) {
                com.withouthonor.npcs.network.SpawnerPackets.openEditor(sp, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                  InteractionHand hand) {
        if (target instanceof CompanionEntity npc) {
            if (!player.level().isClientSide && player instanceof ServerPlayer sp && sp.hasPermissions(2)) {
                UUID pid = npc.getProfileId();
                CompanionProfile profile = pid != null ? ProfileManager.get().get(pid) : null;
                if (profile != null) {
                    EditorDataPacket.send(sp, profile, npc.getId());
                }
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        return InteractionResult.PASS;
    }

    private static void openTriggerEditor(Level level, BlockPos clicked, Block clickedBlock, ServerPlayer sp) {
        BlockPos triggerPos = null;
        boolean viaHelper = false;
        if (clickedBlock instanceof TriggerBlock) {
            triggerPos = clicked;
        } else if (clickedBlock instanceof TriggerHelperBlock
                && level.getBlockEntity(clicked) instanceof TriggerHelperBlockEntity hbe) {
            viaHelper = true;
            triggerPos = hbe.getTriggerPos();
        }
        if (triggerPos != null && level.getBlockEntity(triggerPos) instanceof TriggerBlockEntity be) {
            NetworkHandler.sendToPlayer(new TriggerEditPacket(triggerPos, be.isOnce(),
                    be.actionsJson(), be.conditionsJson(), viaHelper, be.enterDirByte(),
                    be.getTargetNpc(), be.targetNpcName()), sp);
        } else {
            sp.displayClientMessage(Component.translatable("wh_npcs.msg.feather.helper_unbound"), true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("wh_npcs.tip.memorion_feather.1"));
        tip.add(Component.translatable("wh_npcs.tip.memorion_feather.2"));
        tip.add(Component.translatable("wh_npcs.tip.memorion_feather.3"));
    }
}
