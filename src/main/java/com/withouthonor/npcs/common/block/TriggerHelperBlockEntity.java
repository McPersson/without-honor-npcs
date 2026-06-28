package com.withouthonor.npcs.common.block;

import com.withouthonor.npcs.common.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class TriggerHelperBlockEntity extends BlockEntity {

    @Nullable
    private BlockPos triggerPos;

    public TriggerHelperBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRIGGER_HELPER.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) {
            TriggerClientIndex.add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) {
            TriggerClientIndex.remove(this);
        }
    }

    @Nullable
    public BlockPos getTriggerPos() {
        return triggerPos;
    }

    public void setTriggerPos(@Nullable BlockPos pos) {
        this.triggerPos = pos;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (triggerPos != null) {
            tag.putBoolean("HasTrigger", true);
            tag.putInt("TX", triggerPos.getX());
            tag.putInt("TY", triggerPos.getY());
            tag.putInt("TZ", triggerPos.getZ());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        triggerPos = tag.getBoolean("HasTrigger")
                ? new BlockPos(tag.getInt("TX"), tag.getInt("TY"), tag.getInt("TZ"))
                : null;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            load(tag);
        }
    }
}
