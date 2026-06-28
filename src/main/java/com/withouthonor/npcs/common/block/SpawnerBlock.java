package com.withouthonor.npcs.common.block;

import com.withouthonor.npcs.common.registry.ModBlockEntities;
import com.withouthonor.npcs.common.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class SpawnerBlock extends Block implements EntityBlock {

    public SpawnerBlock(Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpawnerBlockEntity(pos, state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource rand) {
        if (!(level.getBlockEntity(pos) instanceof SpawnerBlockEntity be) || !be.isActive()) {
            return;
        }
        double x = pos.getX() + rand.nextDouble();
        double y = pos.getY() + rand.nextDouble();
        double z = pos.getZ() + rand.nextDouble();
        level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
        level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME, x, y, z, 0.0, 0.0, 0.0);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.SPAWNER.get(), SpawnerBlockEntity::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        float hardness = 2.0F;
        if (level.getBlockEntity(pos) instanceof SpawnerBlockEntity be) {
            hardness = be.getHardness();
        }
        if (hardness < 0.0F) {
            return 0.0F;
        }
        int i = player.hasCorrectToolForDrops(state) ? 30 : 100;
        return player.getDestroySpeed(state) / hardness / (float) i;
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SpawnerBlockEntity be) {
            if (be.getBreakXp() > 0 && level instanceof net.minecraft.server.level.ServerLevel sl
                    && !player.isCreative()) {
                ExperienceOrb.award(sl, Vec3.atCenterOf(pos), be.getBreakXp());
            }
            if (!player.isCreative()) {
                ItemStack stack = new ItemStack(ModItems.NPC_AUTO_SPAWNER.get());
                CompoundTag beTag = be.saveWithoutMetadata();
                stack.addTagElement("BlockEntityTag", beTag);
                popResource(level, pos, stack);
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }
}
