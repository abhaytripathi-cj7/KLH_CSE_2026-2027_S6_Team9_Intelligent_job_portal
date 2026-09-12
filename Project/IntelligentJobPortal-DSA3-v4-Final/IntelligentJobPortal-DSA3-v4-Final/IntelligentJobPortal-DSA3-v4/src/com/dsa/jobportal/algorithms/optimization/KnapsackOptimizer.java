package com.dsa.jobportal.algorithms.optimization;

import com.dsa.jobportal.model.MatchResult;

public class KnapsackOptimizer {
    public static class Plan {
        public final boolean[] selected;
        public final int totalHours;
        public final int totalValue;
        public final String method;

        public Plan(boolean[] selected, int totalHours, int totalValue, String method) {
            this.selected = selected;
            this.totalHours = totalHours;
            this.totalValue = totalValue;
            this.method = method;
        }
    }

    public Plan exact(MatchResult[] jobs, int maxHours) {
        int n = jobs.length;
        int[][] dp = new int[n + 1][maxHours + 1];
        for (int i = 1; i <= n; i++) {
            int w = jobs[i - 1].getJob().getApplyHours();
            int value = (int) Math.round(jobs[i - 1].getScore());
            for (int h = 0; h <= maxHours; h++) {
                dp[i][h] = dp[i - 1][h];
                if (w <= h) dp[i][h] = Math.max(dp[i][h], dp[i - 1][h - w] + value);
            }
        }

        boolean[] selected = new boolean[n];
        int h = maxHours;
        int totalHours = 0;
        for (int i = n; i >= 1; i--) {
            if (dp[i][h] != dp[i - 1][h]) {
                selected[i - 1] = true;
                int w = jobs[i - 1].getJob().getApplyHours();
                totalHours += w;
                h -= w;
            }
        }
        return new Plan(selected, totalHours, dp[n][maxHours], "Exact DP (pseudo-polynomial)");
    }

    public Plan halfApproximation(MatchResult[] jobs, int maxHours) {
        int n = jobs.length;
        boolean[] greedy = new boolean[n];
        boolean[] used = new boolean[n];
        int hours = 0;
        int value = 0;

        while (true) {
            int best = -1;
            double bestDensity = -1;
            for (int i = 0; i < n; i++) {
                if (used[i]) continue;
                int w = jobs[i].getJob().getApplyHours();
                int v = (int) Math.round(jobs[i].getScore());
                double density = w == 0 ? Double.MAX_VALUE : (double) v / w;
                if (density > bestDensity) {
                    bestDensity = density;
                    best = i;
                }
            }
            if (best < 0) break;
            used[best] = true;
            int w = jobs[best].getJob().getApplyHours();
            int v = (int) Math.round(jobs[best].getScore());
            if (hours + w <= maxHours) {
                greedy[best] = true;
                hours += w;
                value += v;
            }
        }

        int bestSingle = -1;
        int bestSingleValue = -1;
        for (int i = 0; i < n; i++) {
            if (jobs[i].getJob().getApplyHours() <= maxHours) {
                int v = (int) Math.round(jobs[i].getScore());
                if (v > bestSingleValue) {
                    bestSingleValue = v;
                    bestSingle = i;
                }
            }
        }

        if (bestSingleValue > value) {
            boolean[] selected = new boolean[n];
            if (bestSingle >= 0) selected[bestSingle] = true;
            int h = bestSingle >= 0 ? jobs[bestSingle].getJob().getApplyHours() : 0;
            return new Plan(selected, h, Math.max(0, bestSingleValue), "1/2-approximation (best single vs density greedy)");
        }
        return new Plan(greedy, hours, value, "1/2-approximation (best single vs density greedy)");
    }
}
