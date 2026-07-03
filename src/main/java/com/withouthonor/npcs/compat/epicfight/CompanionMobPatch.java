package com.withouthonor.npcs.compat.epicfight;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;

public class CompanionMobPatch extends HumanoidMobPatch<CompanionEntity> {

    public CompanionMobPatch() {
        super(Factions.NEUTRAL);
    }

    @Override
    public boolean overrideRender() {
        // Клиентский класс нельзя резолвить на дедике — только за dist-гардом.
        return FMLEnvironment.dist.isClient() && clientOverrideRender();
    }

    private boolean clientOverrideRender() {
        return EpicFightClientCompat.isEpicFightRendered(this.getOriginal());
    }

    @Override
    public void updateMotion(boolean considerInaction) {
        super.commonMobUpdateMotion(considerInaction);
    }

    @Override
    public void initAnimator(Animator animator) {
        super.initAnimator(animator);
        super.commonMobAnimatorInit(animator);
    }

    @Override
    public void tick(LivingEvent.LivingTickEvent event) {
        if (!this.getOriginal().isEpicFightMode()) {
            return;
        }
        super.tick(event);
    }

    @Override
    protected void initAI() {
    }

    @Override
    public boolean applyStun(yesman.epicfight.world.damagesource.StunType stunType, float stunTime) {
        if (!this.getOriginal().isEpicFightMode()) {
            return false;
        }
        return super.applyStun(stunType, stunTime);
    }
}
