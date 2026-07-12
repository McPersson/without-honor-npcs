package com.withouthonor.npcs.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Гейт применения наших миксинов по наличию сторонних модов.
 *
 * IssCompanionMixin инжектит интерфейс IMagicEntity (Iron's Spells) в CompanionEntity —
 * без ISS этот класс не должен даже применяться, иначе верификатор потянет отсутствующие
 * классы ISS и уронит загрузку. ModList на этапе применения миксинов ещё не готов,
 * поэтому проверяем через LoadingModList (доступен с ранней стадии загрузки FML).
 */
public final class WhMixinPlugin implements IMixinConfigPlugin {

    private static final java.util.Set<String> ISS_MIXINS = java.util.Set.of(
            "com.withouthonor.npcs.mixin.IssCompanionMixin",
            "com.withouthonor.npcs.mixin.IssHealAllyMixin");

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (ISS_MIXINS.contains(mixinClassName)) {
            return net.minecraftforge.fml.loading.LoadingModList.get()
                    .getModFileById("irons_spellbooks") != null;
        }
        return true;
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
