package com.dsa.jobportal.algorithms.stringalgorithms;

public class ZAlgorithm {
    private long comparisons;

    public boolean contains(String text, String pattern) {
        comparisons = 0;
        if (pattern == null || pattern.isEmpty()) return true;
        if (text == null || pattern.length() > text.length()) return false;
        String p = pattern.toLowerCase();
        String t = text.toLowerCase();
        // U+0001 is used as an internal separator and does not occur in normal job text.
        String combined = p + '\u0001' + t;
        int[] z = zFunction(combined);
        for (int i = p.length() + 1; i < z.length; i++) {
            if (z[i] >= p.length()) return true;
        }
        return false;
    }

    private int[] zFunction(String s) {
        int n = s.length();
        int[] z = new int[n];
        int left = 0;
        int right = 0;
        for (int i = 1; i < n; i++) {
            if (i <= right) z[i] = Math.min(right - i + 1, z[i - left]);
            while (i + z[i] < n) {
                comparisons++;
                if (s.charAt(z[i]) != s.charAt(i + z[i])) break;
                z[i]++;
            }
            if (i + z[i] - 1 > right) {
                left = i;
                right = i + z[i] - 1;
            }
        }
        return z;
    }

    public long getComparisons() { return comparisons; }
}
