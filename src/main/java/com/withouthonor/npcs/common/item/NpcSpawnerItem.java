package com.withouthonor.npcs.common.item;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import com.withouthonor.npcs.common.profile.CompanionProfile;
import com.withouthonor.npcs.common.registry.ModEntities;
import com.withouthonor.npcs.common.skin.DefaultSkins;
import com.withouthonor.npcs.common.storage.ProfileManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class NpcSpawnerItem extends Item {

    public NpcSpawnerItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(ctx.getPlayer() instanceof ServerPlayer player) || !player.hasPermissions(2)) {
            return InteractionResult.PASS;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());

        CompanionProfile profile = ProfileManager.get().create("NPC");

        DefaultSkins.DefaultSkin skin =
                DefaultSkins.ALL.get(serverLevel.getRandom().nextInt(DefaultSkins.ALL.size()));
        profile.setSkinPlayerName(skin.spec());
        ProfileManager.get().save(profile);
        CompanionEntity npc = ModEntities.COMPANION.get().create(serverLevel);
        if (npc == null) {
            return InteractionResult.FAIL;
        }
        float yaw = player.getYRot() + 180.0F;
        npc.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0F);
        npc.setYBodyRot(yaw);
        npc.setYHeadRot(yaw);
        npc.setProfileId(profile.getId());
        npc.setCustomName(Component.literal("NPC"));
        serverLevel.addFreshEntity(npc);

        player.displayClientMessage(Component.translatable("wh_npcs.msg.spawner.created"), true);
        return InteractionResult.CONSUME;
    }
}
