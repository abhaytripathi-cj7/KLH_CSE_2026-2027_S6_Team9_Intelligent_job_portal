package com.dsa.jobportal.algorithms.dp;

import com.dsa.jobportal.model.Candidate;
import com.dsa.jobportal.model.MatchResult;

/**
 * Bitmask/subset-state DP used by the "Skill Upgrade Planner".
 *
 * The state is the subset of currently considered missing skills that the
 * candidate chooses to learn. The recurrence adds one least-significant skill
 * to a smaller subset and accumulates its marginal coverage utility across the
 * target jobs, including a completion bonus when a job's full skill set becomes
 * covered. This makes the subset interaction visible and relevant to the portal.
 */
public class SkillUpgradePlanner {
    public static class Plan {
        public final String[] selectedSkills;
        public final double baselineUtility;
        public final double projectedUtility;
        public final int evaluatedStates;
        public final int candidateSkillUniverse;

        public Plan(String[] selectedSkills, double baselineUtility, double projectedUtility,
                    int evaluatedStates, int candidateSkillUniverse) {
            this.selectedSkills = selectedSkills;
            this.baselineUtility = baselineUtility;
            this.projectedUtility = projectedUtility;
            this.evaluatedStates = evaluatedStates;
            this.candidateSkillUniverse = candidateSkillUniverse;
        }
    }

    public Plan plan(Candidate candidate, MatchResult[] rankedJobs, int topJobs, int skillBudget) {
        int jobsCount = Math.max(0, Math.min(topJobs, rankedJobs.length));
        int budget = Math.max(0, Math.min(4, skillBudget));
        String[] existing = candidate.skillArray();

        // Keep the state space bounded for a classroom web demo: 2^15 = 32768 states.
        String[] universe = new String[15];
        int universeSize = 0;
        for (int i = 0; i < jobsCount && universeSize < universe.length; i++) {
            String[] required = rankedJobs[i].getJob().skillArray();
            for (int j = 0; j < required.length && universeSize < universe.length; j++) {
                if (containsIgnoreCase(existing, required[j]) || containsIgnoreCase(universe, universeSize, required[j])) continue;
                universe[universeSize++] = required[j];
            }
        }

        if (universeSize == 0 || budget == 0 || jobsCount == 0) {
            return new Plan(new String[0], baseUtility(existing, rankedJobs, jobsCount),
                    baseUtility(existing, rankedJobs, jobsCount), 1, universeSize);
        }

        int[] jobMasks = new int[jobsCount];
        int[] baseMatched = new int[jobsCount];
        int[] totalSkills = new int[jobsCount];
        for (int i = 0; i < jobsCount; i++) {
            String[] required = rankedJobs[i].getJob().skillArray();
            totalSkills[i] = required.length;
            for (int r = 0; r < required.length; r++) {
                if (containsIgnoreCase(existing, required[r])) {
                    baseMatched[i]++;
                } else {
                    int idx = indexOfIgnoreCase(universe, universeSize, required[r]);
                    if (idx >= 0) jobMasks[i] |= (1 << idx);
                }
            }
        }

        int states = 1 << universeSize;
        double[] dp = new double[states];
        dp[0] = utility(baseMatched, totalSkills, null, 0);
        int bestMask = 0;
        double best = dp[0];
        int evaluated = 1;

        for (int mask = 1; mask < states; mask++) {
            int count = bitCount(mask);
            if (count > budget) continue;
            int bitIndex = leastBitIndex(mask);
            int prev = mask ^ (1 << bitIndex);
            dp[mask] = dp[prev];

            for (int j = 0; j < jobsCount; j++) {
                if ((jobMasks[j] & (1 << bitIndex)) == 0 || totalSkills[j] == 0) continue;
                int before = baseMatched[j] + bitCount(prev & jobMasks[j]);
                int after = before + 1;
                dp[mask] += 100.0 / totalSkills[j];
                if (before < totalSkills[j] && after == totalSkills[j]) dp[mask] += 35.0;
            }

            evaluated++;
            if (dp[mask] > best) {
                best = dp[mask];
                bestMask = mask;
            }
        }

        int selectedCount = bitCount(bestMask);
        String[] selected = new String[selectedCount];
        int p = 0;
        for (int i = 0; i < universeSize; i++) if ((bestMask & (1 << i)) != 0) selected[p++] = universe[i];
        return new Plan(selected, dp[0], best, evaluated, universeSize);
    }

    private double baseUtility(String[] existing, MatchResult[] rankedJobs, int jobsCount) {
        double sum = 0;
        for (int i = 0; i < jobsCount; i++) {
            String[] required = rankedJobs[i].getJob().skillArray();
            if (required.length == 0) continue;
            int matched = 0;
            for (int r = 0; r < required.length; r++) if (containsIgnoreCase(existing, required[r])) matched++;
            sum += 100.0 * matched / required.length;
            if (matched == required.length) sum += 35.0;
        }
        return sum;
    }

    private double utility(int[] baseMatched, int[] totalSkills, int[] ignored, int ignoredMask) {
        double sum = 0;
        for (int i = 0; i < totalSkills.length; i++) {
            if (totalSkills[i] == 0) continue;
            sum += 100.0 * baseMatched[i] / totalSkills[i];
            if (baseMatched[i] == totalSkills[i]) sum += 35.0;
        }
        return sum;
    }

    private int bitCount(int value) {
        int count = 0;
        while (value != 0) { value &= value - 1; count++; }
        return count;
    }

    private int leastBitIndex(int mask) {
        int idx = 0;
        while ((mask & 1) == 0) { mask >>>= 1; idx++; }
        return idx;
    }

    private boolean containsIgnoreCase(String[] values, String target) {
        return containsIgnoreCase(values, values.length, target);
    }

    private boolean containsIgnoreCase(String[] values, int length, String target) {
        for (int i = 0; i < length; i++) if (values[i] != null && values[i].equalsIgnoreCase(target)) return true;
        return false;
    }

    private int indexOfIgnoreCase(String[] values, int length, String target) {
        for (int i = 0; i < length; i++) if (values[i].equalsIgnoreCase(target)) return i;
        return -1;
    }
}
