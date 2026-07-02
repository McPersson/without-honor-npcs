package com.withouthonor.npcs.compat.epicfight;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;

public class CompanionMobPatch extends HumanoidMobPatch<CompanionEntity> {

    public CompanionMobPatch() {
        super(Factions.NEUTRAL);
    }

    @Override
    public boolean overrideRender() {
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
