package com.withouthonor.npcs.mixin;

import com.withouthonor.npcs.common.entity.CompanionEntity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Расширяет проверку союзника Iron's Spells (Utils.shouldHealEntity) нашим понятием союзника:
 * фракция + хозяин (см. CompanionEntity.whIsSupportAlly). Без этого «Поддержка» лечит только себя,
 * т.к. ISS опирается на ванильные команды/isAlliedTo и про наши фракции/следование не знает.
 *
 * Применяется ТОЛЬКО при наличии ISS (гейт в WhMixinPlugin). Направление проверки не важно —
 * смотрим обе стороны: если один из участников наш NPC, а другой ему союзник — лечение разрешаем.
 */
@Mixin(value = Utils.class, remap = false)
public class IssHealAllyMixin {

    @Inject(method = "shouldHealEntity", at = @At("HEAD"), cancellable = true, remap = false)
    private static void wh$supportAllies(Entity a, Entity b, CallbackInfoReturnable<Boolean> cir) {
        if (a instanceof CompanionEntity npc && npc.whIsSupportAlly(b)) {
            cir.setReturnValue(true);
        } else if (b instanceof CompanionEntity npc && npc.whIsSupportAlly(a)) {
            cir.setReturnValue(true);
        }
    }
}
