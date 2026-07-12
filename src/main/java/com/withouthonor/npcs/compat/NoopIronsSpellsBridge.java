package com.withouthonor.npcs.compat;

import com.withouthonor.npcs.common.entity.CompanionEntity;

public final class NoopIronsSpellsBridge implements IronsSpellsBridge {

    public static final NoopIronsSpellsBridge INSTANCE = new NoopIronsSpellsBridge();

    private NoopIronsSpellsBridge() {
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public boolean castSpell(CompanionEntity npc, String spellId, int spellLevel) {
        return false;
    }

    @Override
    public boolean castSpellAt(CompanionEntity npc, String spellId, int spellLevel,
                               net.minecraft.world.entity.LivingEntity aimTarget) {
        return false;
    }

    @Override
    public void tick(CompanionEntity npc) {
    }

    @Override
    public boolean isCastingNow(CompanionEntity npc) {
        return false;
    }

    @Override
    public void cancel(CompanionEntity npc) {
    }

    @Override
    public net.minecraft.world.entity.ai.goal.Goal buildMageGoal(
            CompanionEntity npc, com.withouthonor.npcs.common.profile.CompanionProfile profile) {
        return null;
    }

    @Override
    public net.minecraft.world.entity.ai.goal.Goal buildSupportFinderGoal(CompanionEntity npc) {
        return null;
    }

    @Override
    public net.minecraft.world.entity.ai.goal.Goal buildSupportCastGoal(
            CompanionEntity npc, com.withouthonor.npcs.common.profile.CompanionProfile profile) {
        return null;
    }

    @Override
    public java.util.List<SpellInfo> listSpells() {
        return java.util.List.of();
    }

    @Override
    public java.util.List<MagicAttrInfo> listMagicAttributes() {
        return java.util.List.of();
    }

    @Override
    public java.util.List<String> magicAttributeIds() {
        return java.util.List.of();
    }

    @Override
    public java.util.List<String> readEquippedSpellbookSpellIds(net.minecraft.world.entity.LivingEntity entity) {
        return java.util.List.of();
    }
}
