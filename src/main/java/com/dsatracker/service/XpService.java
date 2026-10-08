package com.dsatracker.service;

import com.dsatracker.model.Difficulty;
import org.springframework.stereotype.Service;

/**
 * XP/level math, kept in one place so the award amounts and the level curve
 * can't drift apart between {@link JudgeService} (awards it) and
 * {@link AuthService} (reports level/progress on the profile).
 */
@Service
public class XpService {

    /** First-time-ACCEPTED XP by difficulty. Deliberately not linear -- Hard should feel worth it. */
    public long xpForDifficulty(Difficulty difficulty) {
        if (difficulty == null) return 10;
        return switch (difficulty) {
            case EASY -> 10;
            case MEDIUM -> 25;
            case HARD -> 50;
        };
    }

    /** Level N requires N(N-1)/2 * 100 total XP -- each level costs 100 XP more than the last. */
    public int levelForXp(long xp) {
        int level = 1;
        while (xp >= xpForLevelStart(level + 1)) {
            level++;
        }
        return level;
    }

    /** Total cumulative XP needed to *reach* this level (level 1 starts at 0, level 2 at 100, level 3 at 300, ...). */
    public long xpForLevelStart(int level) {
        long n = level - 1;
        return n * (n + 1) / 2 * 100;
    }

    public long xpIntoCurrentLevel(long xp) {
        return xp - xpForLevelStart(levelForXp(xp));
    }

    public long xpForNextLevel(long xp) {
        int level = levelForXp(xp);
        return xpForLevelStart(level + 1) - xpForLevelStart(level);
    }
}
