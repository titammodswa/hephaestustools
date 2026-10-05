package com.titammods.hephaestus_tools.table;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MasteryStreak {
    private MasteryStreak() {}

    private static final Map<UUID, long[]> MINE = new HashMap<>();
    private static final Map<UUID, long[]> COMBAT = new HashMap<>();
    private static final Map<UUID, long[]> KILLS = new HashMap<>();

    public static void clear(UUID id) {
        MINE.remove(id);
        COMBAT.remove(id);
        KILLS.remove(id);
    }

    public static void clearAll() {
        MINE.clear();
        COMBAT.clear();
        KILLS.clear();
    }

    public static int mine(UUID id, long now, long timeout) {
        long[] s = MINE.get(id);
        if (s == null || now > s[1]) s = new long[]{0, 0};
        s[0]++; s[1] = now + timeout; MINE.put(id, s);
        return (int) s[0];
    }
    public static int mineCount(UUID id, long now) {
        long[] s = MINE.get(id);
        return (s == null || now > s[1]) ? 0 : (int) s[0];
    }

    public static int combat(UUID id, int target, long now, long timeout) {
        long[] s = COMBAT.get(id);
        if (s == null || now > s[1] || s[2] != target) s = new long[]{0, 0, target};
        s[0]++; s[1] = now + timeout; s[2] = target; COMBAT.put(id, s);
        return (int) s[0];
    }

    public static int nextCombatCount(UUID id, int target, long now) {
        long[] streak = COMBAT.get(id);
        return streak == null || now > streak[1] || streak[2] != target ? 1 : (int) streak[0] + 1;
    }

    public static int kill(UUID id, long now, long timeout) {
        long[] s = KILLS.get(id);
        if (s == null || now > s[1]) s = new long[]{0, 0};
        s[0]++; s[1] = now + timeout; KILLS.put(id, s);
        return (int) s[0];
    }
    public static int killCount(UUID id, long now) {
        long[] s = KILLS.get(id);
        return (s == null || now > s[1]) ? 0 : (int) s[0];
    }
}