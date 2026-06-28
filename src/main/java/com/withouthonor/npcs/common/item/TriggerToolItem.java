package com.withouthonor.npcs.common.item;

import com.withouthonor.npcs.common.block.TriggerBlock;
import com.withouthonor.npcs.common.block.TriggerHelperBlock;
import com.withouthonor.npcs.common.block.TriggerHelperBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;

public class TriggerToolItem extends BlockItem {

    public TriggerToolItem(Block block, Properties props) {
        super(block, props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos clicked = ctx.getClickedPos();
        Block clickedBlock = level.getBlockState(clicked).getBlock();
        BlockPos placedPos = new BlockPlaceContext(ctx).getClickedPos();

        InteractionResult res = super.useOn(ctx);

        if (!level.isClientSide && res.consumesAction() && getBlock() instanceof TriggerHelperBlock) {
            BlockPos link = groupOf(level, clicked, clickedBlock);
            if (link != null && level.getBlockEntity(placedPos) instanceof TriggerHelperBlockEntity be) {
                be.setTriggerPos(link);
            }
        }
        return res;
    }

    @Nullable
    private static BlockPos groupOf(Level level, BlockPos clicked, Block clickedBlock) {
        if (clickedBlock instanceof TriggerBlock) {
            return clicked;
        }
        if (clickedBlock instanceof TriggerHelperBlock
                && level.getBlockEntity(clicked) instanceof TriggerHelperBlockEntity hbe) {
            return hbe.getTriggerPos();
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        if (getBlock() instanceof TriggerHelperBlock) {
            tip.add(Component.translatable("wh_npcs.tip.trigger_helper.1"));
            tip.add(Component.translatable("wh_npcs.tip.trigger_helper.2"));
            tip.add(Component.translatable("wh_npcs.tip.trigger_helper.3"));
        } else {
            tip.add(Component.translatable("wh_npcs.tip.trigger.1"));
            tip.add(Component.translatable("wh_npcs.tip.trigger.2"));
        }
        tip.add(Component.translatable("wh_npcs.tip.trigger.editor"));
    }
}
