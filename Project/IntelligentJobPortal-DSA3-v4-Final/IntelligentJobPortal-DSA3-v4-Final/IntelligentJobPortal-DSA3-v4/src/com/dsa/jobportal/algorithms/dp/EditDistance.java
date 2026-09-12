package com.dsa.jobportal.algorithms.dp;

public class EditDistance {
    public int distance(String a, String b) {
        String x = a == null ? "" : a.toLowerCase();
        String y = b == null ? "" : b.toLowerCase();
        int[] prev = new int[y.length() + 1];
        int[] curr = new int[y.length() + 1];
        for (int j = 0; j <= y.length(); j++) prev[j] = j;

        for (int i = 1; i <= x.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= y.length(); j++) {
                int cost = x.charAt(i - 1) == y.charAt(j - 1) ? 0 : 1;
                int deletion = prev[j] + 1;
                int insertion = curr[j - 1] + 1;
                int replace = prev[j - 1] + cost;
                curr[j] = Math.min(Math.min(deletion, insertion), replace);
            }
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }
        return prev[y.length()];
    }

    public double similarityPercent(String a, String b) {
        int max = Math.max(a == null ? 0 : a.length(), b == null ? 0 : b.length());
        if (max == 0) return 100.0;
        return Math.max(0.0, 100.0 * (1.0 - ((double) distance(a, b) / max)));
    }
}
