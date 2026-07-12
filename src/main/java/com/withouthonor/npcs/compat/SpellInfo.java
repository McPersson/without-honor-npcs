package com.withouthonor.npcs.compat;

import net.minecraft.resources.ResourceLocation;

/**
 * DTO спелла Iron's Spells для клиентского UI — без ISS-типов, чтобы экран лоадаута
 * (client/gui) не линковал классы ISS (изоляция как у остального compat-слоя).
 *
 * @param id     строковый id спелла ("irons_spellbooks:fireball")
 * @param name   локализованное имя
 * @param icon   текстура иконки (AbstractSpell.getSpellIconResource)
 * @param color  ARGB-цвет школы (SchoolType.getTargetingColor)
 */
public record SpellInfo(String id, String name, ResourceLocation icon, int color) {
}
