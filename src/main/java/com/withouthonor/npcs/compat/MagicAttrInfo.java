package com.withouthonor.npcs.compat;

/**
 * DTO магического атрибута Iron's Spells для клиентского UI — без ISS-типов (изоляция как у SpellInfo).
 *
 * Все 21 атрибут ISS — это MagicPercentAttribute(имя, 1.0, -100, 100), т.е. нейтральное значение 1.0.
 * В редакторе показываем проценты: pct = (attr - 1) * 100, где 0 = «как у всех».
 *
 * @param id     строковый id атрибута ("irons_spellbooks:fire_magic_resist")
 * @param name   локализованное имя атрибута
 * @param school локализованное имя школы, либо пустая строка для общих атрибутов
 * @param color  ARGB-цвет школы (0 для общих)
 * @param power  true — сила заклинаний школы, false — сопротивление
 */
public record MagicAttrInfo(String id, String name, String school, int color, boolean power) {

    public boolean isSchool() {
        return !school.isEmpty();
    }
}
