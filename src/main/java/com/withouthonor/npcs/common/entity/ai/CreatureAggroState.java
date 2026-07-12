package com.withouthonor.npcs.common.entity.ai;

/**
 * Серверный счётчик загруженных NPC, релевантных для «Типа существа» (непустой attackers() ИЛИ
 * defenders()). Пока счётчик 0 — canUse() у {@link WhCreatureAggroGoal}/{@link WhCreatureDefendGoal}
 * не запускает скан целей: на мирах без назначенных типов (в т.ч. существующих) лишних AABB-сканов
 * на мобофермах нет. Только главный серверный поток; на клиенте не используется.
 */
public final class CreatureAggroState {

    private static int loaded;

    private CreatureAggroState() {
    }

    public static boolean anyLoaded() {
        return loaded > 0;
    }

    /** Синхронизировать вклад одной сущности: was/now — считалась ли она раньше/сейчас. */
    public static void update(boolean was, boolean now) {
        if (was == now) {
            return;
        }
        loaded += now ? 1 : -1;
        if (loaded < 0) {
            loaded = 0; // страховка от рассинхрона (пропущенный remove и т.п.)
        }
    }

    /** Сброс на старте сервера — от дрейфа между мирами в одном процессе (одиночная игра). */
    public static void reset() {
        loaded = 0;
    }
}
